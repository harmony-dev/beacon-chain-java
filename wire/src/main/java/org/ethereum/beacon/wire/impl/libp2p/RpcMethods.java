package org.ethereum.beacon.wire.impl.libp2p;

import io.libp2p.core.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import org.ethereum.beacon.consensus.hasher.ObjectHasher;
import org.ethereum.beacon.wire.WireApiSync;
import org.ethereum.beacon.wire.impl.libp2p.encoding.RpcMessageCodecFactory;
import org.ethereum.beacon.wire.message.payload.BlockRequestMessage;
import org.ethereum.beacon.wire.message.payload.BlockResponseMessage;
import org.ethereum.beacon.wire.message.payload.GoodbyeMessage;
import org.ethereum.beacon.wire.message.payload.HelloMessage;
import org.ethereum.beacon.wire.message.payload.RecentBlockRequestMessage;
import org.ethereum.beacon.wire.message.payload.RecentBlockResponseMessage;
import tech.pegasys.artemis.ethereum.core.Hash32;

public class RpcMethods {

  final Libp2pMethodHandler<HelloMessage, HelloMessage> hello;
  final Libp2pMethodHandler<GoodbyeMessage, Void> goodbye;
  final Libp2pMethodHandler<BlockRequestMessage, BlockResponseMessage> blocks;
  final Libp2pMethodHandler<RecentBlockRequestMessage, RecentBlockResponseMessage> recentBlocks;


  public RpcMethods(
      ObjectHasher<Hash32> hasher,
      RpcMessageCodecFactory codecFactory, WireApiSync server,
      BiFunction<Connection, HelloMessage, HelloMessage> helloHandler,
      BiFunction<Connection, GoodbyeMessage, Void> goodbyeHandler) {

    hello = new Libp2pMethodHandler<HelloMessage, HelloMessage>(
        "/eth2/beacon_chain/req/hello/1/ssz",
        codecFactory.create(HelloMessage.class, HelloMessage.class)) {
      @Override
      protected CompletableFuture<HelloMessage> invokeLocal(Connection connection,
          HelloMessage helloMessage) {
        return CompletableFuture.completedFuture(helloHandler.apply(connection, helloMessage));
      }
    };

    goodbye = new Libp2pMethodHandler<GoodbyeMessage, Void>(
        "/eth2/beacon_chain/req/goodbye/1/ssz",
        codecFactory.create(GoodbyeMessage.class, Void.class)) {
      @Override
      protected CompletableFuture<Void> invokeLocal(Connection connection,
          GoodbyeMessage msg) {
        goodbyeHandler.apply(connection, msg);
        return null;
      }
    }.setNotification();

    blocks = new Libp2pMethodHandler<BlockRequestMessage, BlockResponseMessage>(
        "/eth2/beacon_chain/req/beacon_blocks/1/ssz",
        codecFactory.create(BlockRequestMessage.class, BlockResponseMessage.class)) {
      @Override
      protected CompletableFuture<BlockResponseMessage> invokeLocal(Connection connection,
          BlockRequestMessage msg) {
        return server.requestBlocks(msg, hasher).thenApply(l -> new BlockResponseMessage(l.get()));
      }
    };

    recentBlocks = new Libp2pMethodHandler<RecentBlockRequestMessage, RecentBlockResponseMessage>(
        "/eth2/beacon_chain/req/recent_beacon_blocks/1/ssz",
        codecFactory.create(RecentBlockRequestMessage.class, RecentBlockResponseMessage.class)) {
      @Override
      protected CompletableFuture<RecentBlockResponseMessage> invokeLocal(Connection connection,
          RecentBlockRequestMessage msg) {
        return server.requestRecentBlocks(msg.getBlockRoots(), hasher)
            .thenApply(l -> new RecentBlockResponseMessage(l.get()));
      }
    };
  }

  public List<Libp2pMethodHandler<?, ?>> all() {
    return Arrays.asList(
        hello,
        goodbye,
        blocks,
        recentBlocks
    );
  }
}
