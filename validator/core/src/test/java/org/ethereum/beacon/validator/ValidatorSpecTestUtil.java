package org.ethereum.beacon.validator;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.validator.crypto.MessageSigner;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ValidatorSpecTestUtil {

  public static List<ValidatorIndex> getCommittee(int size) {
    return IntStream.range(0, size).mapToObj(ValidatorIndex::of).collect(Collectors.toList());
  }

  public static boolean verifySignature(
      BeaconChainSpec spec,
      BeaconState initialState,
      BeaconBlock block,
      MessageSigner<BLSSignature> signer) {

    BLSSignature expectedSignature =
        signer.sign(
            spec.signing_root(block),
            spec.get_domain(initialState, SignatureDomains.BEACON_PROPOSER));

    return expectedSignature.equals(block.getSignature());
  }
}
