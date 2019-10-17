package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.RlpUtil;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.function.Function;

/**
 * Used as first encrypted message sent in response to WHOAREYOU {@link WhoAreYouPacket}. Contains
 * an authentication header completing the handshake.
 *
 * <p>Format:<code>
 * message-packet = tag || auth-header || message
 * auth-header = [auth-tag, id-nonce, auth-scheme-name, ephemeral-pubkey, auth-response]
 * auth-scheme-name = "gcm"</code>
 *
 * <p>auth-response-pt is encrypted with a separate key, the auth-resp-key, using an all-zero nonce.
 * This is safe because only one message is ever encrypted with this key.
 *
 * <p><code>auth-response = aesgcm_encrypt(auth-resp-key, zero-nonce, auth-response-pt, "")
 * zero-nonce = 12 zero bytes
 * auth-response-pt = [version, id-nonce-sig, node-record]
 * version = 5
 * id-nonce-sig = sign(static-node-key, sha256("discovery-id-nonce" || id-nonce))
 * static-node-key = the private key used for node record identity
 * node-record = record of sender OR [] if enr-seq in WHOAREYOU != current seq
 * message = aesgcm_encrypt(initiator-key, auth-tag, message-pt, tag || auth-header)
 * message-pt = message-type || message-data
 * auth-tag = AES-GCM nonce, 12 random bytes unique to message</code>
 */
public class AuthHeaderMessagePacket extends AbstractPacket {
  public static final String AUTH_SCHEME_NAME = "gcm";
  private static final NodeRecordFactory nodeRecordFactory = NodeRecordFactory.DEFAULT;
  private static final BytesValue DISCOVERY_ID_NONCE =
      BytesValue.wrap("discovery-id-nonce".getBytes());
  private static final BytesValue ZERO_NONCE = BytesValue.wrap(new byte[12]);
  private MessagePacketDecoded decoded = null;

  public AuthHeaderMessagePacket(BytesValue bytes) {
    super(bytes);
  }

  public static AuthHeaderMessagePacket create(
      Bytes32 homeNodeId,
      Bytes32 destNodeId,
      BytesValue authResponseKey,
      BytesValue idNonce,
      BytesValue staticNodeKey,
      NodeRecord nodeRecord,
      BytesValue ephemeralPubkey,
      BytesValue authTag,
      BytesValue initiatorKey,
      DiscoveryMessage message) {
    BytesValue tag = Packet.createTag(homeNodeId, destNodeId);
    BytesValue idNonceSig =
        Functions.sign(
            staticNodeKey,
            Functions.hash(DISCOVERY_ID_NONCE.concat(idNonce).concat(ephemeralPubkey)));
    idNonceSig = idNonceSig.slice(1); // Remove recovery id
    byte[] authResponsePt =
        RlpEncoder.encode(
            new RlpList(
                RlpString.create(5),
                RlpString.create(idNonceSig.extractArray()),
                RlpString.create(
                    nodeRecord == null ? null : nodeRecord.serialize().extractArray())));
    BytesValue authResponse =
        Functions.aesgcm_encrypt(
            authResponseKey, ZERO_NONCE, BytesValue.wrap(authResponsePt), BytesValue.EMPTY);
    RlpList authHeaderRlp =
        new RlpList(
            RlpString.create(authTag.extractArray()),
            RlpString.create(idNonce.extractArray()),
            RlpString.create(AUTH_SCHEME_NAME.getBytes()),
            RlpString.create(ephemeralPubkey.extractArray()),
            RlpString.create(authResponse.extractArray()));
    BytesValue authHeader = BytesValue.wrap(RlpEncoder.encode(authHeaderRlp));
    BytesValue encryptedData =
        Functions.aesgcm_encrypt(initiatorKey, authTag, message.getBytes(), tag.concat(authHeader));
    return new AuthHeaderMessagePacket(tag.concat(authHeader).concat(encryptedData));
  }

  public void verify(BytesValue expectedIdNonce) {
    verifyDecode();
    assert expectedIdNonce.equals(getIdNonce());
  }

  public Bytes32 getHomeNodeId(Bytes32 destNodeId) {
    verifyDecode();
    return Bytes32s.xor(Functions.hash(destNodeId), decoded.tag);
  }

