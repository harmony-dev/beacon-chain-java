package org.ethereum.beacon.discovery;

import com.google.common.collect.Sets;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.schedulers.Scheduler;
import tech.pegasys.artemis.util.bytes.Bytes33;

import java.util.Set;

public class RefreshTask {
  Scheduler scheduler;
  Set<Bytes33> currentTasks = Sets.newConcurrentHashSet();

  public RefreshTask(Scheduler scheduler) {
    this.scheduler = scheduler;
  }

  public void add(
      NodeRecordInfo nodeRecord, Runnable successCallback, Runnable failCallback) {
    synchronized (this) {
      if (currentTasks.contains(nodeRecord.getNode().getPublicKey())) {
        return;
      }
      currentTasks.add(nodeRecord.getNode().getPublicKey());
    }

    scheduler.execute(
        () -> {
          // try to connect;
          // if success:
          // successCallback.run();
          failCallback.run();
          currentTasks.remove(nodeRecord.getNode().getPublicKey());
        });
  }
}
