package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.ethereum.beacon.discovery.task.TaskMessageFactory;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/** Performs task execution for any task found in {@link Field#TASK} */
public class TaskHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(TaskHandler.class);
  private final Random rnd;

  public TaskHandler(Random rnd) {
    this.rnd = rnd;
  }

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in TaskHandler, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.TASK, envelope)) {
      return;
    }
    if (!HandlerUtil.requireField(Field.SESSION, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in TaskHandler, requirements are satisfied!", envelope.getId()));

    TaskType task = (TaskType) envelope.get(Field.TASK);
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    CompletableFuture<Void> completableFuture =
        (CompletableFuture<Void>) envelope.get(Field.FUTURE);
    BytesValue authTag = session.generateNonce();
    if (session.getStatus().equals(NodeSession.SessionStatus.INITIAL)) {
      RandomPacket randomPacket =
          RandomPacket.create(
              session.getHomeNodeId(),
              session.getNodeRecord().getNodeId(),
              authTag,
              new SecureRandom());
      session.setAuthTag(authTag);
      session.saveFuture(completableFuture);
      session.saveTask(task);
      session.sendOutgoing(randomPacket);
      session.setStatus(NodeSession.SessionStatus.RANDOM_PACKET_SENT);
    } else if (session.getStatus().equals(NodeSession.SessionStatus.AUTHENTICATED)) {
      if (TaskType.PING.equals(task)) {
        session.sendOutgoing(TaskMessageFactory.createPingPacket(authTag, session));
        session.saveFuture(completableFuture);
      } else if (TaskType.FINDNODE.equals(task)) {
        session.sendOutgoing(TaskMessageFactory.createFindNodePacket(authTag, session));
        session.saveFuture(completableFuture);
      } else {
        throw new RuntimeException(
            String.format(
                "Task type %s handler not found in envelope %s",
                envelope.get(Field.TASK), envelope.getId()));
      }
    } else {
      completableFuture.completeExceptionally(new RuntimeException("Already initiating"));
      // FIXME: or should we queue?
    }
  }
}
