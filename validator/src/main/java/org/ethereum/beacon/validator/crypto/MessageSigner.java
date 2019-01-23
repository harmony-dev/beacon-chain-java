package org.ethereum.beacon.validator.crypto;

import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;

public interface MessageSigner<S> {
  S sign(Hash32 messageHash, Bytes8 domain);
}
