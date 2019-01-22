package org.ethereum.beacon.pow;

import java.util.List;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.types.Ether;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.uint.UInt64;

public interface DepositContract {

  ChainStart getChainStart();

  List<Deposit> getInitialDeposits();

  Hash32 getRecentDepositRoot();

  boolean isValidatorRegistered(Bytes48 pubKey);

  class ChainStart {
    private final UInt64 time;
    private final Hash32 receiptRoot;

    public ChainStart(UInt64 time, Hash32 receiptRoot) {
      this.time = time;
      this.receiptRoot = receiptRoot;
    }

    public UInt64 getTime() {
      return time;
    }

    public Hash32 getReceiptRoot() {
      return receiptRoot;
    }
  }
}
