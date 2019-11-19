package org.ethereum.beacon.discovery.task;

import com.google.common.collect.Sets;
import org.ethereum.beacon.discovery.DiscoveryManager;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.schedulers.Scheduler;
import org.ethereum.beacon.util.ExpirationScheduler;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sends {@link TaskType#PING} to closest NodeRecords added via {@link #add(NodeRecordInfo,
 * Runnable, Runnable)}. Tasks is called failed if timeout is reached and reply from node is not
 * received.
 */
public class LiveCheckTasks {
  private final Scheduler scheduler;
  private final DiscoveryManager discoveryManager;
  private final Set<Bytes32> currentTasks = Sets.newConcurrentHashSet();
  private final ExpirationScheduler<Bytes32> taskTimeouts;

  public LiveCheckTasks(
      DiscoveryManager discoveryManager, Scheduler scheduler, Duration timeout) {
    this.discoveryManager = discoveryManager;
    this.scheduler = scheduler;
    this.taskTimeouts =
        new ExpirationScheduler<>(timeout.get(ChronoUnit.MILLIS), TimeUnit.MILLISECONDS);
  }

  public void add(NodeRecordInfo nodeRecordInfo, Runnable successCallback, Runnable failCallback) {
    synchronized (this) {
      if (currentTasks.contains(nodeRecordInfo.getNode().getNodeId())) {
        return;
      }
      currentTasks.add(nodeRecordInfo.getNode().getNodeId());
    }

    scheduler.execute(
        () -> {
          CompletableFuture<Void> retry =
              discoveryManager.executeTaskWithoutLivenessUpdate(nodeRecordInfo.getNode(), TaskType.PING);
          taskTimeouts.put(
              nodeRecordInfo.getNode().getNodeId(),
              () ->
                  retry.completeExceptionally(new RuntimeException("Timeout for node check task")));
          retry.whenComplete(
              (aVoid, throwable) -> {
                if (throwable != null) {
                  failCallback.run();
                  currentTasks.remove(nodeRecordInfo.getNode().getNodeId());
                } else {
                  successCallback.run();
                  currentTasks.remove(nodeRecordInfo.getNode().getNodeId());
                }
              });
        });
  }
}
