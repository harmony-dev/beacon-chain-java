package org.ethereum.beacon.consensus.spec;

import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import org.ethereum.beacon.crypto.MessageParameters;
import org.jetbrains.annotations.NotNull;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * bls functions are put in a separate interface, so one can plug in different implementations. The
 * main reason is to replace slow BLS with fast (but absolutely insecure) {@link
 * org.ethereum.beacon.consensus.util.PseudoBLSFunctions}.
 */
public interface BLSFunctions {
  @NotNull
  static BLSFunctions getDefaultBLSFunctions(boolean blsVerify) {
    return blsVerify ? new InsecureBLSFunctions() : new DummyBLSFunctions();
  }

  boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain);

  boolean bls_verify_multiple(
      List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain);

  PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes);

  class InsecureBLSFunctions implements BLSFunctions {
    public boolean bls_verify(
        BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
      try {
        PublicKey blsPublicKey = PublicKey.create(publicKey);
        MessageParameters messageParameters = MessageParameters.create(message, domain);
        Signature blsSignature = Signature.create(signature);
        return BLS381.verify(messageParameters, blsSignature, blsPublicKey);
      } catch (Exception e) {
        return false;
      }
    }

    public boolean bls_verify_multiple(
        List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
      List<MessageParameters> messageParameters =
          messages.stream()
              .map(hash -> MessageParameters.create(hash, domain))
              .collect(Collectors.toList());
      Signature blsSignature = Signature.create(signature);
      return BLS381.verifyMultiple(messageParameters, blsSignature, publicKeys);
    }

    public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
      List<PublicKey> publicKeys =
          publicKeysBytes.stream().map(PublicKey::create).collect(toList());
      return PublicKey.aggregate(publicKeys);
    }
  }

  class DummyBLSFunctions implements BLSFunctions {
    @Override
    public boolean bls_verify(
        BLSPubkey publicKey, Hash32 message, BLSSignature signature, UInt64 domain) {
      return true;
    }

    @Override
    public boolean bls_verify_multiple(
        List<PublicKey> publicKeys, List<Hash32> messages, BLSSignature signature, UInt64 domain) {
      return true;
    }

    @Override
    public PublicKey bls_aggregate_pubkeys(List<BLSPubkey> publicKeysBytes) {
      return PublicKey.aggregate(Collections.emptyList());
    }
  }
}
