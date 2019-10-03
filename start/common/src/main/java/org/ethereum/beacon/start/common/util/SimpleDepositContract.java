package org.ethereum.beacon.start.common.util;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.Schedulers;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class SimpleDepositContract implements DepositContract {

  private final Logger logger = LogManager.getLogger();

  private final ChainStart chainStart;
  private final Schedulers schedulers;

  public SimpleDepositContract(ChainStart chainStart, Schedulers schedulers) {
    this.chainStart = chainStart;
    this.schedulers = schedulers;
  }

  @Override
  public Publisher<ChainStart> getChainStartMono() {
    long delay = chainStart.getTime().getMillis().getValue() - schedulers.getCurrentTime();
    if (delay > 0) {
      logger.info(
          "Genesis time {} is in the future, delaying start by {}s",
          chainStart.getTime().getValue(),
          delay / 1000);
      return Mono.delay(Duration.ofMillis(delay), schedulers.events().toReactor())
          .map(d -> chainStart);
    } else {
      return Mono.just(chainStart);
    }
  }

  @Override
  public Publisher<Deposit> getDepositStream() {
    return Mono.empty();
  }

  @Override
  public List<DepositInfo> peekDeposits(
      int maxCount, Eth1Data fromDepositExclusive, Eth1Data tillDepositInclusive) {
    return Collections.emptyList();
  }

  @Override
  public boolean hasDepositRoot(Hash32 blockHash, Hash32 depositRoot) {
    return true;
  }

  @Override
  public Optional<Eth1Data> getLatestEth1Data() {
    return Optional.of(chainStart.getEth1Data());
  }

  @Override
  public void setDistanceFromHead(long distanceFromHead) {}
}
