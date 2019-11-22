package org.ethereum.beacon.discovery.pipeline.info;

import org.ethereum.beacon.discovery.task.TaskStatus;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

/** Stores info related to performed request */
public interface RequestInfo {
  /** Task type, in execution of which request was created */
  TaskType getTaskType();

  /** Status of corresponding task */
  TaskStatus getTaskStatus();

  /** Id of request */
  BytesValue getRequestId();

  /** Future that should be fired when request is fulfilled or cancelled due to errors */
  CompletableFuture<Void> getFuture();
}
