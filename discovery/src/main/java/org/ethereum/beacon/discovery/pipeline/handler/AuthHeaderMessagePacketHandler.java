package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeContext;
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

import static org.ethereum.beacon.discovery.NodeContext.SessionStatus.AUTHENTICATED;

/** Handles {@link AuthHeaderMessagePacket} in {@link Field#PACKET_AUTH_HEADER_MESSAGE} field */
public class AuthHeaderMessagePacketHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(AuthHeaderMessagePacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_AUTH_HEADER_MESSAGE)) {
      return;
    }
    if (!envelope.contains(Field.CONTEXT)) {
      return;
    }
    AuthHeaderMessagePacket packet =
        (AuthHeaderMessagePacket) envelope.get(Field.PACKET_AUTH_HEADER_MESSAGE);
    NodeContext context = (NodeContext) envelope.get(Field.CONTEXT);

    try {
      byte[] ephemeralKeyBytes = new byte[32];
      Functions.getRandom().nextBytes(ephemeralKeyBytes);
      ECKeyPair ephemeralKey = ECKeyPair.create(ephemeralKeyBytes);
      Triplet<BytesValue, BytesValue, BytesValue> hkdf =
          Functions.hkdf_expand(
              context.getHomeNodeId(),
              context.getNodeRecord().getNodeId(),
              BytesValue.wrap(ephemeralKey.getPrivateKey().toByteArray()),
              context.getIdNonce(),
              (BytesValue) context.getNodeRecord().get(NodeRecord.FIELD_PKEY_SECP256K1));
      BytesValue initiatorKey = hkdf.getValue0();
      context.setInitiatorKey(initiatorKey);
      BytesValue authResponseKey = hkdf.getValue2();
      packet.decode(initiatorKey, authResponseKey);
      packet.verify(context.getAuthTag().get(), context.getIdNonce());
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
      envelope.remove(Field.PACKET_AUTH_HEADER_MESSAGE);
      if (context.loadFuture() != null) {
        CompletableFuture<Void> future = context.loadFuture();
        context.saveFuture(null);
        future.completeExceptionally(ex);
      }
      return;
    }
    context.setStatus(AUTHENTICATED);
    envelope.remove(Field.PACKET_AUTH_HEADER_MESSAGE);
    if (context.loadFuture() != null) {
      boolean taskCompleted = false;
      if (envelope.get(Field.TASK).equals(TaskType.PING) && packet.getMessage() instanceof DiscoveryV5Message && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.PONG) {
        taskCompleted = true;
      }
      if (envelope.get(Field.TASK).equals(TaskType.FINDNODE) && packet.getMessage() instanceof DiscoveryV5Message && ((DiscoveryV5Message) packet.getMessage()).getCode() == MessageCode.NODES) {
        taskCompleted = true;
      }
      if (taskCompleted) {
        CompletableFuture<Void> future = context.loadFuture();
        future.complete(null);
        context.saveFuture(null);
      }
    }
  }
}
