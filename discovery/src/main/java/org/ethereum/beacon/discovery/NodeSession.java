package org.ethereum.beacon.discovery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.Packet;
import org.ethereum.beacon.discovery.pipeline.info.RequestInfo;
import org.ethereum.beacon.discovery.pipeline.info.RequestInfoFactory;
import org.ethereum.beacon.discovery.storage.AuthTagRepository;
import org.ethereum.beacon.discovery.storage.NodeBucket;
import org.ethereum.beacon.discovery.storage.NodeBucketStorage;
import org.ethereum.beacon.discovery.storage.NodeTable;
import org.ethereum.beacon.discovery.task.TaskOptions;
import org.ethereum.beacon.discovery.task.TaskType;
import org.ethereum.beacon.util.ExpirationScheduler;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.ethereum.beacon.discovery.task.TaskStatus.AWAIT;

/**
 * Stores session status and all keys for discovery session between us (homeNode) and the other node
 */
public class NodeSession {
  public static final int NONCE_SIZE = 12;
  public static final int REQUEST_ID_SIZE = 8;
  private static final Logger logger = LogManager.getLogger(NodeSession.class);
  private static final int CLEANUP_DELAY_SECONDS = 60;
  private final NodeRecord homeNodeRecord;
  private final Bytes32 homeNodeId;
  private final AuthTagRepository authTagRepo;
  private final NodeTable nodeTable;
  private final NodeBucketStorage nodeBucketStorage;
  private final Consumer<Packet> outgoing;
  private final Random rnd;
  private NodeRecord nodeRecord;
  private SessionStatus status = SessionStatus.INITIAL;
  private Bytes32 idNonce;
  private BytesValue initiatorKey;
  private BytesValue recipientKey;
  private Map<BytesValue, RequestInfo> requestIdStatuses = new ConcurrentHashMap<>();
  private ExpirationScheduler<BytesValue> requestExpirationScheduler =
      new ExpirationScheduler<>(CLEANUP_DELAY_SECONDS, TimeUnit.SECONDS);
  private CompletableFuture<Void> completableFuture = null;
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

  public synchronized void updateNodeRecord(NodeRecord nodeRecord) {
    logger.trace(
        () ->
            String.format(
                "NodeRecord updated from %s to %s in session %s",
                this.nodeRecord, nodeRecord, this));
    this.nodeRecord = nodeRecord;
  }

  private void completeConnectFuture() {
    if (completableFuture != null) {
      completableFuture.complete(null);
      completableFuture = null;
    }
  }

  public void sendOutgoing(Packet packet) {
    logger.trace(() -> String.format("Sending outgoing packet %s in session %s", packet, this));
    outgoing.accept(packet);
  }

  /**
   * Creates object with request information: requestId etc, RequestInfo, designed to maintain
   * request status and its changes. Also stores info in session repository to track related
   * messages.
   *
   * <p>The value selected as request ID must allow for concurrent conversations. Using a timestamp
   * can result in parallel conversations with the same id, so this should be avoided. Request IDs
   * also prevent replay of responses. Using a simple counter would be fine if the implementation
   * could ensure that restarts or even re-installs would increment the counter based on previously
   * saved state in all circumstances. The easiest to implement is a random number.
   *
   * @param taskType Type of task, clarifies starting and reply message types
   * @param taskOptions Task options
   * @param future Future to be fired when task is successfully completed or exceptionally break
   *     when its failed
   * @return info bundle.
   */
  public synchronized RequestInfo createNextRequest(
      TaskType taskType, TaskOptions taskOptions, CompletableFuture<Void> future) {
    byte[] requestId = new byte[REQUEST_ID_SIZE];
    rnd.nextBytes(requestId);
    BytesValue wrappedId = BytesValue.wrap(requestId);
    if (taskOptions.isLivenessUpdate()) {
      future.whenComplete(
          (aVoid, throwable) -> {
            if (throwable == null) {
              updateLiveness();
            }
          });
    }
    RequestInfo requestInfo = RequestInfoFactory.create(taskType, wrappedId, taskOptions, future);
    requestIdStatuses.put(wrappedId, requestInfo);
    requestExpirationScheduler.put(
        wrappedId,
        new Runnable() {
          @Override
          public void run() {
            logger.debug(
                () ->
                    String.format(
                        "Request %s expired for id %s in session %s: no reply",
                        requestInfo, wrappedId, this));
            requestIdStatuses.remove(wrappedId);
          }
        });
    return requestInfo;
  }

  public synchronized void updateRequestInfo(BytesValue requestId, RequestInfo newRequestInfo) {
    RequestInfo oldRequestInfo = requestIdStatuses.remove(requestId);
    if (oldRequestInfo == null) {
      logger.debug(
          () ->
              String.format(
                  "An attempt to update requestId %s in session %s which does not exist",
                  requestId, this));
      return;
    }
    requestIdStatuses.put(requestId, newRequestInfo);
    requestExpirationScheduler.put(
        requestId,
        new Runnable() {
          @Override
          public void run() {
            logger.debug(
                String.format(
                    "Request %s expired for id %s in session %s: no reply",
                    newRequestInfo, requestId, this));
            requestIdStatuses.remove(requestId);
          }
        });
  }

  public synchronized void cancelAllRequests(String message) {
    logger.debug(() -> String.format("Cancelling all requests in session %s", this));
    Set<BytesValue> requestIdsCopy = new HashSet<>(requestIdStatuses.keySet());
    requestIdsCopy.forEach(
        requestId -> {
          RequestInfo requestInfo = clearRequestId(requestId);
          requestInfo
              .getFuture()
              .completeExceptionally(
                  new RuntimeException(
                      String.format(
                          "Request %s cancelled due to reason: %s", requestInfo, message)));
        });
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

  public synchronized void clearRequestId(BytesValue requestId, TaskType taskType) {
    RequestInfo requestInfo = clearRequestId(requestId);
    requestInfo.getFuture().complete(null);
    assert taskType.equals(requestInfo.getTaskType());
  }

  public synchronized void updateLiveness() {
    NodeRecordInfo nodeRecordInfo =
        new NodeRecordInfo(getNodeRecord(), Functions.getTime(), NodeStatus.ACTIVE, 0);
    nodeTable.save(nodeRecordInfo);
    nodeBucketStorage.put(nodeRecordInfo);
  }

  private synchronized RequestInfo clearRequestId(BytesValue requestId) {
    RequestInfo requestInfo = requestIdStatuses.remove(requestId);
    requestExpirationScheduler.cancel(requestId);
    return requestInfo;
  }

  public synchronized Optional<RequestInfo> getRequestId(BytesValue requestId) {
    RequestInfo requestInfo = requestIdStatuses.get(requestId);
    return requestId == null ? Optional.empty() : Optional.of(requestInfo);
  }

  public synchronized Optional<RequestInfo> getFirstAwaitRequestInfo() {
    return requestIdStatuses.values().stream()
        .filter(requestInfo -> AWAIT.equals(requestInfo.getTaskStatus()))
        .findFirst();
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
