package org.ethereum.beacon.validator.local.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.util.DepositContractTestUtil;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.validator.BeaconAttestationSigner;
import org.ethereum.beacon.validator.BeaconBlockSigner;
import org.ethereum.beacon.validator.BeaconChainAttester;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.MessageSignerTestUtil;
import org.ethereum.beacon.validator.RandaoGenerator;
import org.ethereum.beacon.validator.attester.BeaconAttestationSignerImpl;
import org.ethereum.beacon.validator.attester.BeaconChainAttesterTestUtil;
import org.ethereum.beacon.validator.crypto.BLS381Credentials;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import org.ethereum.beacon.validator.local.MultiValidatorService;
import org.ethereum.beacon.validator.proposer.BeaconBlockSignerImpl;
import org.ethereum.beacon.validator.proposer.BeaconChainProposerTestUtil;
import org.ethereum.beacon.validator.proposer.RandaoGeneratorImpl;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import tech.pegasys.artemis.util.bytes.Bytes48;

public abstract class ValidatorServiceTestUtil {
  private ValidatorServiceTestUtil() {}

  public static MultiValidatorService mockBeaconChainValidator(
      Random random, BeaconChainSpec spec) {
    BLSPubkey pubkey = BLSPubkey.wrap(Bytes48.random(random));
    MessageSigner<BLSSignature> signer = MessageSignerTestUtil.createBLSSigner();
    BLS381Credentials blsCredentials = new BLS381Credentials(pubkey, signer);
    return mockBeaconChainValidator(
        random,
        spec,
        Collections.singletonList(blsCredentials),
        Schedulers.createControlled());
  }


  public static MultiValidatorService mockBeaconChainValidator(
      Random random, BeaconChainSpec spec, BLS381Credentials credentials) {
    return mockBeaconChainValidator(
        random,
        spec,
        Collections.singletonList(credentials),
        Schedulers.createControlled());
  }

  public static MultiValidatorService mockBeaconChainValidator(
      Random random,
      BeaconChainSpec spec,
      List<BLS381Credentials> blsCredentials,
      Schedulers schedulers) {
    BlockTransition<BeaconStateEx> perBlockTransition =
        StateTransitionTestUtil.createPerBlockTransition();
    DepositContract depositContract =
        DepositContractTestUtil.mockDepositContract(random, Collections.emptyList());
    BeaconChainProposer proposer =
        BeaconChainProposerTestUtil.mockProposer(
            perBlockTransition, depositContract, spec);
    BeaconChainAttester attester = BeaconChainAttesterTestUtil.mockAttester(spec);
    BeaconAttestationSigner attestationSigner = new BeaconAttestationSignerImpl(spec);
    BeaconBlockSigner blockSigner = new BeaconBlockSignerImpl(spec);
    RandaoGenerator randaoGenerator = new RandaoGeneratorImpl(spec);

    return Mockito.spy(
        new MultiValidatorService(
            blsCredentials,
            proposer,
            blockSigner,
            randaoGenerator,
            attester,
            attestationSigner,
            spec,
            Mono.empty(),
            schedulers));
  }
}
