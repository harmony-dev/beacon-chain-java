package org.ethereum.beacon.discovery.packet.handler;

import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.javatuples.Triplet;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class AuthHeaderMessagePacketHandler implements PacketHandler<AuthHeaderMessagePacket> {
  private final NodeContext context;
  private final Logger logger;

  public AuthHeaderMessagePacketHandler(NodeContext context, Logger logger) {
    this.context = context;
    this.logger = logger;
  }

  @Override
  public boolean handle(AuthHeaderMessagePacket packet) {
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
      context.handleMessage(packet.getMessage());
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
      return false;
    }
    return true;
  }
}
