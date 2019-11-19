package org.ethereum.beacon.discovery;

import com.google.common.base.Objects;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.util.Utils;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Sign;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import static org.ethereum.beacon.util.Utils.extractBytesFromUnsignedBigInt;
import static org.web3j.crypto.Sign.CURVE_PARAMS;

public class Functions {
  public static final ECDomainParameters SECP256K1_CURVE =
      new ECDomainParameters(
          CURVE_PARAMS.getCurve(), CURVE_PARAMS.getG(), CURVE_PARAMS.getN(), CURVE_PARAMS.getH());
  public static final int PUBKEY_SIZE = 64;
  private static final int RECIPIENT_KEY_LENGTH = 16;
  private static final int INITIATOR_KEY_LENGTH = 16;
  private static final int AUTH_RESP_KEY_LENGTH = 16;
  private static final int MS_IN_SECOND = 1000;

  public static Bytes32 hash(BytesValue value) {
    return Hashes.sha256(value);
  }

  /**
   * Creates a signature of message `x` using the given key.
   *
   * @param key private key
   * @param x message, not hashed
   * @return ECDSA signature with properties merged together: r || s
   */
  public static BytesValue sign(BytesValue key, BytesValue x) {
    BytesValue hash = Functions.hash(x);
    Sign.SignatureData signatureData =
        Sign.signMessage(hash.extractArray(), ECKeyPair.create(key.extractArray()), false);
    Bytes32 r = Bytes32.wrap(signatureData.getR());
    Bytes32 s = Bytes32.wrap(signatureData.getS());
    return r.concat(s);
  }

  /**
   * Verifies that signature is made by signer
   *
   * @param signature Signature, ECDSA
   * @param x message, not hashed
   * @param pubKey Public key of supposed signer, compressed, 33 bytes
   * @return whether `signature` reflects message `x` signed with `pubkey`
   */
  public static boolean verifyECDSASignature(
      BytesValue signature, BytesValue x, BytesValue pubKey) {
    assert pubKey.size() == 33;
    ECPoint ecPoint = Functions.publicKeyToPoint(pubKey);
    BytesValue pubKeyUncompressed = BytesValue.wrap(ecPoint.getEncoded(false)).slice(1);
    ECDSASignature ecdsaSignature =
        new ECDSASignature(
            new BigInteger(1, signature.slice(0, 32).extractArray()),
            new BigInteger(1, signature.slice(32).extractArray()));
    byte[] msgHash = Functions.hash(x).extractArray();
    for (int recId = 0; recId < 4; ++recId) {
      BigInteger calculatedPubKey = Sign.recoverFromSignature(recId, ecdsaSignature, msgHash);
      if (calculatedPubKey == null) {
        continue;
      }
      if (Arrays.areEqual(
          pubKeyUncompressed.extractArray(),
          extractBytesFromUnsignedBigInt(calculatedPubKey, PUBKEY_SIZE))) {
        return true;
      }
    }
    return false;
  }

