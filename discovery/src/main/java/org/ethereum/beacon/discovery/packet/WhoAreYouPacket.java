package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.discovery.Functions;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * The WHOAREYOU packet, used during the handshake as a response to any message received from
 * unknown host
 *
 * <p>Format:<code>
 * whoareyou-packet = magic || [token, id-nonce, enr-seq]
 * magic = sha256(dest-node-id || "WHOAREYOU")
 * token = auth-tag of request
 * id-nonce = 32 random bytes
 * enr-seq = highest ENR sequence number of node A known on node B's side</code>
 */
public class WhoAreYouPacket extends AbstractPacket {
  private static final BytesValue MAGIC_BYTES = BytesValue.wrap("WHOAREYOU".getBytes());
  private WhoAreYouDecoded decoded = null;

  public WhoAreYouPacket(BytesValue bytes) {
    super(bytes);
  }

  public static WhoAreYouPacket create(
      Bytes32 destNodeId, BytesValue authTag, Bytes32 idNonce, Long enrSeq) {
    BytesValue magic = getStartMagic(destNodeId);
    byte[] rlpListEncoded =
        RlpEncoder.encode(
            new RlpList(
                RlpString.create(authTag.extractArray()),
                RlpString.create(idNonce.extractArray()),
                RlpString.create(enrSeq)));
    return new WhoAreYouPacket(magic.concat(BytesValue.wrap(rlpListEncoded)));
  }

  /** Calculates first 32 bytes of WHOAREYOU packet */
  public static BytesValue getStartMagic(Bytes32 destNodeId) {
    return Functions.hash(destNodeId.concat(MAGIC_BYTES));
  }

  public BytesValue getAuthTag() {
    decode();
    return decoded.authTag;
  }

  public Bytes32 getIdNonce() {
    decode();
    return decoded.idNonce;
  }

  public Long getEnrSeq() {
    decode();
    return decoded.enrSeq;
  }

  public void verify(Bytes32 destNodeId, BytesValue expectedAuthTag) {
    decode();
    assert Functions.hash(destNodeId.concat(MAGIC_BYTES)).equals(decoded.magic);
    assert expectedAuthTag.equals(getAuthTag());
  }

  private synchronized void decode() {
    if (decoded != null) {
      return;
    }
    WhoAreYouDecoded blank = new WhoAreYouDecoded();
    blank.magic = Bytes32.wrap(getBytes().slice(0, 32), 0);
    RlpList payload =
        (RlpList) RlpDecoder.decode(getBytes().slice(32).extractArray()).getValues().get(0);
    blank.authTag = BytesValue.wrap(((RlpString) payload.getValues().get(0)).getBytes());
    blank.idNonce = Bytes32.wrap(((RlpString) payload.getValues().get(1)).getBytes());
    blank.enrSeq =
        UInt64.fromBytesLittleEndian(
                Bytes8.rightPad(
                    BytesValue.wrap(((RlpString) payload.getValues().get(2)).getBytes())))
            .getValue();
    this.decoded = blank;
  }

  @Override
  public String toString() {
    if (decoded != null) {
      return "WhoAreYou{"
          + "magic="
          + decoded.magic
          + ", authTag="
          + decoded.authTag
          + ", idNonce="
          + decoded.idNonce
          + ", enrSeq="
          + decoded.enrSeq
          + '}';
    } else {
      return "WhoAreYou{" + getBytes() + '}';
    }
  }

  private static class WhoAreYouDecoded {
    private Bytes32 magic;
    private BytesValue authTag;
    private Bytes32 idNonce;
    private Long enrSeq;
  }
}
