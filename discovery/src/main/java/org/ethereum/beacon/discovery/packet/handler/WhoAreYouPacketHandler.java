package org.ethereum.beacon.discovery.packet.handler;

import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeContext;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.ethereum.beacon.discovery.message.FindNodeMessage;
import org.ethereum.beacon.discovery.message.MessageCode;
import org.ethereum.beacon.discovery.packet.AuthHeaderMessagePacket;
import org.ethereum.beacon.discovery.packet.WhoAreYouPacket;
import org.javatuples.Triplet;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.ethereum.beacon.discovery.NodeContext.DEFAULT_DISTANCE;

public class WhoAreYouPacketHandler implements PacketHandler<WhoAreYouPacket> {
  private final NodeContext context;
  private final Logger logger;

  public WhoAreYouPacketHandler(NodeContext context, Logger logger) {
    this.context = context;
    this.logger = logger;
  }

  @Override
  public boolean handle(WhoAreYouPacket packet) {
    try {
      BytesValue authTag = context.getAuthTag().get();
      packet.verify(context.getHomeNodeId(), authTag);
      packet.getEnrSeq(); // FIXME: Their side enr seq. Do we need it?
      byte[] ephemeralKeyBytes = new byte[32];
      Functions.getRandom().nextBytes(ephemeralKeyBytes);
      ECKeyPair ephemeralKey = ECKeyPair.create(ephemeralKeyBytes); // TODO: generate
      Triplet<BytesValue, BytesValue, BytesValue> hkdf =
          Functions.hkdf_expand(
              context.getHomeNodeId(),
              context.getNodeRecord().getNodeId(),
              BytesValue.wrap(ephemeralKey.getPrivateKey().toByteArray()),
              packet.getIdNonce(),
              context.getNodeRecord().getPublicKey());
      BytesValue initiatorKey = hkdf.getValue0();
      BytesValue staticNodeKey = hkdf.getValue1();
      BytesValue authResponseKey = hkdf.getValue2();

      AuthHeaderMessagePacket response =
          AuthHeaderMessagePacket.create(
              context.getHomeNodeId(),
              context.getNodeRecord().getNodeId(),
              authResponseKey,
              packet.getIdNonce(),
              staticNodeKey,
              context.getHomeNodeRecord(),
              BytesValue.wrap(ephemeralKey.getPublicKey().toByteArray()),
              authTag,
              initiatorKey,
              DiscoveryV5Message.from(
                  new FindNodeMessage(
                      context.getNextRequestId(MessageCode.FINDNODE), DEFAULT_DISTANCE)));
      context.addOutgoingEvent(response);
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
