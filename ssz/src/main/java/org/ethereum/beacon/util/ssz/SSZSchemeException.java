package org.ethereum.beacon.util.ssz;

import net.consensys.cava.ssz.SSZException;

/**
 * Indicates errors associated with SSZ scheme building and type resolving
 */
public class SSZSchemeException extends SSZException {
  public SSZSchemeException() {
    super("Error in SSZ scheme");
  }

  public SSZSchemeException(String string) {
    super(string);
  }

  public SSZSchemeException(String string, Exception ex) {
    super(string, ex);
  }
}
