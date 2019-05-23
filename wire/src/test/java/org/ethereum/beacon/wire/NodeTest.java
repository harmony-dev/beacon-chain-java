package org.ethereum.beacon.wire;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.ethereum.beacon.start.common.NodeLauncher;
import org.ethereum.beacon.chain.storage.impl.MemBeaconChainStorageFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.emulator.config.ConfigBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecBuilder;
import org.ethereum.beacon.emulator.config.chainspec.SpecData;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.start.common.util.MDCControlledSchedulers;
import org.ethereum.beacon.start.common.util.SimpleDepositContract;
import org.ethereum.beacon.start.common.util.SimulateUtils;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.wire.channel.Channel;
import org.ethereum.beacon.wire.net.ConnectionManager;
import org.ethereum.beacon.wire.net.netty.NettyClient;
import org.ethereum.beacon.wire.net.netty.NettyServer;
import org.ethereum.beacon.wire.sync.SyncManager;
import org.ethereum.beacon.wire.sync.SyncManagerImpl;
import org.ethereum.beacon.wire.sync.SyncManagerImpl.SyncMode;
import org.javatuples.Pair;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

public class NodeTest {

  @Test
  public void test1() throws Exception {
    Random rnd = new Random();

    ConfigBuilder<SpecData> specConfigBuilder =
        new ConfigBuilder<>(SpecData.class)
            .addYamlConfigFromResources("/config/spec-constants.yml")
            .addYamlConfigFromResources("/test-spec-config.yml");
    SpecData specData = specConfigBuilder.build();
    SpecBuilder specBuilder = new SpecBuilder()
        .withSpec(specData);
    BeaconChainSpec spec = specBuilder.buildSpec();

    int depositCount = 16;
    Pair<List<Deposit>, List<KeyPair>> depositPairs =
        SimulateUtils.getAnyDeposits(rnd, spec, depositCount, false);

    Time genesisTime = Time.of(60000);

    MDCControlledSchedulers controlledSchedulers = new MDCControlledSchedulers();
    controlledSchedulers.setCurrentTime(genesisTime.getMillis().getValue() + 1000);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), UInt64.valueOf(depositCount), Hash32.random(rnd));

    DepositContract.ChainStart chainStart =
        new DepositContract.ChainStart(genesisTime, eth1Data, depositPairs.getValue0());
    SimpleDepositContract depositContract = new SimpleDepositContract(chainStart);

    try (NettyServer nettyServer = new NettyServer(41001)) {
      // master node with all validators
      NodeLauncher masterNode;
      {
        ControlledSchedulers schedulers = controlledSchedulers.createNew("master");
        nettyServer.start();
        ConnectionManager<SocketAddress> connectionManager = new ConnectionManager<>(
            nettyServer, null, schedulers.reactorEvents());
        masterNode = new NodeLauncher(
            specBuilder.buildSpec(),
            depositContract,
            depositPairs
                .getValue1()
                .stream()
                .map(BLS381Credentials::createWithDummySigner)
                .collect(Collectors.toList()),
            connectionManager,
            new MemBeaconChainStorageFactory(spec.getObjectHasher()),
            schedulers,
            false);
      }

      // generate some blocks
      controlledSchedulers.addTime(Duration.ofSeconds(64 * 10));

      // slave node
      ConnectionManager<SocketAddress> slaveConnectionManager;
      CompletableFuture<Channel<BytesValue>> connectFut;
      NodeLauncher slaveNode;
      {
        ControlledSchedulers schedulers = controlledSchedulers.createNew("slave");
        NettyClient nettyClient = new NettyClient();
        slaveConnectionManager = new ConnectionManager<>(
            null, nettyClient, schedulers.reactorEvents());
        slaveNode = new NodeLauncher(
            specBuilder.buildSpec(),
            depositContract,
            null,
            slaveConnectionManager,
            new MemBeaconChainStorageFactory(spec.getObjectHasher()),
            schedulers,
            true);
        connectFut = slaveConnectionManager
            .connect(InetSocketAddress.createUnresolved("localhost", 41001));
        System.out.println("Connected! " + connectFut.get());
      }

      Assert.assertEquals(
          SyncMode.Long,
          Mono.from(slaveNode.getSyncManager().getSyncModeStream()).block(Duration.ZERO));

      // generate some new blocks
      System.out.println("Generating online blocks");
      for (int i = 0; i < 10; i++) {
        controlledSchedulers.addTime(Duration.ofSeconds(1));
        Thread.sleep(100);
      }

      Flux.from(slaveNode.getSyncManager().getSyncModeStream())
          .filter(mode -> mode == SyncMode.Short)
          .blockFirst(Duration.ofSeconds(30));

      // 'realtime' mode
      System.out.println("Some time in 'realtime' mode...");
      for (int i = 0; i < 50; i++) {
        controlledSchedulers.addTime(Duration.ofSeconds(1));
        Thread.sleep(50);
      }

      // disconnecting slave
      System.out.println("Disconnecting slave");
      connectFut.get().close();

      // generate new blocks on master
      System.out.println("Generate new blocks on master");
      controlledSchedulers.addTime(Duration.ofSeconds(32 * 10));

      // connect the slave again
      System.out.println("Connect the slave again");
      CompletableFuture<Channel<BytesValue>> connectFut1 = slaveConnectionManager
          .connect(InetSocketAddress.createUnresolved("localhost", 41001));
      connectFut1.get();
      System.out.println("Slave connected");

      System.out.println("Generating online blocks");
      controlledSchedulers.addTime(Duration.ofSeconds(10 * 10));

      Flux.from(slaveNode.getSyncManager().getSyncModeStream())
          .filter(mode -> mode == SyncMode.Long)
          .blockFirst(Duration.ofSeconds(30));

      System.out.println("Some time in 'realtime' mode...");
      for (int i = 0; i < 50; i++) {
        controlledSchedulers.addTime(Duration.ofSeconds(1));
        Thread.sleep(50);
      }

      Flux.from(slaveNode.getSyncManager().getSyncModeStream())
          .filter(mode -> mode == SyncMode.Short)
          .blockFirst(Duration.ofSeconds(30));
    }
  }

  public static Integer getValue() {
    System.out.println("I am called");
    // Simulating a long network call of 1 second in the worst case
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return 10;
  }

  @Test
  public void main() {
    ScheduledExecutorService schedulerService = Executors
        .newSingleThreadScheduledExecutor();
    ExecutorService executor = new ThreadPoolExecutor(10, 10,
        0L, TimeUnit.MILLISECONDS,
        // This is an unbounded Queue. This should never be used
        // in real life. That is the first step to failure.
        new LinkedBlockingQueue<Runnable>());
    // We want to call the dummy service 10 times
    CompletableFuture<?>[] allFutures = new CompletableFuture[10];
    for (int i = 0; i < 10; ++i) {
      CompletableFuture dependencyFuture = CompletableFuture
          .supplyAsync(() -> getValue(), executor);
      CompletableFuture futureTimeout = new CompletableFuture();
      schedulerService.schedule(() ->
          futureTimeout.completeExceptionally(new TimeoutException()), 3000, TimeUnit.MILLISECONDS);
      CompletableFuture result = CompletableFuture.anyOf(dependencyFuture, futureTimeout);
      allFutures[i] = result;
    }
    // Finally wait for all futures to join
    CompletableFuture.allOf(allFutures).join();
    System.out.println("All futures completed");
    System.out.println(executor.toString());

  }
}