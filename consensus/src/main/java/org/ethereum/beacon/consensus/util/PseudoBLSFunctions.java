package org.ethereum.beacon.consensus.util;

import org.ethereum.beacon.consensus.spec.BLSFunctions;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.math.BigInteger;
import java.util.List;

/**
 * Fast and not so dumb implementation of {@link BLSFunctions}, suitable for testing. It's not
 * secure at all, but able to detect when a wrong key/validator made a signature.
 * It's not 100% correct, but good enough to find bugs.
 */
public class PseudoBLSFunctions implements BLSFunctions {
  private static final BigInteger MAX_BYTES48_PLUS_ONE = BigInteger.ONE.shiftLeft(48 * 8);

  public static Bytes48 mapMessage(Hash32 messageHash, UInt64 domain) {
    return Bytes48.leftPad(messageHash.concat(domain.toBytes8()));
  }

  public static Bytes96 pseudoExp(Bytes48 a, Bytes48 b) {
    if (b.equals(Bytes48.ZERO)) return Bytes96.ZERO;
    else return Bytes96.leftPad(a.concat(b));
  }

  public static Bytes96 pseudoSign(Bytes48 key, Hash32 messageHash, UInt64 domain) {
    return pseudoExp(mapMessage(messageHash, domain), key);
  }

  public static BigInteger toBigInteger(BytesValue value) {
    return new BigInteger(1, value.extractArray());
  }

  public static BytesValue toBytesValue(BigInteger value, int size) {
    byte[] bytes = value.toByteArray();
    if (bytes.length > size + 1 || (bytes.length == size + 1 && bytes[0] != 0)) {
      throw new IllegalArgumentException("Too big value, cannot convert to Bytes" + size);
    }
    if (bytes.length == size + 1) {
      return BytesValue.wrap(bytes, 1, size);
    } else {
      return BytesValue.wrap(bytes);
    }
  }

  public static Bytes48 pseudoSum(Bytes48 a, Bytes48 b) {
    return Bytes48.leftPad(
        toBytesValue(toBigInteger(a).add(toBigInteger(b)).mod(MAX_BYTES48_PLUS_ONE), 48));
  }

  public static boolean pseudo_bls_verify(
      BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return pseudoSign(publicKey, message, domain).equals(signature);
  }

  public static boolean pseudo_bls_verify_multiple(
      List<BLS381.PublicKey> publicKeys,
      List<Hash32> messages,
      BLSSignature signature,
      UInt64 domain) {
    if (publicKeys.size() != messages.size()) {
      throw new IllegalArgumentException("message amount doesn't match key amount");
    }
    Bytes48 messageAcc = Bytes48.ZERO;
    Bytes48 keyAcc = Bytes48.ZERO;
    for (int i = 0; i < publicKeys.size(); i++) {
      Bytes48 publicKey = publicKeys.get(i).getEncodedBytes();
      if (!publicKey.equals(Bytes48.ZERO)) {
        messageAcc = pseudoSum(messageAcc, mapMessage(messages.get(i), domain));
        keyAcc = pseudoSum(keyAcc, publicKey);
      }
    }
    return pseudoExp(messageAcc, keyAcc).equals(signature);
  }

  public static BLS381.PublicKey pseudo_bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    return BLS381.PublicKey.createWithoutValidation(
        publicKeysBytes.stream()
            .map(pk -> (Bytes48) pk)
            .reduce(Bytes48.ZERO, PseudoBLSFunctions::pseudoSum));
  }

  public static BLSSignature mergeSignatures(List<BLSSignature> signatures) {
    Bytes48 messageAcc =
        signatures.stream()
            .map(s -> Bytes48.leftPad(s.slice(0, 48)))
            .reduce(Bytes48.ZERO, PseudoBLSFunctions::pseudoSum);
    Bytes48 keyAcc =
        signatures.stream()
            .map(s -> Bytes48.leftPad(s.slice(48, 48)))
            .reduce(Bytes48.ZERO, PseudoBLSFunctions::pseudoSum);
    return BLSSignature.wrap(pseudoExp(messageAcc, keyAcc));
  }

  @Override
  public boolean bls_verify(
      BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
    return pseudo_bls_verify(publicKey, message, signature, domain);
  }

  @Override
  public boolean bls_verify_multiple(
      List<BLS381.PublicKey> publicKeys,
      List<Hash32> messages,
      BLSSignature signature,
      UInt64 domain) {
    return pseudo_bls_verify_multiple(publicKeys, messages, signature, domain);
  }

  @Override
  public BLS381.PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
    return pseudo_bls_aggregate_pubkeys(publicKeysBytes);
  }
}
