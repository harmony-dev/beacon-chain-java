package org.ethereum.beacon.chain.pool.verifier;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.PublicKey;
import org.ethereum.beacon.crypto.BLS381.Signature;
import tech.pegasys.artemis.util.collections.Bitlist;

/** A helper class aiding signature aggregation in context of beacon chain attestations. */
final class AggregateSignature {
  private Bitlist bits;
  private List<PublicKey> key0s = new ArrayList<>();
  private List<PublicKey> key1s = new ArrayList<>();
  private List<BLS381.Signature> sigs = new ArrayList<>();

  /**
   * Adds yet another attestation to the aggregation churn.
   *
   * @param bits an aggregate bitfield.
   * @param key0 bit0 key.
   * @param key1 bit1 key.
   * @param sig a signature.
   * @return {@code true} if it could be aggregated successfully, {@code false} if given bitfield
   *     has intersection with an accumulated one.
   */
  boolean add(Bitlist bits, PublicKey key0, PublicKey key1, BLS381.Signature sig) {
    // if bits has intersection it's not possible to get a viable aggregate
    if (this.bits != null && !this.bits.and(bits).isEmpty()) {
      return false;
    }

    if (this.bits == null) {
      this.bits = bits;
    } else {
      this.bits = this.bits.or(bits);
    }

    key0s.add(key0);
    key1s.add(key1);
    sigs.add(sig);

    return true;
  }

  /**
   * Computes and returns aggregate bit0 public key.
   *
   * @return a public key.
   */
  PublicKey getKey0() {
    return PublicKey.aggregate(key0s);
  }

  /**
   * Computes and returns aggregate bit1 public key.
   *
   * @return a public key.
   */
  PublicKey getKey1() {
    return PublicKey.aggregate(key1s);
  }

  /**
   * Computes and returns aggregate signature.
   *
   * @return a signature.
   */
  Signature getSignature() {
    return Signature.aggregate(sigs);
  }
}
