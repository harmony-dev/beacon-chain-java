package org.ethereum.beacon.crypto;

import tech.pegasys.pantheon.util.bytes.Bytes32;
import tech.pegasys.pantheon.util.bytes.BytesValue;

public interface MessageSpec {

  Bytes32 getHash();

  BytesValue getDomain();
}
