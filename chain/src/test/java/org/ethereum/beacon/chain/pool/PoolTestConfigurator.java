package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.chain.BeaconTuple;
import org.ethereum.beacon.chain.DefaultBeaconChain;
import org.ethereum.beacon.chain.MutableBeaconChain;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.impl.SSZBeaconChainStorageFactory;
import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.ChainStart;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.consensus.transition.EmptySlotTransition;
import org.ethereum.beacon.consensus.transition.ExtendedSlotTransition;
import org.ethereum.beacon.consensus.transition.InitialStateTransition;
import org.ethereum.beacon.consensus.transition.PerEpochTransition;
import org.ethereum.beacon.consensus.transition.PerSlotTransition;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.consensus.verifier.BeaconBlockVerifier;
import org.ethereum.beacon.consensus.verifier.BeaconStateVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.deposit.DepositData;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.ShardNumber;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.Time;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.MessageParameters;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.schedulers.ControlledSchedulers;
import org.ethereum.beacon.schedulers.Schedulers;
import reactor.core.publisher.DirectProcessor;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Collections;
import java.util.Random;

import static org.ethereum.beacon.core.spec.SignatureDomains.DEPOSIT;

public class PoolTestConfigurator {

    protected final SpecConstants specConstants =
            new SpecConstants() {
                @Override
                public SlotNumber getGenesisSlot() {
                    return SlotNumber.of(12345);
                }

                @Override
                public Time getSecondsPerSlot() {
                    return Time.of(1);
                }
            };

    protected final BeaconChainSpec spec = BeaconChainSpec.Builder.createWithDefaultParams()
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
            .withBlsVerifyProofOfPossession(false)
            .withBlsVerify(false)
            .withCache(true)
            .build();

    protected StateTransition<BeaconStateEx> perSlotTransition = new PerSlotTransition(spec);
    protected final DirectProcessor<Checkpoint> finalizedCheckpoints = DirectProcessor.create();
    protected final ControlledSchedulers schedulers = Schedulers.createControlled();
    protected final DirectProcessor<ReceivedAttestation> source = DirectProcessor.create();

    protected Attestation createAttestation(BytesValue someValue) {
        return createAttestation(someValue, createAttestationData());
    }

    protected Attestation createAttestation(BytesValue someValue, AttestationData attestationData) {
        final Random rnd = new Random();
        BLS381.KeyPair keyPair = BLS381.KeyPair.create(BLS381.PrivateKey.create(Bytes32.random(rnd)));
        Hash32 withdrawalCredentials = Hash32.random(rnd);
        DepositData depositDataWithoutSignature = new DepositData(
                BLSPubkey.wrap(Bytes48.leftPad(keyPair.getPublic().getEncodedBytes())),
                withdrawalCredentials,
                spec.getConstants().getMaxEffectiveBalance(),
                BLSSignature.wrap(Bytes96.ZERO)
        );
        Hash32 msgHash = spec.signing_root(depositDataWithoutSignature);
        UInt64 domain = spec.compute_domain(DEPOSIT, Bytes4.ZERO);
        BLS381.Signature signature = BLS381
                .sign(MessageParameters.create(msgHash, domain), keyPair);
        return new Attestation(
                Bitlist.of(someValue.size() * 8, someValue, specConstants.getMaxValidatorsPerCommittee().getValue()),
                attestationData,
                Bitlist.of(8, BytesValue.fromHexString("bb"), specConstants.getMaxValidatorsPerCommittee().getValue()),
                BLSSignature.wrap(signature.getEncoded()),
                specConstants);
    }

    private AttestationData createAttestationData() {

        return new AttestationData(
                Hashes.sha256(BytesValue.fromHexString("aa")),
                new Checkpoint(EpochNumber.of(231), Hashes.sha256(BytesValue.fromHexString("bb"))),
                new Checkpoint(EpochNumber.of(232), Hashes.sha256(BytesValue.fromHexString("cc"))),
                Crosslink.EMPTY);
    }

    protected BeaconTuple createBlock(
            BeaconTuple parent,
            BeaconChainSpec spec, long currentTime,
            StateTransition<BeaconStateEx> perSlotTransition) {
        BeaconBlock block =
                new BeaconBlock(
                        spec.get_current_slot(parent.getState(), currentTime),
                        spec.signing_root(parent.getBlock()),
                        Hash32.ZERO,
                        BeaconBlockBody.getEmpty(spec.getConstants()),
                        BLSSignature.ZERO);
        BeaconState state = perSlotTransition.apply(new BeaconStateExImpl(parent.getState()));

        return BeaconTuple.of(
                block.withStateRoot(spec.hash_tree_root(state)), new BeaconStateExImpl(state));
    }

    protected MutableBeaconChain createBeaconChain(BeaconChainSpec spec, StateTransition<BeaconStateEx> perSlotTransition, Schedulers schedulers) {

        final Time start = Time.castFrom(UInt64.valueOf(schedulers.getCurrentTime() / 1000));
        final ChainStart chainStart = new ChainStart(start, Eth1Data.EMPTY, Collections.emptyList());

        final BlockTransition<BeaconStateEx> initialTransition = new InitialStateTransition(chainStart, spec);
        final BlockTransition<BeaconStateEx> perBlockTransition = StateTransitionTestUtil.createPerBlockTransition();
        final StateTransition<BeaconStateEx> perEpochTransition = StateTransitionTestUtil.createStateWithNoTransition();

        final BeaconBlockVerifier blockVerifier = (block, state) -> VerificationResult.PASSED;
        final BeaconStateVerifier stateVerifier = (block, state) -> VerificationResult.PASSED;

        final Database database = Database.inMemoryDB();
        final SerializerFactory ssz = SerializerFactory.createSSZ(spec.getConstants());
        final BeaconChainStorage chainStorage = new SSZBeaconChainStorageFactory(spec.getObjectHasher(), ssz)
                .create(database);

        final EmptySlotTransition preBlockTransition = new EmptySlotTransition(
                new ExtendedSlotTransition(new PerEpochTransition(spec) {
                    @Override
                    public BeaconStateEx apply(BeaconStateEx stateEx) {
                        return perEpochTransition.apply(stateEx);
                    }
                }, perSlotTransition, spec));

        return new DefaultBeaconChain(
                spec,
                preBlockTransition,
                perBlockTransition,
                blockVerifier,
                stateVerifier,
                chainStorage,
                schedulers);
    }
}
