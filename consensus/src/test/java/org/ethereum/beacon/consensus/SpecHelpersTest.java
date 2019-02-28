package org.ethereum.beacon.consensus;

import java.util.Random;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.deposit.DepositInput;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.ShardCommittee;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Ignore;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes3;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import tech.pegasys.artemis.util.uint.UInt64;

import static org.junit.Assert.assertEquals;

public class SpecHelpersTest {

  @Test
  public void shuffleTest0() throws Exception {
    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);

    int[] sample = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

    BytesValue bytes = BytesValue.wrap(new byte[]{(byte) 1, (byte) 256, (byte) 65656});

    int expectedInt = 817593;
    Hash32 hash = Hashes.keccak256(bytes);
    int res = Bytes3.wrap(hash, 0).asUInt24BigEndian().getValue();


//    int[] actual = specHelpers.shuffle(sample, Hashes.keccak256(bytes));
//    int[] expected = new int[]{2, 4, 10, 7, 5, 6, 9, 8, 1, 3};
//
//    Assert.assertArrayEquals(expected, actual);
  }
  @Test
  public void shuffleTest1() throws Exception {
    int[] statuses = new int[]{
        2, 4, 0, 0, 2, 2, 4, 2, 3, 1, 0, 3, 3, 4, 4, 4, 1, 1, 1, 1,
        3, 2, 3, 0, 2, 4, 0, 2, 4, 0, 0, 4, 2, 1, 4, 1, 4, 2, 2, 1, 2, 4, 0, 4, 0, 3,
        0, 4, 4, 0, 0, 1, 3, 3, 0, 4, 3, 1, 1, 3, 1, 0, 0, 1, 0, 0, 4, 1, 2, 0, 1, 4,
        2, 1, 1, 4, 1, 1, 1, 1, 0, 4, 4, 0, 1, 3, 4, 2, 0, 1, 4, 3, 1, 2, 4, 2, 2, 2,
        3, 3, 3, 0, 2, 0, 4, 1, 1, 3, 0, 3, 1, 3, 4, 3, 3, 4, 0, 1, 0, 3, 3, 1, 4, 2,
        0, 3, 2, 3, 0, 4, 3, 1, 3, 3, 4, 3, 0, 0, 1, 0, 2, 4, 1, 3, 1, 3, 2, 4, 2, 2,
        0, 3, 2, 3, 1, 3, 0, 2, 1, 3, 2, 2, 1, 3, 0, 2, 1, 3, 2, 2, 2, 0, 0, 0, 3, 4,
        1, 4, 4, 3, 3, 0, 1, 2, 4, 1, 4, 0, 0, 4, 3, 2, 4, 3, 1, 2, 0, 4, 4, 2, 0, 4,
        4, 4, 4, 0, 1, 4, 4, 3, 0, 3, 2, 1, 4, 3, 0, 3, 0, 3, 1, 3, 3, 2, 3, 2, 2, 2,
        1, 0, 4, 2, 0, 4, 2, 2, 0, 1, 0, 0, 2, 0, 3, 3, 2, 4, 0, 3, 1, 0, 3, 4, 2, 4,
        0, 1, 4, 1, 0, 0, 4, 3, 3, 1, 1, 4, 1, 3, 1, 0, 4, 3, 3, 0, 2, 1, 3, 4, 1, 3,
        3, 3, 0, 4, 2, 3, 0, 0, 0, 1, 4, 3, 1, 4, 2, 0, 4, 2, 3, 0, 1, 2, 0, 4, 0, 4,
        4, 2, 1, 3, 4, 3, 2, 3, 3, 4, 3, 2, 2, 1, 3, 0, 3, 2, 1, 0, 1, 3, 2, 0, 0, 0,
        1, 1, 2, 2, 0, 3, 1, 0, 3, 2, 0, 0, 2, 3, 0, 0, 4, 4, 2, 0, 1, 1, 3, 0, 1, 0,
        1, 1, 3, 4, 0, 0, 3, 4, 4, 4, 0, 2, 4, 4, 1, 0, 2, 2, 3, 4, 4, 0, 1, 3, 2, 4,
        0, 1, 2, 1, 3, 3, 0, 3, 4, 1, 3, 1, 0, 1, 0, 4, 4, 3, 4, 1, 0, 3, 1, 3
    };
    // 148

    List<Integer> activeValidatorIndices = new ArrayList<>();
    for (int i = 0; i < statuses.length; i++) {
      if (statuses[i] == 1 || statuses[i] == 2) {
        activeValidatorIndices.add(i);
      }
    }

    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);

    Map<Integer, Long> map = Arrays.stream(statuses).boxed().collect
        (Collectors.groupingBy(Function.identity(), Collectors.counting()));


    System.out.println(map);
  }

  private DepositInput createDepositInput() {
    DepositInput depositInput =
        new DepositInput(
            BLSPubkey.wrap(Bytes48.TRUE),
            Hashes.keccak256(BytesValue.fromHexString("aa")),
            BLSSignature.wrap(Bytes96.ZERO));

    return depositInput;
  }

  @Test
  public void testHashTreeRoot1() {
    SpecHelpers specHelpers = SpecHelpers.createWithSSZHasher(ChainSpec.DEFAULT, () -> 0L);
    Hash32 expected =
        Hash32.fromHexString("0x175dfc7ad9dd6f35ef93ba1bcb94fd573ca9ce7d6e4423de3d5e9ca9dca593c8");
    Hash32 actual = specHelpers.hash_tree_root(createDepositInput());
    assertEquals(expected, actual);
  }

  @Ignore
  @Test
  public void committeeTest1() {
    int validatorCount = 4;
    int epochLength = 4;
    int shardCount = 8;
    int targetCommitteeSize = 2;
    SlotNumber genesisSlot = SlotNumber.of(1_000_000);
    Random rnd = new Random(1);
    Time genesisTime = Time.of(10 * 60);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    ChainSpec chainSpec =
        new ChainSpec() {
          @Override
          public SlotNumber.EpochLength getEpochLength() {
            return new SlotNumber.EpochLength(UInt64.valueOf(epochLength));
          }

          @Override
          public SlotNumber getGenesisSlot() {
            return genesisSlot;
          }

          @Override
          public ValidatorIndex getTargetCommitteeSize() {
            return ValidatorIndex.of(targetCommitteeSize);
          }

          @Override
          public ShardNumber getShardCount() {
            return ShardNumber.of(shardCount);
          }
        };
    SpecHelpers specHelpers = new SpecHelpers(
            chainSpec, Hashes::keccak256, SSZObjectHasher.create(Hashes::keccak256), () -> 0L) {
      @Override
      public boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature,
          Bytes8 domain) {
        return true;
      }
    };

    InitialStateTransition initialStateTransition =
        new InitialStateTransition(
            new ChainStart(genesisTime, eth1Data, TestUtils.generateRandomDepositsWithoutSig(rnd, specHelpers, validatorCount)),
            specHelpers);

    BeaconState initialState = initialStateTransition.apply(
            BeaconBlocks.createGenesis(specHelpers.getChainSpec()));
    MutableBeaconState state = initialState.createMutableCopy();

    for(int i = 1; i < 128; i++) {
      System.out.println("get_epoch_committee_count(" + i + ") = " +
          specHelpers.get_epoch_committee_count(i));
    }

    for (SlotNumber slot : genesisSlot.iterateTo(genesisSlot.plus(SlotNumber.of(epochLength)))) {
      System.out.println("Slot #" + slot
          + " beacon proposer: "
          + specHelpers.get_beacon_proposer_index(state, slot)
          + " committee: "
          + specHelpers.get_crosslink_committees_at_slot(state, slot));
    }
  }
}
