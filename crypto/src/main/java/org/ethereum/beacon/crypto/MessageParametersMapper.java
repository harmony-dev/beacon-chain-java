package org.ethereum.beacon.crypto;

public interface MessageParametersMapper<P> {

  P map(MessageParameters parameters);
}
