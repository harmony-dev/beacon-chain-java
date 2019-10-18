package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.pipeline.HandlerUtil;
import org.ethereum.beacon.discovery.task.TaskType;
import org.javatuples.Triplet;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

import static org.ethereum.beacon.discovery.NodeSession.SessionStatus.AUTHENTICATED;

/** Handles {@link AuthHeaderMessagePacket} in {@link Field#PACKET_AUTH_HEADER_MESSAGE} field */
public class AuthHeaderMessagePacketHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(AuthHeaderMessagePacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    logger.trace(
        () ->
            String.format(
                "Envelope %s in AuthHeaderMessagePacketHandler, checking requirements satisfaction",
                envelope.getId()));
    if (!HandlerUtil.requireField(Field.PACKET_AUTH_HEADER_MESSAGE, envelope)) {
      return;
    }
    if (!HandlerUtil.requireField(Field.SESSION, envelope)) {
      return;
    }
    logger.trace(
        () ->
            String.format(
                "Envelope %s in AuthHeaderMessagePacketHandler, requirements are satisfied!",
                envelope.getId()));

    AuthHeaderMessagePacket packet =
        (AuthHeaderMessagePacket) envelope.get(Field.PACKET_AUTH_HEADER_MESSAGE);
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    try {
      // FIXME: make this logic without side-effect
      packet.decode(
          ephemeralPubKey -> {
            Triplet<BytesValue, BytesValue, BytesValue> hkdf =
                Functions.hkdf_expand(
                    session.getNodeRecord().getNodeId(),
                    session.getHomeNodeId(),
                    session.getStaticNodeKey(),
                    ephemeralPubKey,
                    session.getIdNonce());
            session.setInitiatorKey(hkdf.getValue0());
            session.setRecipientKey(hkdf.getValue1());
            return hkdf;
          });
      packet.verify(session.getIdNonce());
      envelope.put(Field.MESSAGE, packet.getMessage());
    } catch (AssertionError ex) {
      logger.info(
          String.format(
              "Verification not passed for message [%s] from node %s in status %s",
              packet, session.getNodeRecord(), session.getStatus()));
    } catch (Exception ex) {
      String error =
          String.format(
              "Failed to read message [%s] from node %s in status %s",
              packet, session.getNodeRecord(), session.getStatus());
      logger.error(error, ex);
      envelope.remove(Field.PACKET_AUTH_HEADER_MESSAGE);
      if (session.loadFuture() != null) {
        CompletableFuture<Void> future = session.loadFuture();
        session.saveFuture(null);
        future.completeExceptionally(ex);
      }
      return;
    }
    session.setStatus(AUTHENTICATED);
    envelope.remove(Field.PACKET_AUTH_HEADER_MESSAGE);
    if (session.loadFuture() != null) {
      boolean taskCompleted = false;
      if (envelope.get(Field.TASK).equals(TaskType.PING)
          && packet.getMessage() instanceof DiscoveryV5Message
          && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.PONG) {
        taskCompleted = true;
      }
      if (envelope.get(Field.TASK).equals(TaskType.FINDNODE)
          && packet.getMessage() instanceof DiscoveryV5Message
          && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.NODES) {
        taskCompleted = true;
      }
      if (taskCompleted) {
        CompletableFuture<Void> future = session.loadFuture();
        future.complete(null);
        session.saveFuture(null);
      }
    }
  }
}
