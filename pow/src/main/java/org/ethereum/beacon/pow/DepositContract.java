package org.ethereum.beacon.pow;

import tech.pegasys.artemis.util.uint.UInt64;

public interface DepositContract {

  UInt64 getChainStartTime();
}
