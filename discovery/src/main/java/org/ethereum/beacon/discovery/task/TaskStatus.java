package org.ethereum.beacon.discovery.task;

public enum TaskStatus {
  AWAIT, // waiting for handshake or whatever
  SENT, // request sent
  IN_PROCESS, // reply should contain several messages, at least one received but not all
  // XXX: completed task is not stored, so no status for completed
}
