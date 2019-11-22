package org.ethereum.beacon.discovery.enr;

import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;

public interface EnrFieldInterpreter {
  Object decode(String key, RlpString rlpString);

  RlpType encode(String key, Object object);
}
