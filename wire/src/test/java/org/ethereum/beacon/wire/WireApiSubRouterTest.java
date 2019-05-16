package org.ethereum.beacon.wire;

import static org.ethereum.beacon.core.ModelsSerializeTest.createBeaconBlock;
import static tech.pegasys.artemis.util.bytes.BytesValue.fromHexString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.ModelsSerializeTest;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.util.Utils;
import org.ethereum.beacon.wire.WireApiSubRouterTest.TestRouter.Connection;
import org.ethereum.beacon.wire.channel.beacon.WireApiSubAdapter;
import org.junit.Assert;
import org.junit.Test;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class WireApiSubRouterTest {

  static class TestRouter {
    static class Connection {
      TestRouter router1;
      TestRouter router2;
      WireApiSub outerApi1;
      WireApiSub outerApi2;

      public Connection(TestRouter router1,
          TestRouter router2, WireApiSub outerApi1, WireApiSub outerApi2) {
        this.router1 = router1;
        this.router2 = router2;
        this.outerApi1 = outerApi1;
        this.outerApi2 = outerApi2;
      }

      public void disconnect() {
        router1.removeSink.next(outerApi2);
        router2.removeSink.next(outerApi1);
      }
    }

    FluxSink<WireApiSub> addSink;
    FluxSink<WireApiSub> removeSink;
    WireApiSubRouter router;

    List<BeaconBlock> receivedBlocks = new ArrayList<>();
    List<Attestation> receivedAttestations = new ArrayList<>();

    public TestRouter() {
      router = new WireApiSubRouter(
              Flux.<WireApiSub>create(s -> addSink = s).publish(1).autoConnect(),
              Flux.<WireApiSub>create(s -> removeSink = s).publish(1).autoConnect());
      Flux.from(router.inboundBlocksStream()).subscribe(receivedBlocks::add);
      Flux.from(router.inboundAttestationsStream()).subscribe(receivedAttestations::add);
    }

    public Connection connect(TestRouter other) {
      WireApiSubAdapter thisApi = new WireApiSubAdapter();
      WireApiSubAdapter otherApi = new WireApiSubAdapter();
      thisApi.setSubClient(otherApi);
      otherApi.setSubClient(thisApi);
      this.addSink.next(otherApi);
      other.addSink.next(thisApi);

      return new Connection(this, other, thisApi, otherApi);
    }

    void clear() {
      receivedBlocks.clear();
      receivedAttestations.clear();
    }
  }

  @Test
  public void test1() {
    TestRouter router1 = new TestRouter();
    router1.router.sendProposedBlock(createBeaconBlock());
    Assert.assertTrue(router1.receivedBlocks.isEmpty());

    TestRouter router2 = new TestRouter();
    Connection connection1 = router1.connect(router2);

    router1.router.sendProposedBlock(createBeaconBlock(fromHexString("01")));
    Assert.assertTrue(router1.receivedBlocks.isEmpty());
    Assert.assertEquals(1, router2.receivedBlocks.size());

    router1.router.sendProposedBlock(createBeaconBlock(fromHexString("02")));
    Assert.assertTrue(router1.receivedBlocks.isEmpty());
    Assert.assertEquals(2, router2.receivedBlocks.size());

    router1.router.sendProposedBlock(createBeaconBlock(fromHexString("01")));
    Assert.assertTrue(router1.receivedBlocks.isEmpty());
    Assert.assertEquals(2, router2.receivedBlocks.size());

    connection1.outerApi2.sendProposedBlock(createBeaconBlock(fromHexString("01")));
    Assert.assertEquals(2, router2.receivedBlocks.size());

    connection1.disconnect();
    router2.clear();

    router1.router.sendProposedBlock(createBeaconBlock(fromHexString("03")));
    Assert.assertTrue(router1.receivedBlocks.isEmpty());
    Assert.assertTrue(router2.receivedBlocks.isEmpty());

    Connection connection2 = router1.connect(router2);

    router1.router.sendProposedBlock(createBeaconBlock(fromHexString("04")));
    Assert.assertTrue(router1.receivedBlocks.isEmpty());
    Assert.assertEquals(1, router2.receivedBlocks.size());

    router2.clear();

    TestRouter router3 = new TestRouter();
    Connection connection3 = router2.connect(router3);

    router1.router.sendProposedBlock(createBeaconBlock(fromHexString("05")));
    Assert.assertTrue(router1.receivedBlocks.isEmpty());
    Assert.assertEquals(1, router2.receivedBlocks.size());
    Assert.assertEquals(1, router3.receivedBlocks.size());
  }

  @Test
  public void testMisc() {
    BeaconBlock b1 = createBeaconBlock(fromHexString("01"));
    BeaconBlock b2 = createBeaconBlock(fromHexString("01"));
    Assert.assertTrue(b1.equals(b2));
    Assert.assertEquals(b1.hashCode(), b2.hashCode());
    HashSet<BeaconBlock> set = new HashSet<>();
    Assert.assertTrue(set.add(b1));
    Assert.assertFalse(set.add(b2));
  }
}
