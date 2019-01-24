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
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.state.Eth1DataVote;
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
  private StateTransition<BeaconState> blockTransition;
  private DepositContract depositContract;

  public BeaconChainProposerImpl(
      SpecHelpers specHelpers,
      StateTransition<BeaconState> blockTransition,
      DepositContract depositContract) {
    this.specHelpers = specHelpers;
    this.blockTransition = blockTransition;
    this.depositContract = depositContract;
  }

  @Override
  public BeaconBlock propose(
      UInt24 index, ObservableBeaconState observableState, MessageSigner<Bytes96> signer) {
    BeaconState state = observableState.getLatestSlotState();

    Hash32 parentRoot = specHelpers.get_block_root(state, state.getSlot().decrement());
    Bytes96 randaoReveal = getRandaoReveal(state, index, signer);
    Eth1Data eth1Data = getEth1Data(state);
    BeaconBlockBody blockBody = getBlockBody(state, observableState.getPendingOperations());

    // create new block
    Builder builder = BeaconBlock.Builder.createEmpty();
    builder
        .withSlot(state.getSlot())
        .withParentRoot(parentRoot)
        .withRandaoReveal(randaoReveal)
        .withEth1Data(eth1Data)
        .withSignature(specHelpers.getEmptySignature())
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
            specHelpers.getBeaconChainShardNumber(),
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

  /*
   Let D be the set of Eth1DataVote objects vote in state.eth1_data_votes.

   If D is empty:
     Let block_hash be the block hash of the ETH1_FOLLOW_DISTANCEth ancestor of the head of the
       canonical eth1.0 chain.
     Let deposit_root be the deposit root of the eth1.0 deposit contract at the block defined by
       block_hash.

   If D is nonempty:
     Let best_vote be the member of D that has the highest vote.eth1_data.vote_count,
       breaking ties by favoring block hashes with higher associated block height.
     Let block_hash = best_vote.eth1_data.block_hash.
     Let deposit_root = best_vote.eth1_data.deposit_root.

   Set block.eth1_data = Eth1Data(deposit_root=deposit_root, block_hash=block_hash).
  */
  private Eth1Data getEth1Data(BeaconState state) {
    List<Eth1DataVote> eth1DataVotes = state.getEth1DataVotes();
    Eth1Data contractData =
        depositContract.getEth1DataByDistance(specHelpers.getEth1FollowDistance());

    if (eth1DataVotes.isEmpty()) {
      return contractData;
    }

    UInt64 maxVotes =
        eth1DataVotes.stream().map(Eth1DataVote::getVoteCount).max(UInt64::compareTo).get();
    Eth1DataVote bestVote =
        eth1DataVotes.stream()
            .filter(eth1DataVote -> eth1DataVote.getVoteCount().equals(maxVotes))
            .reduce((first, second) -> second)
            .get();

    // verify best vote data and return if verification passed,
    // otherwise, return data from contract
    return depositContract
        .getEth1DataByBlockHash(bestVote.getEth1Data().getBlockHash())
        .filter(data -> data.equals(bestVote.getEth1Data()))
        .orElse(contractData);
  }

  private BeaconBlockBody getBlockBody(BeaconState state, PendingOperations operations) {
    List<ProposerSlashing> proposerSlashings =
        operations.peekProposerSlashings(specHelpers.getMaxProposerSlashings());
    List<CasperSlashing> casperSlashings =
        operations.peekCasperSlashings(specHelpers.getMaxCasperSlashings());
    List<Attestation> attestations =
        operations.peekAggregatedAttestations(specHelpers.getMaxAttestations());
    List<Exit> exits = operations.peekExits(specHelpers.getMaxExits());
    List<Deposit> deposits =
        depositContract.peekDeposits(
            specHelpers.getMaxDeposits(), state.getLatestEth1Data(), UInt64.ZERO);

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
