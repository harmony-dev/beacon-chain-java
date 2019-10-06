package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.MessagePacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.task.TaskType;

import java.util.concurrent.CompletableFuture;

/** Handles {@link MessagePacket} in {@link Field#PACKET_MESSAGE} field */
public class MessagePacketHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(MessagePacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_MESSAGE)) {
      return;
    }
    if (!envelope.contains(Field.CONTEXT)) {
      return;
    }
    MessagePacket packet = (MessagePacket) envelope.get(Field.PACKET_MESSAGE);
    NodeContext context = (NodeContext) envelope.get(Field.CONTEXT);

    try {
      packet.decode(context.getInitiatorKey());
      packet.verify(context.getAuthTag().get());
      envelope.put(Field.MESSAGE, packet.getMessage());
    } catch (AssertionError ex) {
      logger.info(
          String.format(
              "Verification not passed for message [%s] from node %s in status %s",
              packet, context.getNodeRecord(), context.getStatus()));
    } catch (Exception ex) {
      String error =
          String.format(
              "Failed to read message [%s] from node %s in status %s",
              packet, context.getNodeRecord(), context.getStatus());
      logger.error(error, ex);
      envelope.remove(Field.PACKET_MESSAGE);
      if (envelope.contains(Field.FUTURE)) {
        CompletableFuture<Void> future = (CompletableFuture<Void>) envelope.get(Field.FUTURE);
        future.completeExceptionally(ex);
      }
      return;
    }
    envelope.remove(Field.PACKET_MESSAGE);
    CompletableFuture<Void> future = context.loadFuture();
    TaskType taskType = context.loadTask();
    if (future != null) {
      boolean taskCompleted = false;
      if (TaskType.PING.equals(taskType) && packet.getMessage() instanceof DiscoveryV5Message && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.PONG) {
        taskCompleted = true;
      }
      if (TaskType.FINDNODE.equals(taskType) && packet.getMessage() instanceof DiscoveryV5Message && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.NODES) {
        taskCompleted = true;
      }
      if (taskCompleted) {
        future.complete(null);
        context.saveFuture(null);
        context.saveTask(null);
      }
    }
  }
}
