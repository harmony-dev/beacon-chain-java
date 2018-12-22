package org.ethereum.beacon.crypto;

import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public interface MessageParameters {

  Bytes32 getHash();

  BytesValue getDomain();

  class Impl implements MessageParameters {
    private final Bytes32 hash;
    private final BytesValue domain;

    public Impl(Bytes32 hash, BytesValue domain) {
      this.hash = hash;
      this.domain = domain;
    }

    @Override
    public Bytes32 getHash() {
      return hash;
    }

    @Override
    public BytesValue getDomain() {
      return domain;
    }
  }
}
