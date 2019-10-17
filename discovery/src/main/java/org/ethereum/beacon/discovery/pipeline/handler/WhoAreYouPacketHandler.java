package org.ethereum.beacon.discovery.pipeline.handler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeSession;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.V5Message;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.ethereum.beacon.discovery.pipeline.Envelope;
import org.ethereum.beacon.discovery.pipeline.EnvelopeHandler;
import org.ethereum.beacon.discovery.pipeline.Field;
import org.ethereum.beacon.discovery.task.TaskMessageFactory;
import org.ethereum.beacon.discovery.task.TaskType;
import org.javatuples.Triplet;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.concurrent.CompletableFuture;

import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_PKEY_SECP256K1;

/** Handles {@link WhoAreYouPacket} in {@link Field#PACKET_WHOAREYOU} field */
public class WhoAreYouPacketHandler implements EnvelopeHandler {
  private static final Logger logger = LogManager.getLogger(WhoAreYouPacketHandler.class);

  @Override
  public void handle(Envelope envelope) {
    if (!envelope.contains(Field.PACKET_WHOAREYOU)) {
      return;
    }
    if (!envelope.contains(Field.SESSION)) {
      return;
    }
    WhoAreYouPacket packet = (WhoAreYouPacket) envelope.get(Field.PACKET_WHOAREYOU);
    NodeSession session = (NodeSession) envelope.get(Field.SESSION);
    try {
      NodeRecord respRecord = null;
      if (packet.getEnrSeq().compareTo(session.getHomeNodeRecord().getSeq()) < 0) {
        respRecord = session.getHomeNodeRecord();
      }
      BytesValue remotePubKey = (BytesValue) session.getNodeRecord().getKey(FIELD_PKEY_SECP256K1);
      byte[] ephemeralKeyBytes = new byte[32];
      Functions.getRandom().nextBytes(ephemeralKeyBytes);
      ECKeyPair ephemeralKey = ECKeyPair.create(ephemeralKeyBytes);

      Triplet<BytesValue, BytesValue, BytesValue> hkdf =
          Functions.hkdf_expand(
              session.getHomeNodeId(),
              session.getNodeRecord().getNodeId(),
              BytesValue.wrap(ephemeralKeyBytes),
              remotePubKey,
              packet.getIdNonce());
      BytesValue initiatorKey = hkdf.getValue0();
      BytesValue recipientKey = hkdf.getValue1();
      session.setInitiatorKey(initiatorKey);
      session.setRecipientKey(recipientKey);
      BytesValue authResponseKey = hkdf.getValue2();
      V5Message taskMessage = null;
      if (session.loadTask() == TaskType.PING) {
        taskMessage = TaskMessageFactory.createPing(session);
      } else if (session.loadTask() == TaskType.FINDNODE) {
        taskMessage = TaskMessageFactory.createFindNode(session);
      } else {
        throw new RuntimeException(
            String.format(
                "Type %s in envelope #%s is not known", session.loadTask(), envelope.getId()));
      }

      BytesValue ephemeralPubKey = BytesValue.wrap(ephemeralKey.getPublicKey().toByteArray());
      if (ephemeralPubKey.size() == 65) {
        ephemeralPubKey = ephemeralPubKey.slice(1); // slice leading 00
      }
      AuthHeaderMessagePacket response =
          AuthHeaderMessagePacket.create(
              session.getHomeNodeId(),
              session.getNodeRecord().getNodeId(),
              authResponseKey,
              packet.getIdNonce(),
              session.getStaticNodeKey(),
              respRecord,
              ephemeralPubKey,
              session.generateNonce(),
              initiatorKey,
              DiscoveryV5Message.from(taskMessage));
      session.sendOutgoing(response);
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
      envelope.remove(Field.PACKET_WHOAREYOU);
      if (envelope.contains(Field.FUTURE)) {
        CompletableFuture<Void> future = (CompletableFuture<Void>) envelope.get(Field.FUTURE);
        future.completeExceptionally(ex);
      }
      return;
    }
    session.setStatus(NodeSession.SessionStatus.AUTHENTICATED);
    envelope.remove(Field.PACKET_WHOAREYOU);
  }
}
