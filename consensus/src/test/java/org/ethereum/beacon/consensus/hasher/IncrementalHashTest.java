package org.ethereum.beacon.consensus.hasher;

import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.TestUtils;
import org.ethereum.beacon.consensus.hasher.SSZObjectHasher;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.util.CachingSpecHelpers;
import org.ethereum.beacon.core.BeaconBlocks;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.pow.DepositContract.ChainStart;
import org.junit.Test;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.uint.UInt64;

public class IncrementalHashTest {

  @Test
  public void test1() {
    int validatorCount = 8;
    int epochLength = 4;
    int shardCount = 8;
    int targetCommitteeSize = 4;
    SlotNumber genesisSlot = SlotNumber.of(1_000_000);
    Random rnd = new Random(1);
    Time genesisTime = Time.of(10 * 60);

    Eth1Data eth1Data = new Eth1Data(Hash32.random(rnd), Hash32.random(rnd));

    SpecConstants specConstants =
        new SpecConstants() {
          @Override
          public SlotNumber.EpochLength getSlotsPerEpoch() {
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
    SpecHelpers specHelpers = new CachingSpecHelpers(
        specConstants, Hashes::keccak256, SSZObjectHasher.create(Hashes::keccak256)) {
      @Override
      public boolean bls_verify(BLSPubkey publicKey, Hash32 message, BLSSignature signature,
          Bytes8 domain) {
        return true;
      }
    };

    System.out.println("Generating deposits...");
    List<Deposit> deposits = TestUtils
        .generateRandomDepositsWithoutSig(rnd, specHelpers, validatorCount);
    InitialStateTransition initialStateTransition =
        new InitialStateTransition(new ChainStart(genesisTime, eth1Data, deposits), specHelpers);

    System.out.println("Applying initial state transition...");
    BeaconState initialState = initialStateTransition.apply(
        BeaconBlocks.createGenesis(specHelpers.getConstants()));
    BeaconState state = initialState;

    while (true) {
      long s = System.currentTimeMillis();
      for (int i = 0; i < 5; i++) {
        specHelpers.hash_tree_root(state);
        MutableBeaconState mutableCopy = state.createMutableCopy();
        mutableCopy.getValidatorBalances().update(ValidatorIndex.of(i), b -> b.plus(Gwei.of(1)));
        state = mutableCopy.createImmutable();
      }
      System.out.println(System.currentTimeMillis() - s);
    }
  }
}
