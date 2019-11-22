package org.ethereum.beacon.discovery.enr;

public interface EnrFieldV4 extends EnrField {
  // Compressed secp256k1 public key, 33 bytes
  String PKEY_SECP256K1 = "secp256k1";
}
