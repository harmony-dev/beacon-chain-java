package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucket;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class NodeSession {
  public static final int NONCE_SIZE = 12;
  private static final Logger logger = LogManager.getLogger(NodeSession.class);
  private final NodeRecord nodeRecord;
  private final NodeRecord homeNodeRecord;
  private final Bytes32 homeNodeId;
  private final AuthTagRepository authTagRepo;
  private final NodeTable nodeTable;
  private final NodeBucketStorage nodeBucketStorage;
  private final Consumer<Packet> outgoing;
  private final Random rnd;
  private SessionStatus status = SessionStatus.INITIAL;
  private Bytes32 idNonce;
  private BytesValue initiatorKey;
  private BytesValue recipientKey;
  private Map<BytesValue, MessageCode> requestIdReservations = new ConcurrentHashMap<>();
  private CompletableFuture<Void> completableFuture = null;
  private TaskType task = null;
  private BytesValue staticNodeKey;

  public NodeSession(
      NodeRecord nodeRecord,
      NodeRecord homeNodeRecord,
      BytesValue staticNodeKey,
      NodeTable nodeTable,
      NodeBucketStorage nodeBucketStorage,
      AuthTagRepository authTagRepo,
      Consumer<Packet> outgoing,
      Random rnd) {
    this.nodeRecord = nodeRecord;
    this.outgoing = outgoing;
    this.authTagRepo = authTagRepo;
    this.nodeTable = nodeTable;
    this.nodeBucketStorage = nodeBucketStorage;
    this.homeNodeRecord = homeNodeRecord;
    this.staticNodeKey = staticNodeKey;
    this.homeNodeId = homeNodeRecord.getNodeId();
    this.rnd = rnd;
  }

  public NodeRecord getNodeRecord() {
    return nodeRecord;
  }

  private void completeConnectFuture() {
    if (completableFuture != null) {
      completableFuture.complete(null);
      completableFuture = null;
    }
  }

  public synchronized void sendOutgoing(Packet packet) {
    logger.trace(() -> String.format("Sending outgoing packet %s in session %s", packet, this));
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

  public synchronized BytesValue generateNonce() {
    byte[] nonce = new byte[NONCE_SIZE];
    rnd.nextBytes(nonce);
    return BytesValue.wrap(nonce);
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

  public BytesValue getRecipientKey() {
    return recipientKey;
  }

  public void setRecipientKey(BytesValue recipientKey) {
    this.recipientKey = recipientKey;
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

  public void putRecordInBucket(NodeRecordInfo nodeRecordInfo) {
    nodeBucketStorage.put(nodeRecordInfo);
  }

  public Optional<NodeBucket> getBucket(int index) {
    return nodeBucketStorage.get(index);
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
    return "NodeSession{"
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

  public synchronized void setStatus(SessionStatus newStatus) {
    logger.debug(
        () ->
            String.format(
                "Switching status of node %s from %s to %s", nodeRecord, status, newStatus));
    this.status = newStatus;
  }

  public void saveFuture(@Nullable CompletableFuture<Void> completableFuture) {
    this.completableFuture = completableFuture;
  }

  public CompletableFuture<Void> loadFuture() {
    return completableFuture;
  }

  public void saveTask(@Nullable TaskType task) {
    this.task = task;
  }

  public TaskType loadTask() {
    return task;
  }

  public BytesValue getStaticNodeKey() {
    return staticNodeKey;
  }

  public enum SessionStatus {
    INITIAL, // other side is trying to connect, or we are initiating (before random packet is sent
    WHOAREYOU_SENT, // other side is initiator, we've sent whoareyou in response
    RANDOM_PACKET_SENT, // our node is initiator, we've sent random packet
    AUTHENTICATED
  }
}
