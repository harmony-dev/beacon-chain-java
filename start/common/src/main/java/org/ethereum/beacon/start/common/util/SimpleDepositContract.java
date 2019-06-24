package org.ethereum.beacon.start.common.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.pow.DepositContract;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class SimpleDepositContract implements DepositContract {
  private final ChainStart chainStart;

  public SimpleDepositContract(ChainStart chainStart) {
    this.chainStart = chainStart;
  }

  @Override
  public Publisher<ChainStart> getChainStartMono() {
    return Mono.just(chainStart);
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
