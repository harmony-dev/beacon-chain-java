package org.ethereum.beacon.chain.pool;

import org.ethereum.beacon.chain.*;
import org.ethereum.beacon.chain.pool.checker.*;
import org.ethereum.beacon.chain.pool.registry.*;
import org.ethereum.beacon.chain.pool.verifier.*;
import org.ethereum.beacon.chain.storage.BeaconChainStorage;
import org.ethereum.beacon.chain.storage.impl.*;
import org.ethereum.beacon.consensus.*;
import org.ethereum.beacon.consensus.transition.*;
import org.ethereum.beacon.consensus.util.StateTransitionTestUtil;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.consensus.verifier.*;
import org.ethereum.beacon.core.*;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.*;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.*;
import org.ethereum.beacon.core.types.*;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.db.*;
import org.ethereum.beacon.schedulers.Schedulers;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.*;
import tech.pegasys.artemis.util.collections.Bitlist;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Collections;

import static org.ethereum.beacon.chain.pool.AttestationPool.*;

class InMemoryAttestationPoolTest {

    private Schedulers schedulers = Schedulers.createControlled();

    private SpecConstants specConstants =
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
    private BeaconChainSpec spec = BeaconChainSpec.Builder.createWithDefaultParams()
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

    private InMemoryDatabase db = new InMemoryDatabase();
    private BeaconChainStorage beaconChainStorage =
            new SSZBeaconChainStorageFactory(
                    spec.getObjectHasher(), SerializerFactory.createSSZ(specConstants))
                    .create(db);

    private PerEpochTransition perEpochTransition = new PerEpochTransition(spec);
    private StateTransition<BeaconStateEx> perSlotTransition = new PerSlotTransition(spec);
    private EmptySlotTransition slotTransition =
            new EmptySlotTransition(
                    new ExtendedSlotTransition(perEpochTransition, perSlotTransition, spec));

    @Test
    void integrationTest() {
        final MutableBeaconChain beaconChain = createBeaconChain(spec, perSlotTransition, schedulers);
        beaconChain.init();
        final BeaconTuple recentlyProcessed = beaconChain.getRecentlyProcessed();
        final BeaconBlock aBlock = createBlock(recentlyProcessed, spec,
                schedulers.getCurrentTime(), perSlotTransition);

        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        final Publisher<ReceivedAttestation> source = Flux.just(attestation);
        StepVerifier.create(source)
                .expectNext(attestation)
                .expectComplete()
                .verify();

        final SlotNumber slotNumber = SlotNumber.of(100L);
        final Publisher<SlotNumber> newSlots = Flux.just(slotNumber);
        StepVerifier.create(newSlots)
                .expectNext(slotNumber)
                .expectComplete()
                .verify();

        final Checkpoint checkpoint = Checkpoint.EMPTY;
        final Publisher<Checkpoint> finalizedCheckpoints = Flux.just(checkpoint);
        StepVerifier.create(finalizedCheckpoints)
                .expectNext(checkpoint)
                .expectComplete()
                .verify();

        final Publisher<BeaconBlock> importedBlocks = Flux.just(aBlock);
        StepVerifier.create(importedBlocks)
                .expectNext(aBlock)
                .expectComplete()
                .verify();

        final Publisher<BeaconBlock> chainHeads = Flux.just(aBlock);
        StepVerifier.create(chainHeads)
                .expectNext(aBlock)
                .expectComplete()
                .verify();

        final AttestationPool pool = AttestationPool.create(
                source,
                newSlots,
                finalizedCheckpoints,
                importedBlocks,
                chainHeads,
                schedulers,
                spec,
                beaconChainStorage,
                slotTransition
        );

        pool.start();
    }

