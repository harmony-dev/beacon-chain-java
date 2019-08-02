package org.ethereum.beacon.validator.proposer;

import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_PROPOSER;
import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.ethereum.beacon.chain.observer.ObservableBeaconState;
import org.ethereum.beacon.chain.observer.PendingOperations;
import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.BeaconStateEx;
import org.ethereum.beacon.consensus.BlockTransition;
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
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.uint.UInt64;

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
      ObservableBeaconState observableState, MessageSigner<BLSSignature> signer) {
    BeaconStateEx state = observableState.getLatestSlotState();

    Hash32 parentRoot = spec.signing_root(observableState.getHead());
    BLSSignature randaoReveal = getRandaoReveal(state, signer);
    Eth1Data eth1Data = getEth1Data(state);
    BeaconBlockBody blockBody =
        getBlockBody(state, observableState.getPendingOperations(), randaoReveal, eth1Data);

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
    BeaconState newState = perBlockTransition.apply(state, newBlock);
    builder.withStateRoot(spec.hash_tree_root(newState));

    // sign off on proposal
    BeaconBlock blockWithoutSignature = builder.build();
    BLSSignature signature = getProposalSignature(state, blockWithoutSignature, signer);
    builder.withSignature(signature);

    return builder.build();
  }

  /**
   * Signs off on a block.
   *
   * @param state state at the slot of created block.
   * @param block block
   * @param signer message signer.
   * @return signature of proposal signed data.
   */
  private BLSSignature getProposalSignature(
      BeaconState state, BeaconBlock block, MessageSigner<BLSSignature> signer) {
    Hash32 proposalRoot = spec.signing_root(block);
    UInt64 domain =
        spec.get_domain(state, BEACON_PROPOSER, spec.compute_epoch_of_slot(state.getSlot()));
    return signer.sign(proposalRoot, domain);
  }

  /**
   * Returns next RANDAO reveal.
   *
   * @param state state at the slot of created block.
   * @param signer message signer.
   * @return next RANDAO reveal.
   */
  private BLSSignature getRandaoReveal(BeaconState state, MessageSigner<BLSSignature> signer) {
    Hash32 hash = spec.hash_tree_root(spec.compute_epoch_of_slot(state.getSlot()));
    UInt64 domain = spec.get_domain(state, RANDAO, spec.compute_epoch_of_slot(state.getSlot()));
    return signer.sign(hash, domain);
  }

  /**
   * Returns Eth1 data vote.
   *
   * @param state state at the slot of proposing block.
   * @return voted eth1 data.
   */
  private Eth1Data getEth1Data(BeaconState state) {
    UInt64 previousEth1Distance = UInt64.ZERO;
    return spec.get_eth1_vote(state, previousEth1Distance,
        distance -> depositContract.getLatestEth1Data().orElse(state.getEth1Data()));
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
        operations.peekAggregateAttestations(spec.getConstants().getMaxAttestations(), spec.getConstants());
    List<VoluntaryExit> voluntaryExits =
        operations.peekExits(spec.getConstants().getMaxVoluntaryExits());
    List<Transfer> transfers = operations.peekTransfers(spec.getConstants().getMaxTransfers());

    Eth1Data latestProcessedDeposit = null; // TODO wait for spec update to include this to state
    List<Deposit> deposits =
        depositContract
            .peekDeposits(
                spec.getConstants().getMaxDeposits(),
                latestProcessedDeposit,
                state.getEth1Data())
            .stream()
            .map(DepositInfo::getDeposit)
            .collect(Collectors.toList());
    Bytes32 graffiti = getGraffiti(state);

    return new BeaconBlockBody(
        randaoReveal,
        eth1Data,
        graffiti,
        proposerSlashings,
        attesterSlashings,
        attestations,
        deposits,
        voluntaryExits,
        transfers,
        spec.getConstants());
  }
}