  /**
   * AES-GCM encryption/authentication with the given `key`, `nonce` and additional authenticated
   * data `ad`. Size of `key` is 16 bytes (AES-128), size of `nonce` 12 bytes.
   */
  public static BytesValue aesgcm_encrypt(
      BytesValue privateKey, BytesValue nonce, BytesValue message, BytesValue aad) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE,
          new SecretKeySpec(privateKey.extractArray(), "AES"),
          new GCMParameterSpec(128, nonce.extractArray()));
      cipher.updateAAD(aad.extractArray());
      return BytesValue.wrap(cipher.doFinal(message.extractArray()));
    } catch (Exception e) {
      throw new RuntimeException("No AES/GCM cipher provider", e);
    }
  }

  public static BytesValue aesgcm_decrypt(
      BytesValue privateKey, BytesValue nonce, BytesValue encoded, BytesValue aad) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(privateKey.extractArray(), "AES"),
          new GCMParameterSpec(128, nonce.extractArray()));
      cipher.updateAAD(aad.extractArray());
      return BytesValue.wrap(cipher.doFinal(encoded.extractArray()));
    } catch (Exception e) {
      throw new RuntimeException("No AES/GCM cipher provider", e);
    }
  }

  public static ECPoint publicKeyToPoint(BytesValue pkey) {
    byte[] destPubPointBytes;
    if (pkey.size() == 64) { // uncompressed
      destPubPointBytes = new byte[pkey.size() + 1];
      destPubPointBytes[0] = 0x04; // default prefix
      System.arraycopy(pkey.extractArray(), 0, destPubPointBytes, 1, pkey.size());
    } else {
      destPubPointBytes = pkey.extractArray();
    }
    return SECP256K1_CURVE.getCurve().decodePoint(destPubPointBytes);
  }

  /** Derives public key in SECP256K1, compressed */
  public static BytesValue derivePublicKeyFromPrivate(BytesValue privateKey) {
    ECKeyPair ecKeyPair = ECKeyPair.create(privateKey.extractArray());
    final BytesValue pubKey =
        BytesValue.wrap(
            Utils.extractBytesFromUnsignedBigInt(ecKeyPair.getPublicKey(), PUBKEY_SIZE));
    ECPoint ecPoint = Functions.publicKeyToPoint(pubKey);
    return BytesValue.wrap(ecPoint.getEncoded(true));
  }

  /** Derives key agreement ECDH by multiplying private key by public */
  public static BytesValue deriveECDHKeyAgreement(BytesValue srcPrivKey, BytesValue destPubKey) {
    ECPoint pudDestPoint = publicKeyToPoint(destPubKey);
    ECPoint mult = pudDestPoint.multiply(new BigInteger(1, srcPrivKey.extractArray()));
    return BytesValue.wrap(mult.getEncoded(true));
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
  public static HKDFKeys hkdf_expand(
      BytesValue srcNodeId,
      BytesValue destNodeId,
      BytesValue srcPrivKey,
      BytesValue destPubKey,
      BytesValue idNonce) {
    BytesValue keyAgreement = deriveECDHKeyAgreement(srcPrivKey, destPubKey);
    return hkdf_expand(srcNodeId, destNodeId, keyAgreement, idNonce);
  }

  /**
   * {@link #hkdf_expand(BytesValue, BytesValue, BytesValue, BytesValue, BytesValue)} but with
   * keyAgreement already derived by {@link #deriveECDHKeyAgreement(BytesValue, BytesValue)}
   */
  public static HKDFKeys hkdf_expand(
      BytesValue srcNodeId, BytesValue destNodeId, BytesValue keyAgreement, BytesValue idNonce) {
    try {
      BytesValue info =
          BytesValue.wrap("discovery v5 key agreement".getBytes())
              .concat(srcNodeId)
              .concat(destNodeId);
      HKDFParameters hkdfParameters =
          new HKDFParameters(
              keyAgreement.extractArray(), idNonce.extractArray(), info.extractArray());
      Digest digest = new SHA256Digest();
      HKDFBytesGenerator hkdfBytesGenerator = new HKDFBytesGenerator(digest);
      hkdfBytesGenerator.init(hkdfParameters);
      // initiator-key || recipient-key || auth-resp-key
      byte[] hkdfOutputBytes =
          new byte[INITIATOR_KEY_LENGTH + RECIPIENT_KEY_LENGTH + AUTH_RESP_KEY_LENGTH];
      hkdfBytesGenerator.generateBytes(
          hkdfOutputBytes, 0, INITIATOR_KEY_LENGTH + RECIPIENT_KEY_LENGTH + AUTH_RESP_KEY_LENGTH);
      BytesValue hkdfOutput = BytesValue.wrap(hkdfOutputBytes);
      BytesValue initiatorKey = hkdfOutput.slice(0, INITIATOR_KEY_LENGTH);
      BytesValue recipientKey = hkdfOutput.slice(INITIATOR_KEY_LENGTH, RECIPIENT_KEY_LENGTH);
      BytesValue authRespKey = hkdfOutput.slice(INITIATOR_KEY_LENGTH + RECIPIENT_KEY_LENGTH);
      return new HKDFKeys(initiatorKey, recipientKey, authRespKey);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static long getTime() {
    return System.currentTimeMillis() / MS_IN_SECOND;
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

  public static class HKDFKeys {
    private final BytesValue initiatorKey;
    private final BytesValue recipientKey;
    private final BytesValue authResponseKey;

    public HKDFKeys(BytesValue initiatorKey, BytesValue recipientKey, BytesValue authResponseKey) {
      this.initiatorKey = initiatorKey;
      this.recipientKey = recipientKey;
      this.authResponseKey = authResponseKey;
    }

    public BytesValue getInitiatorKey() {
      return initiatorKey;
    }

    public BytesValue getRecipientKey() {
      return recipientKey;
    }

    public BytesValue getAuthResponseKey() {
      return authResponseKey;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      HKDFKeys hkdfKeys = (HKDFKeys) o;
      return Objects.equal(initiatorKey, hkdfKeys.initiatorKey)
          && Objects.equal(recipientKey, hkdfKeys.recipientKey)
          && Objects.equal(authResponseKey, hkdfKeys.authResponseKey);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(initiatorKey, recipientKey, authResponseKey);
    }
  }
}
