package org.ethereum.beacon.discovery.enr;

/**
 * Fields of Ethereum Node Record V4 as defined by <a
 * href="https://eips.ethereum.org/EIPS/eip-778">https://eips.ethereum.org/EIPS/eip-778</a>
 */
public interface EnrFieldV4 extends EnrField {
  // Compressed secp256k1 public key, 33 bytes
  String PKEY_SECP256K1 = "secp256k1";
}
