package org.ethereum.beacon.discovery;

import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.math.ec.ECCurve;
import org.ethereum.beacon.crypto.Hashes;
import org.javatuples.Triplet;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Random;

public class Functions {
  private static final int RECIPIENT_KEY_LENGTH = 32; // FIXME
  private static final int INITIATOR_KEY_LENGTH = 32; // FIXME
  private static final int AUTH_RESP_KEY_LENGTH = 32; // FIXME

  public static Bytes32 hash(BytesValue value) {
    return Hashes.sha256(value);
  }

  /**
   * Creates a signature of message `x` using the given key
   *
   * @param key private key
   * @param x message
   * @return ECDSA signature with properties merged together: v || r || s
   */
  public static BytesValue sign(BytesValue key, BytesValue x) {
    Sign.SignatureData signatureData =
        Sign.signMessage(x.extractArray(), ECKeyPair.create(key.extractArray()));
    Bytes1 v = Bytes1.wrap(new byte[] {signatureData.getV()});
    Bytes32 r = Bytes32.wrap(signatureData.getR());
    Bytes32 s = Bytes32.wrap(signatureData.getS());
    return v.concat(r).concat(s);
  }

  /**
   * Recovers public key from message and signature
   *
   * @param signature Signature, ECDSA
   * @param x message
   * @return public key
   * @throws SignatureException when recovery is not possible
   */
  public static BytesValue recoverFromSignature(BytesValue signature, BytesValue x)
      throws SignatureException {
    BigInteger publicKey =
        Sign.signedMessageToKey(
            x.extractArray(),
            new Sign.SignatureData(
                signature.get(0),
                signature.slice(1, 33).extractArray(),
                signature.slice(33).extractArray()));
    return BytesValue.wrap(publicKey.toByteArray());
  }

  /**
   * AES-GCM encryption/authentication with the given `key`, `nonce` and additional authenticated
   * data `ad`. Size of `key` is 16 bytes (AES-128), size of `nonce` 12 bytes.
   */
  public static BytesValue aesgcm_encrypt(
      BytesValue privateKey, BytesValue nonce, BytesValue message, BytesValue aad) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(privateKey.extractArray(), "AES"),
          new IvParameterSpec(nonce.extractArray()));
      cipher.updateAAD(aad.extractArray());
      return BytesValue.wrap(cipher.doFinal(message.extractArray()));
    } catch (Exception e) {
      throw new RuntimeException("No AES/GCM cipher provider", e);
    }
  }

  public static BytesValue aesgcm_decrypt(
      BytesValue privateKey, BytesValue nonce, BytesValue encoded, BytesValue aad) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(privateKey.extractArray(), "AES"),
          new IvParameterSpec(nonce.extractArray()));
      cipher.updateAAD(aad.extractArray());
      return BytesValue.wrap(cipher.doFinal(encoded.extractArray()));
    } catch (Exception e) {
      throw new RuntimeException("No AES/GCM cipher provider", e);
    }
  }

  /**
   * The ephemeral key is used to perform Diffie-Hellman key agreement with B's static public key
   * and the session keys are derived from it using the HKDF key derivation function.
   *
   * <p><code>
   * ephemeral-key = random private key
   * ephemeral-pubkey = public key corresponding to ephemeral-key
   * dest-pubkey = public key of B
   * secret = agree(ephemeral-key, dest-pubkey)
   * info = "discovery v5 key agreement" || node-id-A || node-id-B
   * prk = HKDF-Extract(secret, id-nonce)
   * initiator-key, recipient-key, auth-resp-key = HKDF-Expand(prk, info)</code>
   */
  public static Triplet<BytesValue, BytesValue, BytesValue> hkdf_expand(
      BytesValue srcNodeId,
      BytesValue destNodeId,
      BytesValue ephemeralKey,
      BytesValue idNonce,
      BytesValue destPubKey) {
    try {
      Digest digest = new SHA256Digest(); // FIXME: or whatever
      ECPrivateKeyParameters ecdhPrivateKeyParameters =
          (ECPrivateKeyParameters) (PrivateKeyFactory.createKey(ephemeralKey.extractArray()));
      ECDomainParameters ecDomainParameters = ecdhPrivateKeyParameters.getParameters();
      ECCurve ecCurve = ecDomainParameters.getCurve();
      ECPublicKeyParameters ecPublicKeyParameters =
          new ECPublicKeyParameters(
              ecCurve.decodePoint(destPubKey.extractArray()), ecDomainParameters);
      BasicAgreement agree = new ECDHBasicAgreement();
      agree.init(ecdhPrivateKeyParameters);
      byte[] keyAgreement = agree.calculateAgreement(ecPublicKeyParameters).toByteArray();

      BytesValue info =
          BytesValue.wrap("discovery v5 key agreement".getBytes())
              .concat(srcNodeId)
              .concat(destNodeId);
      HKDFParameters hkdfParameters =
          new HKDFParameters(keyAgreement, idNonce.extractArray(), info.extractArray());
      HKDFBytesGenerator hkdfBytesGenerator = new HKDFBytesGenerator(digest);
      hkdfBytesGenerator.init(hkdfParameters);
      // initiator-key, recipient-key, auth-resp-key
      byte[] hkdfOutputBytes =
          new byte[INITIATOR_KEY_LENGTH + RECIPIENT_KEY_LENGTH + AUTH_RESP_KEY_LENGTH];
      hkdfBytesGenerator.generateBytes(
          hkdfOutputBytes, 0, INITIATOR_KEY_LENGTH + RECIPIENT_KEY_LENGTH + AUTH_RESP_KEY_LENGTH);
      BytesValue hkdfOutput = BytesValue.wrap(hkdfOutputBytes);
      BytesValue initiatorKey = hkdfOutput.slice(0, INITIATOR_KEY_LENGTH);
      BytesValue recipientKey = hkdfOutput.slice(INITIATOR_KEY_LENGTH, RECIPIENT_KEY_LENGTH);
      BytesValue authRespKey = hkdfOutput.slice(INITIATOR_KEY_LENGTH + RECIPIENT_KEY_LENGTH);
      return Triplet.with(initiatorKey, recipientKey, authRespKey);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Random getRandom() {
    return new SecureRandom();
  }

  /**
   * The 'distance' between two node IDs is the bitwise XOR of the IDs, taken as the number.
   *
   * <p>distance(n₁, n₂) = n₁ XOR n₂
   *
   * <p>LogDistance is reverse of length of common prefix in bits (length - number of leftmost zeros
   * in XOR)
   */
  public static int logDistance(Bytes32 nodeId1, Bytes32 nodeId2) {
    BytesValue distance = Bytes32s.xor(nodeId1, nodeId2);
    int logDistance = Byte.SIZE * distance.size(); // 256
    final int maxLogDistance = logDistance;
    for (int i = 0; i < maxLogDistance; ++i) {
      if (distance.getHighBit(i)) {
        break;
      } else {
        logDistance--;
      }
    }
    return logDistance;
  }
}
