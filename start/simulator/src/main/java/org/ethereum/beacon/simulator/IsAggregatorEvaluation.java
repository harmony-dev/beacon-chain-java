package org.ethereum.beacon.simulator;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.BLS381.KeyPair;
import org.ethereum.beacon.crypto.BLS381.PrivateKey;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.crypto.util.BlsKeyPairReader;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class IsAggregatorEvaluation {

  public static void main(String[] args) {
    final UInt64 TARGET_AGGREGATORS_PER_COMMITTEE = UInt64.valueOf(16);
    UInt64 committeeSize = UInt64.valueOf(150);
    UInt64 modulo = committeeSize.dividedBy(TARGET_AGGREGATORS_PER_COMMITTEE);
    SlotNumber slot = SlotNumber.of(123);

    BeaconChainSpec spec = BeaconChainSpec.createWithDefaults();

    Set<KeyPair> sample = new HashSet<>();
    BlsKeyPairReader keyPairReader = BlsKeyPairReader.createWithDefaultSource();

    int i = 0;
    while (sample.size() < committeeSize.getValue()) {
      KeyPair keyPair = keyPairReader.next();

      Bytes96 slotSignature = computeSlotSignature(spec, slot, keyPair);
      spec.bytes_to_int(spec.hash(slotSignature).slice(0, 8));
      if (!spec.bytes_to_int(spec.hash(slotSignature).slice(0, 8))
          .modulo(modulo)
          .equals(UInt64.ZERO)) {
        sample.add(keyPair);
      }

      if (++i % 1000 == 0) {
        System.out.println(String.format("%d passed, %d collected", i, sample.size()));
      }
    }

    System.out.println(String.format("Ended on %d iteration", i));

    System.out.println(
        sample.stream()
            .map(KeyPair::getPrivate)
            .map(PrivateKey::getEncodedBytes)
            .map(Objects::toString)
            .collect(Collectors.joining(",")));
  }

  private static Bytes96 computeSlotSignature(
      BeaconChainSpec spec, SlotNumber slot, KeyPair keyPair) {
    Fork fork = new Fork(Bytes4.ZERO, Bytes4.ZERO, spec.getConstants().getGenesisEpoch());
    EpochNumber epoch = spec.compute_epoch_at_slot(slot);
    Bytes4 fork_version =
        epoch.less(fork.getEpoch()) ? fork.getPreviousVersion() : fork.getCurrentVersion();
    UInt64 domain = spec.compute_domain(SignatureDomains.BEACON_ATTESTER, fork_version);
    return BLS381
        .sign(MessageParameters.create(spec.hash_tree_root(slot), domain), keyPair)
        .getEncoded();
  }
}