    @Test
    void integrationTest2() {
        final MutableBeaconChain beaconChain = createBeaconChain(spec, perSlotTransition, schedulers);
        beaconChain.init();
        final BeaconTuple recentlyProcessed = beaconChain.getRecentlyProcessed();
        final BeaconBlock aBlock = createBlock(recentlyProcessed, spec,
                schedulers.getCurrentTime(), perSlotTransition);

        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.fromHexString("aa"));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);
        final Publisher<ReceivedAttestation> source = Flux.just(attestation);
        StepVerifier.create(source)
                .expectNext(attestation)
                .expectComplete()
                .verify();

        final SlotNumber slotNumber = SlotNumber.of(100L);
        final Publisher<SlotNumber> newSlots = Flux.just(slotNumber);
        StepVerifier.create(newSlots)
                .expectNext(slotNumber)
                .expectComplete()
                .verify();

        final Checkpoint checkpoint = Checkpoint.EMPTY;
        final Publisher<Checkpoint> finalizedCheckpoints = Flux.just(checkpoint);
        StepVerifier.create(finalizedCheckpoints)
                .expectNext(checkpoint)
                .expectComplete()
                .verify();

        final Publisher<BeaconBlock> importedBlocks = Flux.just(aBlock);
        StepVerifier.create(importedBlocks)
                .expectNext(aBlock)
                .expectComplete()
                .verify();

        final Publisher<BeaconBlock> chainHeads = Flux.just(aBlock);
        StepVerifier.create(chainHeads)
                .expectNext(aBlock)
                .expectComplete()
                .verify();

        final TimeFrameFilter timeFrameFilter = new TimeFrameFilter(spec, MAX_ATTESTATION_LOOKAHEAD);
        timeFrameFilter.feedFinalizedCheckpoint(checkpoint);
        timeFrameFilter.feedNewSlot(slotNumber);

        final SanityChecker sanityChecker = new SanityChecker(spec);
        final SignatureEncodingChecker encodingChecker = new SignatureEncodingChecker();
        final ProcessedAttestations processedFilter =
                new ProcessedAttestations(spec::hash_tree_root, MAX_PROCESSED_ATTESTATIONS);
        final UnknownAttestationPool unknownAttestationPool =
                new UnknownAttestationPool(
                        beaconChainStorage.getBlockStorage(), spec, MAX_ATTESTATION_LOOKAHEAD, MAX_UNKNOWN_ATTESTATIONS);
        final BatchVerifier batchVerifier =
                new AttestationVerifier(beaconChainStorage.getTupleStorage(), spec, slotTransition);

        final AttestationPool pool = new InMemoryAttestationPool(
                source,
                newSlots,
                finalizedCheckpoints,
                importedBlocks,
                chainHeads,
                schedulers,
                timeFrameFilter,
                sanityChecker,
                encodingChecker,
                processedFilter,
                unknownAttestationPool,
                batchVerifier);

        pool.start();
    }

    private MutableBeaconChain createBeaconChain(
            BeaconChainSpec spec, StateTransition<BeaconStateEx> perSlotTransition, Schedulers schedulers) {
        Time start = Time.castFrom(UInt64.valueOf(schedulers.getCurrentTime() / 1000));
        ChainStart chainStart = new ChainStart(start, Eth1Data.EMPTY, Collections.emptyList());
        BlockTransition<BeaconStateEx> initialTransition =
                new InitialStateTransition(chainStart, spec);
        BlockTransition<BeaconStateEx> perBlockTransition =
                StateTransitionTestUtil.createPerBlockTransition();
        StateTransition<BeaconStateEx> perEpochTransition =
                StateTransitionTestUtil.createStateWithNoTransition();

        BeaconBlockVerifier blockVerifier = (block, state) -> VerificationResult.PASSED;
        BeaconStateVerifier stateVerifier = (block, state) -> VerificationResult.PASSED;
        Database database = Database.inMemoryDB();
        BeaconChainStorage chainStorage = new SSZBeaconChainStorageFactory(
                spec.getObjectHasher(), SerializerFactory.createSSZ(spec.getConstants()))
                .create(database);

        return new DefaultBeaconChain(
                spec,
                initialTransition,
                new EmptySlotTransition(
                        new ExtendedSlotTransition(new PerEpochTransition(spec) {
                            @Override
                            public BeaconStateEx apply(BeaconStateEx stateEx) {
                                return perEpochTransition.apply(stateEx);
                            }
                        }, perSlotTransition, spec)),
                perBlockTransition,
                blockVerifier,
                stateVerifier,
                chainStorage,
                schedulers);
    }

    private BeaconBlock createBlock(
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

        return block.withStateRoot(spec.hash_tree_root(state));
    }

    private Attestation createAttestation(BytesValue someValue) {
        final AttestationData attestationData = createAttestationData();
        final Attestation attestation =
                new Attestation(
                        Bitlist.of(someValue.size() * 8, someValue, specConstants.getMaxValidatorsPerCommittee().getValue()),
                        attestationData,
                        Bitlist.of(8, BytesValue.fromHexString("bb"), specConstants.getMaxValidatorsPerCommittee().getValue()),
                        BLSSignature.wrap(Bytes96.fromHexString("cc")),
                        specConstants);

        return attestation;
    }

    public AttestationData createAttestationData() {
        final AttestationData expected =
                new AttestationData(
                        Hashes.sha256(BytesValue.fromHexString("aa")),
                        new Checkpoint(EpochNumber.of(231), Hashes.sha256(BytesValue.fromHexString("bb"))),
                        new Checkpoint(EpochNumber.of(1), Hashes.sha256(BytesValue.fromHexString("cc"))),
                        Crosslink.EMPTY);

        return expected;
    }

    @Test
    void testValidAttestation() {

    }
}
