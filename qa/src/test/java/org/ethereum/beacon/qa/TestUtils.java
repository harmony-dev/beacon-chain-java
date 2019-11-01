package org.ethereum.beacon.qa;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.util.PseudoBLSFunctions;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.util.uint.UInt64;

public class TestUtils {
  public static BeaconChainSpec getBeaconChainSpec() {
    return BeaconChainSpec.Builder.createWithDefaultParams()
        .withConstants(new SpecConstants() {
          @Override
          public ShardNumber getShardCount() {
            return ShardNumber.of(16);
          }

          @Override
          public SlotNumber.EpochLength getSlotsPerEpoch() {
            return new SlotNumber.EpochLength(UInt64.valueOf(4));
          }
        })
        .withComputableGenesisTime(false)
        .withVerifyDepositProof(false)
        .withBlsVerifyProofOfPossession(true)
        .withBlsVerify(true)
        .withBlsFunctions(new PseudoBLSFunctions())
        .withCache(false)
        .build();
  }

}
