package org.ethereum.beacon.consensus;

import java.util.List;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;

/**
 * Container for the deposit contract <code>ChainStartEvent</code>
 */
public class ChainStart {
  private final Time time;
  private final Eth1Data eth1Data;
  private final List<Deposit> initialDeposits;


  public ChainStart(Time time, Eth1Data eth1Data,
      List<Deposit> initialDeposits) {
    this.time = time;
    this.eth1Data = eth1Data;
    this.initialDeposits = initialDeposits;
  }

  public Time getTime() {
    return time;
  }

  public Eth1Data getEth1Data() {
    return eth1Data;
  }

  public List<Deposit> getInitialDeposits() {
    return initialDeposits;
  }
}
