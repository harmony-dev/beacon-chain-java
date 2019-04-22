package org.ethereum.beacon.ssz;

import net.consensys.cava.ssz.SSZException;

public class SSZSerializeException extends SSZException {
  public SSZSerializeException() {
    super("Error in SSZ scheme");
  }

  public SSZSerializeException(String string) {
    super(string);
  }

  public SSZSerializeException(String string, Exception ex) {
    super(string, ex);
  }
}
