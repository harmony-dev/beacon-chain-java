package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.packet.handler.AuthHeaderMessagePacketHandler;
import org.ethereum.beacon.discovery.packet.handler.MessagePacketHandler;
import org.ethereum.beacon.discovery.packet.handler.PacketHandler;
import org.ethereum.beacon.discovery.packet.handler.UnknownPacketHandler;
import org.ethereum.beacon.discovery.packet.handler.WhoAreYouPacketHandler;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeTable;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NodeContext {
  public static final int DEFAULT_DISTANCE = 10; // FIXME: I shouldn't be here
  private static final Logger logger = LogManager.getLogger(NodeContext.class);
  private final NodeRecordV5 nodeRecord;
  private final NodeRecordV5 homeNodeRecord;
  private final Bytes32 homeNodeId;
  private final AuthTagRepository authTagRepo;
  private final NodeTable nodeTable;
  private final Consumer<Packet> outgoing;
  private final Random rnd;
  private final Map<Class, PacketHandler> packetHandlers = new HashMap<>();
  private SessionStatus status = SessionStatus.INITIAL;
  private Bytes32 idNonce;
  private BytesValue initiatorKey;
  private MessageProcessor messageProcessor = null;
  private Map<BytesValue, MessageCode> requestIdReservations = new ConcurrentHashMap<>();

  public NodeContext(
      NodeRecordV5 nodeRecord,
      NodeRecordV5 homeNodeRecord,
      NodeTable nodeTable,
      AuthTagRepository authTagRepo,
      Consumer<Packet> outgoing,
      Random rnd) {
    this.nodeRecord = nodeRecord;
    this.outgoing = outgoing;
    this.authTagRepo = authTagRepo;
    this.nodeTable = nodeTable;
    this.homeNodeRecord = homeNodeRecord;
    this.homeNodeId = homeNodeRecord.getNodeId();
    this.rnd = rnd;
    packetHandlers.put(UnknownPacket.class, new UnknownPacketHandler(this, logger));
    packetHandlers.put(WhoAreYouPacket.class, new WhoAreYouPacketHandler(this, logger));
    packetHandlers.put(
        AuthHeaderMessagePacket.class, new AuthHeaderMessagePacketHandler(this, logger));
    packetHandlers.put(MessagePacket.class, new MessagePacketHandler(this, logger));
  }

  public NodeRecordV5 getNodeRecord() {
    return nodeRecord;
  }

  public void addIncomingEvent(UnknownPacket packet) {
    logger.trace(() -> String.format("Incoming packet in context %s", this));
    switch (status) {
      case INITIAL:
        {
          if (packetHandlers.get(UnknownPacket.class).handle(packet)) {
            setStatus(SessionStatus.WHOAREYOU_SENT);
          }
          break;
        }
      case RANDOM_PACKET_SENT:
        {
          WhoAreYouPacket whoAreYouPacket = packet.getWhoAreYouPacket();
          if (packetHandlers.get(WhoAreYouPacket.class).handle(whoAreYouPacket)) {
            setStatus(SessionStatus.AUTHENTICATED);
          }
          break;
        }
      case WHOAREYOU_SENT:
        {
          AuthHeaderMessagePacket authHeaderMessagePacket = packet.getAuthHeaderMessagePacket();
          if (packetHandlers.get(AuthHeaderMessagePacket.class).handle(authHeaderMessagePacket)) {
            setStatus(SessionStatus.AUTHENTICATED);
          }
          break;
        }
      case AUTHENTICATED:
        {
          MessagePacket messagePacket = packet.getMessagePacket();
          packetHandlers.get(MessagePacket.class).handle(messagePacket);
          break;
        }
      default:
        {
          String error = String.format("Not expected status:%s from node: %s", status, nodeRecord);
          logger.error(error);
          throw new RuntimeException(error);
        }
    }
  }

  public void handleMessage(DiscoveryMessage message) {
    synchronized (this) {
      if (messageProcessor == null) {
        this.messageProcessor = new MessageProcessor(new DiscoveryV5MessageProcessor());
      }
    }
    messageProcessor.handleIncoming(message, this);
  }

  /** Sends random packet to start initiation of session with node */
  public void initiate() {
    byte[] authTagBytes = new byte[12];
    rnd.nextBytes(authTagBytes);
    BytesValue authTag = BytesValue.wrap(authTagBytes);
    RandomPacket randomPacket =
        RandomPacket.create(homeNodeId, nodeRecord.getNodeId(), authTag, new SecureRandom());
    authTagRepo.put(authTag, this);
    this.addOutgoingEvent(randomPacket);
    this.status = SessionStatus.RANDOM_PACKET_SENT;
  }

  public synchronized void addOutgoingEvent(Packet packet) {
    outgoing.accept(packet);
  }

  // FIXME: size, algo
  /**
   * The value selected as request ID must allow for concurrent conversations. Using a timestamp can
   * result in parallel conversations with the same id, so this should be avoided. Request IDs also
   * prevent replay of responses. Using a simple counter would be fine if the implementation could
   * ensure that restarts or even re-installs would increment the counter based on previously saved
   * state in all circumstances. The easiest to implement is a random number.
   *
   * <p>Request ID is reserved for `messageCode`
   */
  public synchronized BytesValue getNextRequestId(MessageCode messageCode) {
    byte[] requestId = new byte[12];
    rnd.nextBytes(requestId);
    BytesValue wrapped = BytesValue.wrap(requestId);
    requestIdReservations.put(wrapped, messageCode);
    return wrapped;
  }

  public synchronized boolean isAuthenticated() {
    return SessionStatus.AUTHENTICATED.equals(status);
  }

  public void cleanup() {
    authTagRepo.expire(this);
  }

  public Optional<BytesValue> getAuthTag() {
    return authTagRepo.getTag(this);
  }

  public void setAuthTag(BytesValue authTag) {
    authTagRepo.put(authTag, this);
  }

  public Bytes32 getHomeNodeId() {
    return homeNodeId;
  }

  public BytesValue getInitiatorKey() {
    return initiatorKey;
  }

  public void setInitiatorKey(BytesValue initiatorKey) {
    this.initiatorKey = initiatorKey;
  }

  public synchronized void clearRequestId(BytesValue requestId, MessageCode messageCode) {
    assert requestIdReservations.remove(requestId).equals(messageCode);
  }

  public synchronized Optional<MessageCode> getRequestId(BytesValue requestId) {
    MessageCode messageCode = requestIdReservations.get(requestId);
    return messageCode == null ? Optional.empty() : Optional.of(messageCode);
  }

  public NodeTable getNodeTable() {
    return nodeTable;
  }

  public synchronized Bytes32 getIdNonce() {
    return idNonce;
  }

  public synchronized void setIdNonce(Bytes32 idNonce) {
    this.idNonce = idNonce;
  }

  public NodeRecord getHomeNodeRecord() {
    return homeNodeRecord;
  }

  @Override
  public String toString() {
    return "NodeContext{"
        + "nodeRecord="
        + nodeRecord
        + ", homeNodeId="
        + homeNodeId
        + ", status="
        + status
        + '}';
  }

  public synchronized SessionStatus getStatus() {
    return status;
  }

  private synchronized void setStatus(SessionStatus newStatus) {
    logger.debug(
        () ->
            String.format(
                "Switching status of node %s from %s to %s", nodeRecord, status, newStatus));
    this.status = newStatus;
  }

  enum SessionStatus {
    INITIAL, // other side is trying to connect, or we are initiating (before random packet is sent
    WHOAREYOU_SENT, // other side is initiator, we've sent whoareyou in response
    RANDOM_PACKET_SENT, // our node is initiator, we've sent random packet
    AUTHENTICATED
  }
}
