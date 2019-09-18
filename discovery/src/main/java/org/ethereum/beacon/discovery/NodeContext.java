package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.packet.UnknownPacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.javatuples.Triplet;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.security.SecureRandom;
import java.util.List;
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
  private List<Packet> incomingEvents;
  private SessionStatus status = SessionStatus.INITIAL;
  private Bytes32 idNonce;
  private BytesValue initiatorKey;
  private BytesValue authResponseKey;
  private MessageHandler messageHandler = null;
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
  }

  public NodeRecordV5 getNodeRecord() {
    return nodeRecord;
  }

  public synchronized void addIncomingEvent(UnknownPacket packet) {
    try {
      logger.trace(() -> String.format("Incoming packet in context %s", this));
      switch (status) {
        case INITIAL:
          {
            // packet it either random or message packet if session is expired
            BytesValue authTag = null;
            try {
              RandomPacket randomPacket = packet.getRandomPacket();
              authTag = randomPacket.getAuthTag();
            } catch (Exception ex) {
              // Not fatal, 1st attempt
            }
            // 2nd attempt
            if (authTag == null) {
              MessagePacket messagePacket = packet.getMessagePacket();
              authTag = messagePacket.getAuthTag();
            }
            authTagRepo.put(authTag, this);
            byte[] idNonceBytes = new byte[32];
            Functions.getRandom().nextBytes(idNonceBytes);
            this.idNonce = Bytes32.wrap(idNonceBytes);
            WhoAreYouPacket whoAreYouPacket =
                WhoAreYouPacket.create(
                    nodeRecord.getNodeId(), authTag, idNonce, nodeRecord.getSeqNumber());
            addOutgoingEvent(whoAreYouPacket);
            status = SessionStatus.WHOAREYOU_SENT;
            break;
          }
        case RANDOM_PACKET_SENT:
          {
            WhoAreYouPacket whoAreYouPacket = packet.getWhoAreYouPacket();
            BytesValue authTag = authTagRepo.getTag(this).get();
            whoAreYouPacket.verify(homeNodeId, authTag);
            whoAreYouPacket.getEnrSeq(); // FIXME: Their side enr seq. Do we need it?
            byte[] ephemeralKeyBytes = new byte[32];
            Functions.getRandom().nextBytes(ephemeralKeyBytes);
            ECKeyPair ephemeralKey = ECKeyPair.create(ephemeralKeyBytes); // TODO: generate
            Triplet<BytesValue, BytesValue, BytesValue> hkdf =
                Functions.hkdf_expand(
                    homeNodeId,
                    nodeRecord.getNodeId(),
                    BytesValue.wrap(ephemeralKey.getPrivateKey().toByteArray()),
                    whoAreYouPacket.getIdNonce(),
                    nodeRecord.getPublicKey());
            BytesValue initiatorKey = hkdf.getValue0();
            BytesValue staticNodeKey = hkdf.getValue1();
            BytesValue authResponseKey = hkdf.getValue2();

            AuthHeaderMessagePacket response =
                AuthHeaderMessagePacket.create(
                    homeNodeId,
                    nodeRecord.getNodeId(),
                    authResponseKey,
                    whoAreYouPacket.getIdNonce(),
                    staticNodeKey,
                    homeNodeRecord,
                    BytesValue.wrap(ephemeralKey.getPublicKey().toByteArray()),
                    authTag,
                    initiatorKey,
                    DiscoveryV5Message.from(
                        new FindNodeMessage(
                            getNextRequestId(MessageCode.FINDNODE), DEFAULT_DISTANCE)));
            createMessageHandler(initiatorKey, authTag);
            addOutgoingEvent(response);
            status = SessionStatus.AUTHENTICATED;
            break;
          }
        case WHOAREYOU_SENT:
          {
            AuthHeaderMessagePacket authHeaderMessagePacket = packet.getAuthHeaderMessagePacket();
            byte[] ephemeralKeyBytes = new byte[32];
            Functions.getRandom().nextBytes(ephemeralKeyBytes);
            ECKeyPair ephemeralKey = ECKeyPair.create(ephemeralKeyBytes);
            Triplet<BytesValue, BytesValue, BytesValue> hkdf =
                Functions.hkdf_expand(
                    homeNodeId,
                    nodeRecord.getNodeId(),
                    BytesValue.wrap(ephemeralKey.getPrivateKey().toByteArray()),
                    idNonce,
                    nodeRecord.getPublicKey());
            this.initiatorKey = hkdf.getValue0();
            this.authResponseKey = hkdf.getValue2();
            authHeaderMessagePacket.decode(initiatorKey, authResponseKey);
            authHeaderMessagePacket.verify(authTagRepo.getTag(this).get(), idNonce);
            createMessageHandler(initiatorKey, authHeaderMessagePacket.getAuthTag());
            messageHandler.handleIncoming(authHeaderMessagePacket.getMessage(), this);
            status = SessionStatus.AUTHENTICATED;
            break;
          }
        case AUTHENTICATED:
          {
            MessagePacket messagePacket = packet.getMessagePacket();
            messagePacket.decode(initiatorKey);
            messagePacket.verify(authTagRepo.getTag(this).get());
            messageHandler.handleIncoming(messagePacket.getMessage(), this);
            break;
          }
        default:
          {
            String error =
                String.format("Not expected status:%s from node: %s", status, nodeRecord);
            logger.error(error);
            throw new RuntimeException(error);
          }
      }
    } catch (AssertionError ex) {
      logger.info(
          String.format(
              "Verification not passed for message [%s] from node %s in status %s",
              packet, nodeRecord, status));
    } catch (Exception ex) {
      logger.info(
          String.format(
              "Failed to read message [%s] from node %s in status %s", packet, nodeRecord, status));
    }
  }

  private void createMessageHandler(BytesValue initiatorKey, BytesValue authTag) {
    this.messageHandler = new MessageHandler(new DiscoveryV5MessageHandler(homeNodeId, initiatorKey, authTag, nodeTable));
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
  private synchronized BytesValue getNextRequestId(MessageCode messageCode) {
    byte[] requestId = new byte[12];
    rnd.nextBytes(requestId);
    BytesValue wrapped = BytesValue.wrap(requestId);
    requestIdReservations.put(wrapped, messageCode);
    return wrapped;
  }

  public List<Packet> getIncomingEvents() {
    return incomingEvents;
  }

  public synchronized boolean isAuthenticated() {
    return SessionStatus.AUTHENTICATED.equals(status);
  }

  public void cleanup() {
    authTagRepo.expire(this);
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

  public synchronized void clearRequestId(BytesValue requestId, MessageCode messageCode) {
    assert requestIdReservations.remove(requestId).equals(messageCode);
  }

  public synchronized Optional<MessageCode> getRequestId(BytesValue requestId) {
    MessageCode messageCode = requestIdReservations.get(requestId);
    return messageCode == null ? Optional.empty() : Optional.of(messageCode);
  }

  enum SessionStatus {
    INITIAL, // other side is trying to connect, or we are initiating (before random packet is sent
    WHOAREYOU_SENT, // other side is initiator, we've sent whoareyou in response
    RANDOM_PACKET_SENT, // our node is initiator, we've sent random packet
    AUTHENTICATED
  }
}
