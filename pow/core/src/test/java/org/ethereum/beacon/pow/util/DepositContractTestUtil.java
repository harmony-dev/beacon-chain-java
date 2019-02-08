package org.ethereum.beacon.pow.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.util.Eth1DataTestUtil;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.mockito.Mockito;

public abstract class DepositContractTestUtil {
  private DepositContractTestUtil() {}

  public static DepositContract mockDepositContract(Random random, List<DepositInfo> deposits) {
    return mockDepositContract(deposits, Eth1DataTestUtil.createRandom(random));
  }

  public static DepositContract mockDepositContract(List<DepositInfo> deposits, Eth1Data eth1Data) {
    DepositContract depositContract = Mockito.mock(DepositContract.class);
    Mockito.when(depositContract.getLatestEth1Data()).thenReturn(Optional.of(eth1Data));
    Mockito.when(depositContract.peekDeposits(anyInt(), any(), any())).thenReturn(deposits);
    Mockito.when(depositContract.hasDepositRoot(any(), any())).thenReturn(true);
    return depositContract;
  }
}
