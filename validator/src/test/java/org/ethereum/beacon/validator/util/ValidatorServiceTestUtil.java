package org.ethereum.beacon.validator.util;

import java.util.Collections;
import java.util.Random;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateEx;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.util.DepositContractTestUtil;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.BeaconChainValidator;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterTestUtil;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.Bytes48;

public abstract class ValidatorServiceTestUtil {
  private ValidatorServiceTestUtil() {}

  public static BeaconChainValidator mockBeaconChainValidator(
      Random random, SpecHelpers specHelpers) {
    BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.random(random));
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    StateTransition<BeaconStateEx> perEpochTransition =
        StateTransitionTestUtil.createStateWithNoTransition();
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BeaconChainProposer proposer =
        BeaconChainProposerTestUtil.mockProposer(
            perBlockTransition, perEpochTransition, depositContract, specHelpers);
    BeaconChainAttester attester = BeaconChainAttesterTestUtil.mockAttester(specHelpers);
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();

    return Mockito.spy(
        new BeaconChainValidator(pubkey, proposer, attester, specHelpers,
            signer, Mono.empty(), Schedulers.get()));
  }
}
