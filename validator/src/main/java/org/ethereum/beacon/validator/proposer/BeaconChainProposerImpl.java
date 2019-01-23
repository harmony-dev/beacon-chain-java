package org.ethereum.beacon.validator.proposer;

import static java.util.Collections.emptyList;
import static org.ethereum.beacon.core.spec.SignatureDomains.PROPOSAL;
import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import java.util.List;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.StateTransition;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlock.Builder;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.CasperSlashing;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.Exit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.slashing.ProposalSignedData;
import org.ethereum.beacon.core.spec.ChainSpec;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconChainProposerImpl implements BeaconChainProposer {

  private SpecHelpers specHelpers;
  private ChainSpec chainSpec;
  private StateTransition<BeaconState> blockTransition;
  private DepositContract depositContract;

  @Override
  public BeaconBlock propose(
      UInt24 index,
      ObservableBeaconState observableState,
      Hash32 depositRoot,
      MessageSigner<Bytes96> signer) {
    BeaconState state = observableState.getLatestSlotState();

    Hash32 parentRoot = specHelpers.get_block_root(state, state.getSlot().decrement());
    Bytes96 randaoReveal = getRandaoReveal(state, index, signer);
    BeaconBlockBody blockBody = getBlockBody(state, observableState.getPendingOperations());

    // create new block
    Builder builder = BeaconBlock.Builder.createEmpty();
    builder
        .withSlot(state.getSlot())
        .withParentRoot(parentRoot)
        .withRandaoReveal(randaoReveal)
        .withDepositRoot(depositRoot)
        .withSignature(chainSpec.getEmptySignature())
        .withBody(blockBody);

    // calculate state_root
    BeaconBlock newBlock = builder.build();
    BeaconState newState = blockTransition.apply(newBlock, state);
    builder.withStateRoot(specHelpers.hash_tree_root(newState));

    // sign off on proposal
    BeaconBlock blockWithoutSignature = builder.build();
    Bytes96 signature = getProposalSignature(state, blockWithoutSignature, signer);
    builder.withSignature(signature);

    return builder.build();
  }

  private Bytes96 getProposalSignature(
      BeaconState state, BeaconBlock blockWithoutSignature, MessageSigner<Bytes96> signer) {
    ProposalSignedData proposal =
        new ProposalSignedData(
            state.getSlot(),
            chainSpec.getBeaconChainShardNumber(),
            specHelpers.hash_tree_root(blockWithoutSignature));
    Hash32 proposalRoot = specHelpers.hash_tree_root(proposal);
    Bytes8 domain = specHelpers.get_domain(state.getForkData(), state.getSlot(), PROPOSAL);
    return signer.sign(proposalRoot, domain);
  }

  private Bytes96 getRandaoReveal(BeaconState state, UInt24 index, MessageSigner<Bytes96> signer) {
    ValidatorRecord proposer = state.getValidatorRegistry().get(SpecHelpers.safeInt(index));
    Hash32 hash = Hash32.wrap(Bytes32.leftPad(proposer.getProposerSlots().toBytesBigEndian()));
    Bytes8 domain = specHelpers.get_domain(state.getForkData(), state.getSlot(), RANDAO);
    return signer.sign(hash, domain);
  }

  private BeaconBlockBody getBlockBody(BeaconState state, PendingOperations operations) {
    List<ProposerSlashing> proposerSlashings =
        operations.pullProposerSlashings(chainSpec.getMaxProposerSlashings());
    List<CasperSlashing> casperSlashings =
        operations.pullCasperSlashings(chainSpec.getMaxCasperSlashings());
    List<Attestation> attestations = operations.pullAttestations(chainSpec.getMaxAttestations());
    List<Exit> exits = operations.pullExits(chainSpec.getMaxExits());
    List<Deposit> deposits =
        depositContract.pullDeposits(
            chainSpec.getMaxDeposits(), state.getLatestDepositRoot(), UInt64.ZERO);

    return new BeaconBlockBody(
        proposerSlashings,
        casperSlashings,
        attestations,
        emptyList(),
        emptyList(),
        emptyList(),
        deposits,
        exits);
  }
}
