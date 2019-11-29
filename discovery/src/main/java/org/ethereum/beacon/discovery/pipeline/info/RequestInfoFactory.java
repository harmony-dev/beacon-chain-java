package org.ethereum.beacon.discovery.pipeline.info;

import org.ethereum.beacon.discovery.task.TaskOptions;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

import static org.ethereum.beacon.discovery.task.TaskStatus.AWAIT;

public class RequestInfoFactory {
  public static RequestInfo create(
      TaskType taskType, BytesValue id, TaskOptions taskOptions, CompletableFuture<Void> future) {
    switch (taskType) {
      case FINDNODE:
        {
          return new FindNodeRequestInfo(AWAIT, id, future, taskOptions.getDistance(), null);
        }
      case PING:
        {
          return new GeneralRequestInfo(taskType, AWAIT, id, future);
        }
      default:
        {
          throw new RuntimeException(
              String.format("Factory doesn't know how to create task with type %s", taskType));
        }
    }
  }
}
