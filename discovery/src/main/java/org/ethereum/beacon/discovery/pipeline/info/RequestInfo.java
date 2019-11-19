package org.ethereum.beacon.discovery.pipeline.info;

import org.ethereum.beacon.discovery.task.TaskStatus;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

public interface RequestInfo {
  TaskType getTaskType();

  TaskStatus getTaskStatus();

  BytesValue getRequestId();

  CompletableFuture<Void> getFuture();
}
