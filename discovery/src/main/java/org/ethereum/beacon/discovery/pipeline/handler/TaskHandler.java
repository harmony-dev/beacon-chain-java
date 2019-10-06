package org.ethereum.beacon.discovery.pipeline.handler;

import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.packet.RandomPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.task.TaskMessageFactory;
import org.ethereum.beacon.discovery.task.TaskType;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/** Performs task execution for any task found in {@link Field#TASK} */
public class TaskHandler implements EnvelopeHandler {
  private final Random rnd;

  public TaskHandler(Random rnd) {
    this.rnd = rnd;
  }

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.TASK)) {
      return;
    }
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    CompletableFuture<Void> completableFuture =
        (CompletableFuture<Void>) envelope.get(Field.FUTURE);
    if (session.getStatus().equals(NodeSession.SessionStatus.INITIAL)) {
      byte[] authTagBytes = new byte[12];
      rnd.nextBytes(authTagBytes);
      BytesValue authTag = BytesValue.wrap(authTagBytes);
      RandomPacket randomPacket =
          RandomPacket.create(
              session.getHomeNodeId(),
              session.getNodeRecord().getNodeId(),
              authTag,
              new SecureRandom());
      session.setAuthTag(authTag);
      session.sendOutgoing(randomPacket);
      session.setStatus(NodeSession.SessionStatus.RANDOM_PACKET_SENT);
      session.saveFuture(completableFuture);
    } else if (session.getStatus().equals(NodeSession.SessionStatus.AUTHENTICATED)) {
      if (envelope.get(Field.TASK).equals(TaskType.PING)) {
        session.sendOutgoing(TaskMessageFactory.createPingPacket(session));
        session.saveTask((TaskType) envelope.get(Field.TASK));
        session.saveFuture(completableFuture);
      } else if (envelope.get(Field.TASK).equals(TaskType.FINDNODE)) {
        session.sendOutgoing(TaskMessageFactory.createFindNodePacket(session));
        session.saveTask((TaskType) envelope.get(Field.TASK));
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
