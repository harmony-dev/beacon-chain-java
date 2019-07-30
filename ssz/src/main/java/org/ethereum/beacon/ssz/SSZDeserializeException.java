package org.ethereum.beacon.ssz;

import net.consensys.cava.ssz.SSZException;

public class SSZDeserializeException extends SSZException {
  public SSZDeserializeException(String string) {
    super(string);
  }

  public SSZDeserializeException(String string, Exception ex) {
    super(string, ex);
  }
}
