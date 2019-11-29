package org.ethereum.beacon.discovery.pipeline.info;

import org.ethereum.beacon.discovery.task.TaskStatus;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

public class GeneralRequestInfo implements RequestInfo {
  private final TaskType taskType;
  private final TaskStatus taskStatus;
  private final BytesValue requestId;
  private final CompletableFuture<Void> future;

  public GeneralRequestInfo(
      TaskType taskType,
      TaskStatus taskStatus,
      BytesValue requestId,
      CompletableFuture<Void> future) {
    this.taskType = taskType;
    this.taskStatus = taskStatus;
    this.requestId = requestId;
    this.future = future;
  }

  @Override
  public TaskType getTaskType() {
    return taskType;
  }

  @Override
  public TaskStatus getTaskStatus() {
    return taskStatus;
  }

  @Override
  public BytesValue getRequestId() {
    return requestId;
  }

  @Override
  public CompletableFuture<Void> getFuture() {
    return future;
  }

  @Override
  public String toString() {
    return "GeneralRequestInfo{"
        + "taskType="
        + taskType
        + ", taskStatus="
        + taskStatus
        + ", requestId="
        + requestId
        + '}';
  }
}
