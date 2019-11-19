package org.ethereum.beacon.discovery.packet;

import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.RlpUtil;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.message.DiscoveryMessage;
import org.ethereum.beacon.discovery.message.DiscoveryV5Message;
import org.javatuples.Pair;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.annotation.Nullable;
import java.math.BigInteger;

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
  public static final BytesValue DISCOVERY_ID_NONCE =
      BytesValue.wrap("discovery-id-nonce".getBytes());
  private static final BytesValue ZERO_NONCE = BytesValue.wrap(new byte[12]);
  private EphemeralPubKeyDecoded decodedEphemeralPubKeyPt = null;
  private MessagePtDecoded decodedMessagePt = null;

  public AuthHeaderMessagePacket(BytesValue bytes) {
    super(bytes);
  }

  public static AuthHeaderMessagePacket create(
      Bytes32 tag, BytesValue authHeader, BytesValue messageCipherText) {
    return new AuthHeaderMessagePacket(tag.concat(authHeader).concat(messageCipherText));
  }

  public static BytesValue createIdNonceMessage(BytesValue idNonce, BytesValue ephemeralPubkey) {
    BytesValue message = DISCOVERY_ID_NONCE.concat(idNonce).concat(ephemeralPubkey);
    return message;
  }

  public static BytesValue signIdNonce(
      BytesValue idNonce, BytesValue staticNodeKey, BytesValue ephemeralPubkey) {
    BytesValue signed =
        Functions.sign(staticNodeKey, createIdNonceMessage(idNonce, ephemeralPubkey));
    return signed;
  }

  public static byte[] createAuthMessagePt(BytesValue idNonceSig, @Nullable NodeRecord nodeRecord) {
    return RlpEncoder.encode(
        new RlpList(
            RlpString.create(5),
            RlpString.create(idNonceSig.extractArray()),
            nodeRecord == null ? new RlpList() : nodeRecord.asRlp()));
  }

  public static BytesValue encodeAuthResponse(byte[] authResponsePt, BytesValue authResponseKey) {
    return Functions.aesgcm_encrypt(
        authResponseKey, ZERO_NONCE, BytesValue.wrap(authResponsePt), BytesValue.EMPTY);
  }

  public static BytesValue encodeAuthHeaderRlp(
      BytesValue authTag, BytesValue idNonce, BytesValue ephemeralPubkey, BytesValue authResponse) {
    RlpList authHeaderRlp =
        new RlpList(
            RlpString.create(authTag.extractArray()),
            RlpString.create(idNonce.extractArray()),
            RlpString.create(AUTH_SCHEME_NAME.getBytes()),
            RlpString.create(ephemeralPubkey.extractArray()),
            RlpString.create(authResponse.extractArray()));
    return BytesValue.wrap(RlpEncoder.encode(authHeaderRlp));
  }

  public static AuthHeaderMessagePacket create(
      Bytes32 homeNodeId,
      Bytes32 destNodeId,
      BytesValue authResponseKey,
      BytesValue idNonce,
      BytesValue staticNodeKey,
      @Nullable NodeRecord nodeRecord,
      BytesValue ephemeralPubkey,
      BytesValue authTag,
      BytesValue initiatorKey,
      DiscoveryMessage message) {
    Bytes32 tag = Packet.createTag(homeNodeId, destNodeId);
    BytesValue idNonceSig = signIdNonce(idNonce, staticNodeKey, ephemeralPubkey);
    byte[] authResponsePt = createAuthMessagePt(idNonceSig, nodeRecord);
    BytesValue authResponse = encodeAuthResponse(authResponsePt, authResponseKey);
    BytesValue authHeader = encodeAuthHeaderRlp(authTag, idNonce, ephemeralPubkey, authResponse);
    BytesValue encryptedData =
        Functions.aesgcm_encrypt(initiatorKey, authTag, message.getBytes(), tag);
    return create(tag, authHeader, encryptedData);
  }

  public void verify(BytesValue expectedIdNonce, BytesValue remoteNodePubKey) {
    verifyDecode();
    assert expectedIdNonce.equals(getIdNonce());
    assert Functions.verifyECDSASignature(
        getIdNonceSig(),
        createIdNonceMessage(getIdNonce(), getEphemeralPubkey()),
        remoteNodePubKey);
  }

  public Bytes32 getHomeNodeId(Bytes32 destNodeId) {
    verifyDecode();
    return Bytes32s.xor(Functions.hash(destNodeId), decodedEphemeralPubKeyPt.tag);
  }

  public BytesValue getAuthTag() {
    verifyDecode();
    return decodedEphemeralPubKeyPt.authTag;
  }

  public BytesValue getIdNonce() {
    verifyDecode();
    return decodedEphemeralPubKeyPt.idNonce;
  }

  public BytesValue getEphemeralPubkey() {
    verifyEphemeralPubKeyDecode();
    return decodedEphemeralPubKeyPt.ephemeralPubkey;
  }

  public BytesValue getIdNonceSig() {
    verifyDecode();
    return decodedMessagePt.idNonceSig;
  }

  public NodeRecord getNodeRecord() {
    verifyDecode();
    return decodedMessagePt.nodeRecord;
  }

  public DiscoveryMessage getMessage() {
    verifyDecode();
    return decodedMessagePt.message;
  }

  private void verifyEphemeralPubKeyDecode() {
    if (decodedEphemeralPubKeyPt == null) {
      throw new RuntimeException("You should run decodeEphemeralPubKey before!");
    }
  }

  private void verifyDecode() {
    if (decodedEphemeralPubKeyPt == null || decodedMessagePt == null) {
      throw new RuntimeException("You should run decodeEphemeralPubKey and decodeMessage before!");
    }
  }

  public void decodeEphemeralPubKey() {
    if (decodedEphemeralPubKeyPt != null) {
      return;
    }
    EphemeralPubKeyDecoded blank = new EphemeralPubKeyDecoded();
    blank.tag = Bytes32.wrap(getBytes().slice(0, 32), 0);
    Pair<RlpList, BytesValue> decodeRes = RlpUtil.decodeFirstList(getBytes().slice(32));
    blank.messageEncrypted = decodeRes.getValue1();
    RlpList authHeaderParts = (RlpList) decodeRes.getValue0().getValues().get(0);
    // [auth-tag, id-nonce, auth-scheme-name, ephemeral-pubkey, auth-response]
    blank.authTag = BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(0)).getBytes());
    blank.idNonce = BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(1)).getBytes());
    assert AUTH_SCHEME_NAME.equals(
        new String(((RlpString) authHeaderParts.getValues().get(2)).getBytes()));
    blank.ephemeralPubkey =
        BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(3)).getBytes());
    blank.authResponse =
        BytesValue.wrap(((RlpString) authHeaderParts.getValues().get(4)).getBytes());
    this.decodedEphemeralPubKeyPt = blank;
  }

  /** Run {@link AuthHeaderMessagePacket#decodeEphemeralPubKey()} before second part */
  public void decodeMessage(
      BytesValue initiatorKey, BytesValue authResponseKey, NodeRecordFactory nodeRecordFactory) {
    if (decodedEphemeralPubKeyPt == null) {
      throw new RuntimeException("Run decodeEphemeralPubKey() before");
    }
    if (decodedMessagePt != null) {
      return;
    }
    MessagePtDecoded blank = new MessagePtDecoded();
    BytesValue authResponsePt =
        Functions.aesgcm_decrypt(
            authResponseKey, ZERO_NONCE, decodedEphemeralPubKeyPt.authResponse, BytesValue.EMPTY);
    RlpList authResponsePtParts =
        (RlpList) RlpDecoder.decode(authResponsePt.extractArray()).getValues().get(0);
    assert BigInteger.valueOf(5)
        .equals(((RlpString) authResponsePtParts.getValues().get(0)).asPositiveBigInteger());
    blank.idNonceSig =
        BytesValue.wrap(((RlpString) authResponsePtParts.getValues().get(1)).getBytes());
    RlpList nodeRecordDataList = ((RlpList) authResponsePtParts.getValues().get(2));
    blank.nodeRecord =
        nodeRecordDataList.getValues().isEmpty()
            ? null
            : nodeRecordFactory.fromRlpList(nodeRecordDataList);
    blank.message =
        new DiscoveryV5Message(
            Functions.aesgcm_decrypt(
                initiatorKey,
                decodedEphemeralPubKeyPt.authTag,
                decodedEphemeralPubKeyPt.messageEncrypted,
                decodedEphemeralPubKeyPt.tag));
    this.decodedMessagePt = blank;
  }

  @Override
  public String toString() {
    StringBuilder res = new StringBuilder("AuthHeaderMessagePacket{");
    if (decodedEphemeralPubKeyPt != null) {
      res.append("tag=")
          .append(decodedEphemeralPubKeyPt.tag)
          .append(", authTag=")
          .append(decodedEphemeralPubKeyPt.authTag)
          .append(", idNonce=")
          .append(decodedEphemeralPubKeyPt.idNonce)
          .append(", ephemeralPubkey=")
          .append(decodedEphemeralPubKeyPt.ephemeralPubkey);
    }
    if (decodedMessagePt != null) {
      res.append(", idNonceSig=")
          .append(decodedMessagePt.idNonceSig)
          .append(", nodeRecord=")
          .append(decodedMessagePt.nodeRecord)
          .append(", message=")
          .append(decodedMessagePt.message);
    }
    res.append('}');
    return res.toString();
  }

  private static class EphemeralPubKeyDecoded {
    private Bytes32 tag;
    private BytesValue authTag;
    private BytesValue idNonce;
    private BytesValue ephemeralPubkey;
    private BytesValue authResponse;
    private BytesValue messageEncrypted;
  }

  private static class MessagePtDecoded {
    private BytesValue idNonceSig;
    private NodeRecord nodeRecord;
    private DiscoveryMessage message;
  }
}
