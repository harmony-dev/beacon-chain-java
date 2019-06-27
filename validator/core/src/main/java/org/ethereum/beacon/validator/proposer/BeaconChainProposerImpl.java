package org.ethereum.beacon.validator.proposer;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
import org.ethereum.beacon.consensus.transition.BeaconStateExImpl;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlock.Builder;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.pow.DepositContract;
import org.ethereum.beacon.pow.DepositContract.DepositInfo;
import org.ethereum.beacon.validator.BeaconChainProposer;
import org.ethereum.beacon.validator.ValidatorService;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.collections.ReadList;

/**
 * An implementation of beacon chain proposer.
 *
 * @see BeaconChainProposer
 * @see ValidatorService
 */
public class BeaconChainProposerImpl implements BeaconChainProposer {
  /** The spec. */
  private BeaconChainSpec spec;
  /** Per-block state transition. */
  private BlockTransition<BeaconStateEx> perBlockTransition;
  /** Eth1 deposit contract. */
  private DepositContract depositContract;

  public BeaconChainProposerImpl(
      BeaconChainSpec spec,
      BlockTransition<BeaconStateEx> perBlockTransition,
      DepositContract depositContract) {
    this.spec = spec;
    this.perBlockTransition = perBlockTransition;
    this.depositContract = depositContract;
  }

  @Override
  public BeaconBlock propose(
      BeaconState state, BLSSignature randaoReveal, PendingOperations pendingOperations) {

    Hash32 parentRoot = spec.get_block_root_at_slot(state, state.getSlot().decrement());
    Eth1Data eth1Data = getEth1Data(state);
    BeaconBlockBody blockBody = getBlockBody(state, pendingOperations, randaoReveal, eth1Data);

    // create new block
    Builder builder = BeaconBlock.Builder.createEmpty();
    builder
        .withSlot(state.getSlot())
        .withParentRoot(parentRoot)
        .withStateRoot(Hash32.ZERO)
        .withSignature(BLSSignature.ZERO)
        .withBody(blockBody);

    // calculate state_root
    BeaconBlock newBlock = builder.build();
    BeaconState newState = perBlockTransition.apply(new BeaconStateExImpl(state), newBlock);
    builder.withStateRoot(spec.hash_tree_root(newState));

    return builder.build();
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
    ReadList<Integer, Eth1Data> eth1DataVotes = state.getEth1DataVotes();
    Optional<Eth1Data> contractData = depositContract.getLatestEth1Data();

    Map<Eth1Data, Integer> votes = new HashMap<>();
    for (Eth1Data eth1Data : eth1DataVotes) {
      votes.compute(eth1Data, (key, count) -> (count == null) ? 1 : count + 1);
    }

    Optional<Eth1Data> bestVote = votes.keySet().stream().max(Comparator.comparing(votes::get));

    // verify best vote data and return if verification passed,
    // otherwise, return data from the contract
    return bestVote
        .filter(
            eth1Data ->
                depositContract.hasDepositRoot(eth1Data.getBlockHash(), eth1Data.getDepositRoot()))
        // TODO throw exception if contract data can't be read
        .orElse(contractData.orElse(state.getLatestEth1Data()));
  }

  /**
   * Creates block body for new block.
   *
   * @param state state at the slot of created block.
   * @param operations pending operations instance.
   * @return {@link BeaconBlockBody} for new block.
   * @see PendingOperations
   */
  private BeaconBlockBody getBlockBody(
      BeaconState state,
      PendingOperations operations,
      BLSSignature randaoReveal,
      Eth1Data eth1Data) {
    List<ProposerSlashing> proposerSlashings =
        operations.peekProposerSlashings(spec.getConstants().getMaxProposerSlashings());
    List<AttesterSlashing> attesterSlashings =
        operations.peekAttesterSlashings(spec.getConstants().getMaxAttesterSlashings());
    List<Attestation> attestations =
        operations.peekAggregateAttestations(spec.getConstants().getMaxAttestations());
    List<VoluntaryExit> voluntaryExits =
        operations.peekExits(spec.getConstants().getMaxVoluntaryExits());
    List<Transfer> transfers = operations.peekTransfers(spec.getConstants().getMaxTransfers());

    Eth1Data latestProcessedDeposit = null; // TODO wait for spec update to include this to state
    List<Deposit> deposits =
        depositContract
            .peekDeposits(
                spec.getConstants().getMaxDeposits(),
                latestProcessedDeposit,
                state.getLatestEth1Data())
            .stream()
            .map(DepositInfo::getDeposit)
            .collect(Collectors.toList());
    Bytes32 graffiti = getGraffiti(state);

    return BeaconBlockBody.create(
        randaoReveal,
        eth1Data,
        graffiti,
        proposerSlashings,
        attesterSlashings,
        attestations,
        deposits,
        voluntaryExits,
        transfers);
  }
}
