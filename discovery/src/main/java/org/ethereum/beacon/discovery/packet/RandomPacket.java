package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.discovery.Functions;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Random;

/**
 * Sent if no session keys are available to initiate handshake
 *
 * <p>Format:<code>
 * random-packet = tag || rlp_bytes(auth-tag) || random-data
 * auth-tag = 12 random bytes unique to message
 * random-data = at least 44 bytes of random data</code>
 */
public class RandomPacket extends AbstractPacket {
  private RandomPacketDecoded decoded = null;

  public RandomPacket(BytesValue bytes) {
    super(bytes);
  }

  public static RandomPacket create(
      Bytes32 homeNodeId, Bytes32 destNodeId, BytesValue authTag, Random rnd) {
    BytesValue tag = Packet.createTag(homeNodeId, destNodeId);
    byte[] authTagRlp = RlpEncoder.encode(RlpString.create(authTag.extractArray()));
    BytesValue authTagEncoded = BytesValue.wrap(authTagRlp);
    byte[] randomBytes = new byte[44];
    rnd.nextBytes(randomBytes); // at least 44 bytes of random data
    return new RandomPacket(tag.concat(authTagEncoded).concat(BytesValue.wrap(randomBytes)));
  }

  public Bytes32 getHomeNodeId(Bytes32 destNodeId) {
    decode();
    return Bytes32s.xor(Functions.hash(destNodeId), decoded.tag);
  }

  public BytesValue getAuthTag() {
    decode();
    return decoded.authTag;
  }

  private synchronized void decode() {
    if (decoded != null) {
      return;
    }
    RandomPacketDecoded blank = new RandomPacketDecoded();
    blank.tag = Bytes32.wrap(getBytes().slice(0, 32), 0);
    blank.authTag =
        BytesValue.wrap(
            ((RlpString) RlpDecoder.decode(getBytes().slice(32).extractArray()).getValues().get(0))
                .getBytes());
    this.decoded = blank;
  }

  @Override
  public String toString() {
    if (decoded != null) {
      return "RandomPacket{" + "tag=" + decoded.tag + ", authTag=" + decoded.authTag + '}';
    } else {
      return "RandomPacket{" + getBytes() + '}';
    }
  }

  private static class RandomPacketDecoded {
    private Bytes32 tag;
    private BytesValue authTag;
  }
}
