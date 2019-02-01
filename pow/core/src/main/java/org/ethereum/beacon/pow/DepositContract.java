package org.ethereum.beacon.pow;

import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.reactivestreams.Publisher;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Interface to the Eth1.0 Deposit contract
 */
public interface DepositContract {

  /**
   * Returns the '0|1 events' stream publisher to listen to <code>ChainStart</code> event
   * The listening process starts lazily when at least one subscriber subscribes.
   * The process takes into account the <code>distanceFromHead</code> parameter, so
   * the <code>ChainStart</code> event is issued only when specified number
   * of confirmation blocks are imported.
   */
  Publisher<ChainStart> getChainStartMono();

  /**
   * Returns a list of deposits found in already imported blocks.
   * The method takes into account the <code>distanceFromHead</code> parameter, so
   * only events from confirmed blocks are returned.
   * NOTE: this is a blocking call which may execute significant amount of time since
   * it's scanning Blocks database
   *
   * @param maxCount Maximum number of returned events
   * @param fromDepositExclusive effectively the last deposit 'coordinates' which
   * was included into blocks
   * @param tillDepositInclusive effectively the last voted <code>Eth1Data</code> from the
   * <code>BeaconState</code>
   * @return requested deposit infos. Empty list if <code>fromDeposit == tillDeposit</code>
   * od the blockchain is out of sync and <code>tillDeposit</code> block is not imported yet
   */
  List<DepositInfo> peekDeposits(int maxCount,
      Eth1Data fromDepositExclusive, Eth1Data tillDepositInclusive);

  /**
   * Checks if the block with <code>blockHash</code> contains <code>DepositEvent</code>
   * with the specified <code>depositRoot</code>
   * The method takes into account the <code>distanceFromHead</code> parameter, so
   * only events from confirmed blocks are checked. Even if the corresponding block
   * is imported and contains the requested <code>depositRoot</code> but is not confirmed
   * yet the method should return <code>false</code>
   * @param blockHash Block hash where depositRoot event is looked for
   * @param depositRoot The root of the <code>DepositEvent</code>
   */
  boolean hasDepositRoot(Hash32 blockHash, Hash32 depositRoot);

  /**
   * Returns the last found <code>DepositEvent</code> 'coordinates'
   * The method takes into account the <code>distanceFromHead</code> parameter, so
   * only events from confirmed blocks are considered.
   * NOTE: this is a blocking call which may execute significant amount of time since
   * it's scanning Blocks database
   * @return <code>{@link Optional#empty()}</code> if <code>ChainStart</code> event
   * is not yet issued
   */
  Optional<Eth1Data> getLatestEth1Data();

  /**
   * Sets the number of block confirmations which is required for any contract event
   * to be considered as existing
   */
  void setDistanceFromHead(long distanceFromHead);

  /**
   * Container for a pair of {@link Deposit} and its 'coordinates' {@link Eth1Data}
   */
  class DepositInfo {
    private final Deposit deposit;
    private final Eth1Data eth1Data;

    public DepositInfo(Deposit deposit, Eth1Data eth1Data) {
      this.deposit = deposit;
      this.eth1Data = eth1Data;
    }

    public Deposit getDeposit() {
      return deposit;
    }

    public Eth1Data getEth1Data() {
      return eth1Data;
    }
  }

  /**
   * Container for the deposit contract <code>ChainStartEvent</code>
   */
  class ChainStart {
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
}
