package org.ethereum.beacon.pow;

import java.util.List;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

public interface DepositContract {

  ChainStart getChainStart();

  List<Deposit> getInitialDeposits();

  List<Deposit> peekDeposits(int count, Hash32 depositRoot, UInt64 fromIndex);

  class ChainStart {
    private final UInt64 time;
    private final Eth1Data eth1Data;

    public ChainStart(UInt64 time, Eth1Data eth1Data) {
      this.time = time;
      this.eth1Data = eth1Data;
    }

    public UInt64 getTime() {
      return time;
    }

    public Eth1Data getEth1Data() {
      return eth1Data;
    }
  }
}
