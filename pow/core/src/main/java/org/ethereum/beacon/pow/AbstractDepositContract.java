package org.ethereum.beacon.pow;

import java.util.ArrayList;
import java.util.List;
import org.ethereum.beacon.core.operations.Deposit;
import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoProcessor;
import reactor.core.publisher.ReplayProcessor;
import reactor.core.scheduler.Schedulers;

public abstract class AbstractDepositContract implements DepositContract {


  private final MonoProcessor<ChainStart> chainStartSink = MonoProcessor.create();
  private final Publisher<ChainStart> chainStartStream = chainStartSink
      .publishOn(Schedulers.single())
      .name("PowClient.chainStart");
  private final ReplayProcessor<Deposit> depositSink = ReplayProcessor.cacheLast();
  private final Publisher<Deposit> depositStream = depositSink
      .publishOn(Schedulers.single())
      .onBackpressureError()
      .name("PowClient.deposits");

  private List<Deposit> initialDeposits = new ArrayList<>();

  protected void newDeposit(byte[] previous_deposit_root, byte[] data, byte[] merkle_tree_index) {

  }

  protected void chainStart(byte[] deposit_root, byte[] time) {

  }

  @Override
  public ChainStart getChainStart() {
    return null;
  }

  @Override
  public Publisher<ChainStart> getChainStartMono() {
    return chainStartStream;
  }

  @Override
  public Publisher<Deposit> getAfterDepositsStream() {
    return depositStream;
  }
}
