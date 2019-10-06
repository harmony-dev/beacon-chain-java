package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.task.TaskType;
import org.javatuples.Triplet;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

import static org.ethereum.beacon.discovery.NodeSession.SessionStatus.AUTHENTICATED;

/** Handles {@link AuthHeaderMessagePacket} in {@link Field#PACKET_AUTH_HEADER_MESSAGE} field */
public class AuthHeaderMessagePacketHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(AuthHeaderMessagePacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_AUTH_HEADER_MESSAGE)) {
      return;
    }
    if (!envelope.contains(Field.SESSION)) {
      return;
    }
    AuthHeaderMessagePacket packet =
        (AuthHeaderMessagePacket) envelope.get(Field.PACKET_AUTH_HEADER_MESSAGE);
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);

    try {
      byte[] ephemeralKeyBytes = new byte[32];
      Functions.getRandom().nextBytes(ephemeralKeyBytes);
      ECKeyPair ephemeralKey = ECKeyPair.create(ephemeralKeyBytes);
      Triplet<BytesValue, BytesValue, BytesValue> hkdf =
          Functions.hkdf_expand(
              session.getHomeNodeId(),
              session.getNodeRecord().getNodeId(),
              BytesValue.wrap(ephemeralKey.getPrivateKey().toByteArray()),
              session.getIdNonce(),
              (BytesValue) session.getNodeRecord().get(NodeRecord.FIELD_PKEY_SECP256K1));
      BytesValue initiatorKey = hkdf.getValue0();
      session.setInitiatorKey(initiatorKey);
      BytesValue authResponseKey = hkdf.getValue2();
      packet.decode(initiatorKey, authResponseKey);
      packet.verify(session.getAuthTag().get(), session.getIdNonce());
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
      if (envelope.get(Field.TASK).equals(TaskType.PING) && packet.getMessage() instanceof DiscoveryV5Message && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.PONG) {
        taskCompleted = true;
      }
      if (envelope.get(Field.TASK).equals(TaskType.FINDNODE) && packet.getMessage() instanceof DiscoveryV5Message && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.NODES) {
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