  public BytesValue getAuthTag() {
    verifyDecode();
    return decoded.authTag;
  }

  public BytesValue getIdNonce() {
    verifyDecode();
    return decoded.idNonce;
  }

  public BytesValue getEphemeralPubkey() {
    verifyDecode();
    return decoded.ephemeralPubkey;
  }

  public BytesValue getIdNonceSig() {
    verifyDecode();
    return decoded.idNonceSig;
  }

  public NodeRecord getNodeRecord() {
    verifyDecode();
    return decoded.nodeRecord;
  }

  public DiscoveryMessage getMessage() {
    verifyDecode();
    return decoded.message;
  }

  private void verifyDecode() {
    if (decoded == null) {
      throw new RuntimeException("You should decode packet at first!");
    }
  }

  public void decode(Function<BytesValue, Triplet<BytesValue, BytesValue, BytesValue>> ephemeralPubToKeysFunction) {
    if (decoded != null) {
      return;
    }
    MessagePacketDecoded blank = new MessagePacketDecoded();
    blank.tag = Bytes32.wrap(getBytes().slice(0, 32), 0);
    Pair<RlpList, BytesValue> decodeRes = RlpUtil.decodeFirstList(getBytes().slice(32));
    int authHeaderLength = getBytes().size() - 32 - decodeRes.getValue1().size();
    RlpList authHeaderParts = (RlpList) decodeRes.getValue0().getValues().get(0);
    // [auth-tag, id-nonce, auth-scheme-name, ephemeral-pubkey, auth-response]
    blank.authTag = BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(0)).getBytes());
    blank.idNonce = BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(1)).getBytes());
    assert AUTH_SCHEME_NAME.equals(
        new String(((RlpString) authHeaderParts.getValues().get(2)).getBytes()));
    blank.ephemeralPubkey =
        BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(3)).getBytes());
    Triplet<BytesValue, BytesValue, BytesValue> keys = ephemeralPubToKeysFunction.apply(blank.ephemeralPubkey);
    BytesValue authResponse =
        BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(4)).getBytes());

    BytesValue authResponsePt =
        Functions.aesgcm_decrypt(keys.getValue2(), ZERO_NONCE, authResponse, BytesValue.EMPTY);
    RlpList authResponsePtParts =
        (RlpList) RlpDecoder.decode(authResponsePt.extractArray()).getValues().get(0);
    assert BigInteger.valueOf(5)
        .equals(((RlpString) authResponsePtParts.getValues().get(0)).asPositiveBigInteger());
    blank.idNonceSig =
        BytesValue.wrap(((RlpString) authResponsePtParts.getValues().get(1)).getBytes());
    byte[] nodeRecordBytes = ((RlpString) authResponsePtParts.getValues().get(2)).getBytes();
    blank.nodeRecord =
        nodeRecordBytes.length == 0 ? null : nodeRecordFactory.fromBytes(nodeRecordBytes);
    BytesValue messageAad = blank.tag.concat(getBytes().slice(32, authHeaderLength));
    blank.message =
        new DiscoveryV5Message(
            Functions.aesgcm_decrypt(
                keys.getValue0(), blank.authTag, decodeRes.getValue1(), messageAad));
    this.decoded = blank;
  }

  @Override
  public String toString() {
    if (decoded != null) {
      return "AuthHeaderMessagePacket{"
          + "tag="
          + decoded.tag
          + ", authTag="
          + decoded.authTag
          + ", idNonce="
          + decoded.idNonce
          + ", ephemeralPubkey="
          + decoded.ephemeralPubkey
          + ", idNonceSig="
          + decoded.idNonceSig
          + ", nodeRecord="
          + decoded.nodeRecord
          + ", message="
          + decoded.message
          + '}';
    } else {
      return "AuthHeaderMessagePacket{" + getBytes() + '}';
    }
  }

  private static class MessagePacketDecoded {
    private Bytes32 tag;
    private BytesValue authTag;
    private BytesValue idNonce;
    private BytesValue ephemeralPubkey;
    private BytesValue idNonceSig;
    private NodeRecord nodeRecord;
    private DiscoveryMessage message;
  }
}
