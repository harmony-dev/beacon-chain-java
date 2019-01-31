package org.ethereum.beacon.validator.proposer;

import static java.util.Collections.emptyList;
import static org.ethereum.beacon.core.spec.SignatureDomains.PROPOSAL;
import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.ValidatorService;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * An implementation of beacon chain proposer.
 *
 * @see BeaconChainProposer
 * @see ValidatorService
 */
public class BeaconChainProposerImpl implements BeaconChainProposer {

  /** The spec. */
  private SpecHelpers specHelpers;
  /** Chain parameters. */
  private ChainSpec chainSpec;
  /** State transition that is regularly applied to a block during processing. */
  private StateTransition<BeaconState> blockTransition;
  /** Eth1 deposit contract. */
  private DepositContract depositContract;

  public BeaconChainProposerImpl(
      SpecHelpers specHelpers,
      ChainSpec chainSpec,
      StateTransition<BeaconState> blockTransition,
      DepositContract depositContract) {
    this.specHelpers = specHelpers;
    this.chainSpec = chainSpec;
    this.blockTransition = blockTransition;
    this.depositContract = depositContract;
  }

  @Override
  public BeaconBlock propose(ObservableBeaconState observableState, MessageSigner<Bytes96> signer) {
    BeaconState state = observableState.getLatestSlotState();

    Hash32 parentRoot = specHelpers.get_block_root(state, state.getSlot().decrement());
    Bytes96 randaoReveal = getRandaoReveal(state, signer);
    Eth1Data eth1Data = getEth1Data(state);
    BeaconBlockBody blockBody = getBlockBody(state, observableState.getPendingOperations());

    // create new block
    Builder builder = BeaconBlock.Builder.createEmpty();
    builder
        .withSlot(state.getSlot())
        .withParentRoot(parentRoot)
        .withRandaoReveal(randaoReveal)
        .withEth1Data(eth1Data)
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

  /**
   * Creates a {@link ProposalSignedData} instance and signs off on it.
   *
   * @param state state at the slot of created block.
   * @param blockWithoutSignature created block with zero {@link BeaconBlock#signature} field.
   * @param signer message signer.
   * @return signature of proposal signed data.
   */
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

  /**
   * Returns next RANDAO reveal.
   *
   * @param state state at the slot of created block.
   * @param signer message signer.
   * @return next RANDAO reveal.
   */
  private Bytes96 getRandaoReveal(BeaconState state, MessageSigner<Bytes96> signer) {
    Hash32 hash =
        Hash32.wrap(Bytes32.leftPad(specHelpers.get_current_epoch(state).toBytesBigEndian()));
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
    Optional<Eth1Data> contractData = depositContract.getLatestEth1Data();

    if (eth1DataVotes.isEmpty()) {
      return contractData.orElse(state.getLatestEth1Data());
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
    if (depositContract.hasDepositRoot(
        bestVote.getEth1Data().getBlockHash(), bestVote.getEth1Data().getDepositRoot())) {
      return bestVote.getEth1Data();
    } else {
      return contractData.orElse(state.getLatestEth1Data());
    }
  }

  /**
   * Creates block body for new block.
   *
   * @param state state at the slot of created block.
   * @param operations pending operations instance.
   * @return {@link BeaconBlockBody} for new block.
   * @see PendingOperations
   */
  private BeaconBlockBody getBlockBody(BeaconState state, PendingOperations operations) {
    List<ProposerSlashing> proposerSlashings =
        operations.peekProposerSlashings(chainSpec.getMaxProposerSlashings());
    List<CasperSlashing> casperSlashings =
        operations.peekCasperSlashings(chainSpec.getMaxCasperSlashings());
    List<Attestation> attestations =
        operations.peekAggregatedAttestations(
            chainSpec.getMaxAttestations(),
            state.getSlot().plus(chainSpec.getMinAttestationInclusionDelay()));
    List<Exit> exits = operations.peekExits(chainSpec.getMaxExits());

    Eth1Data latestProcessedDeposit = null; // TODO wait for spec update to include this to state
    List<Deposit> deposits =
        depositContract
            .peekDeposits(
                chainSpec.getMaxDeposits(), latestProcessedDeposit, state.getLatestEth1Data())
            .stream()
            .map(DepositInfo::getDeposit)
            .collect(Collectors.toList());

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
