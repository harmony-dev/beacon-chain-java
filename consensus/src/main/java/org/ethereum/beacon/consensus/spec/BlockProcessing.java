package org.ethereum.beacon.consensus.spec;

import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_PROPOSER;
import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.MutableBeaconState;
import org.ethereum.beacon.core.envelops.SignedVoluntaryExit;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.deposit.DepositMessage;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.CommitteeIndex;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Block processing part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.9.2/specs/core/0_beacon-chain.md#block-processing">Block
 *     processing</a> in the spec.
 */
public interface BlockProcessing extends HelperFunction {

  default void verify_block_header(BeaconState state, BeaconBlock block) {
    /* Verify that the slots match
      assert block.slot == state.slot */
    assertTrue(block.getSlot().equals(state.getSlot()));
    /* Verify that the parent matches
      assert block.previous_block_root == hash_tree_root(state.latest_block_header) */
    assertTrue(block.getParentRoot().equals(hash_tree_root(state.getLatestBlockHeader())));

    /* Verify proposer is not slashed
    proposer = state.validator_registry[get_beacon_proposer_index(state)]
    assert not proposer.slashed */
    ValidatorRecord proposer = state.getValidators().get(get_beacon_proposer_index(state));
    assertTrue(!proposer.getSlashed());
  }

  default void process_block_header(MutableBeaconState state, BeaconBlock block) {
    /* Save current block as the new latest block
    state.latest_block_header = BeaconBlockHeader(
        slot=block.slot,
        previous_block_root=block.previous_block_root,
        block_body_root=hash_tree_root(block.body),
    ) */
    state.setLatestBlockHeader(new BeaconBlockHeader(
        block.getSlot(),
        block.getParentRoot(),
        Hash32.ZERO,
        hash_tree_root(block.getBody())));
  }

  default void verify_randao(BeaconState state, BeaconBlockBody body) {
    /* epoch = get_current_epoch(state)
      # Verify RANDAO reveal
      proposer = state.validators[get_beacon_proposer_index(state)]
      assert bls_verify(proposer.pubkey, hash_tree_root(epoch), body.randao_reveal, get_domain(state, DOMAIN_RANDAO)) */
    EpochNumber epoch = get_current_epoch(state);
    ValidatorRecord proposer = state.getValidators().get(get_beacon_proposer_index(state));
    assertTrue(
        bls_verify(
            proposer.getPubKey(),
            hash_tree_root(epoch),
            body.getRandaoReveal(),
            get_domain(state, RANDAO)));
  }

  /*
    def process_randao(state: BeaconState, body: BeaconBlockBody) -> None:
      epoch = get_current_epoch(state)
      # Verify RANDAO reveal
      proposer = state.validators[get_beacon_proposer_index(state)]
      assert bls_verify(proposer.pubkey, hash_tree_root(epoch), body.randao_reveal, get_domain(state, DOMAIN_RANDAO))
      # Mix in RANDAO reveal
      mix = xor(get_randao_mix(state, epoch), hash(body.randao_reveal))
      state.randao_mixes[epoch % EPOCHS_PER_HISTORICAL_VECTOR] = mix
   */
  default void process_randao(MutableBeaconState state, BeaconBlockBody body) {
    EpochNumber epoch = get_current_epoch(state);
    // Mix in RANDAO reveal
    Bytes32 mix = Bytes32s.xor(get_randao_mix(state, epoch), hash(body.getRandaoReveal()));
    state.getRandaoMixes().set(epoch.modulo(getConstants().getEpochsPerHistoricalVector()),
        Hash32.wrap(mix));
  }

  /*
    def process_eth1_data(state: BeaconState, block: BeaconBlock) -> None:
      state.eth1_data_votes.append(block.body.eth1_data)
      if state.eth1_data_votes.count(block.body.eth1_data) * 2 > SLOTS_PER_ETH1_VOTING_PERIOD:
          state.latest_eth1_data = block.body.eth1_data
   */
  default void process_eth1_data(MutableBeaconState state, BeaconBlockBody body) {
    state.getEth1DataVotes().add(body.getEth1Data());
    long votes_count = state.getEth1DataVotes().stream()
        .filter(v -> v.equals(body.getEth1Data()))
        .count();
    if (votes_count * 2 > getConstants().getSlotsPerEth1VotingPeriod().getValue()) {
      state.setEth1Data(body.getEth1Data());
    }
  }

  default void verify_proposer_slashing(BeaconState state, ProposerSlashing proposer_slashing) {
    checkIndexRange(state, proposer_slashing.getProposerIndex());
    ValidatorRecord proposer = state.getValidators().get(proposer_slashing.getProposerIndex());

    /*    # Verify slots match
    assert proposer_slashing.header_1.slot == proposer_slashing.header_2.slot */
    assertTrue(proposer_slashing.getSignedHeader1().getMessage().getSlot()
        .equals(proposer_slashing.getSignedHeader2().getMessage().getSlot()));

    /* But the headers are different
      assert proposer_slashing.header_1 != proposer_slashing.header_2 */
    assertTrue(!proposer_slashing.getSignedHeader1().getMessage()
        .equals(proposer_slashing.getSignedHeader2().getMessage()));

    /* Check proposer is slashable
    assert is_slashable_validator(proposer, get_current_epoch(state)) */
    assertTrue(is_slashable_validator(proposer, get_current_epoch(state)));

    /* Signatures are valid
    for header in (proposer_slashing.header_1, proposer_slashing.header_2):
        domain = get_domain(state, DOMAIN_BEACON_PROPOSER, compute_epoch_of_slot(header.slot))
        assert bls_verify(proposer.pubkey, hash_tree_root(header), header.signature, domain) */
    Stream.of(proposer_slashing.getSignedHeader1(), proposer_slashing.getSignedHeader2()).forEach(signed_header -> {
      UInt64 domain = get_domain(state, BEACON_PROPOSER, compute_epoch_at_slot(signed_header.getMessage().getSlot()));
      assertTrue(bls_verify(
          proposer.getPubKey(),
          hash_tree_root(signed_header.getMessage()),
          signed_header.getSignature(),
          domain
      ));
    });
  }

  /*
    """
    Process ``ProposerSlashing`` transaction.
    """
   */
  default void process_proposer_slashing(MutableBeaconState state, ProposerSlashing proposer_slashing) {
    slash_validator(state, proposer_slashing.getProposerIndex());
  }

  default void verify_attester_slashing(BeaconState state, AttesterSlashing attester_slashing) {
    IndexedAttestation attestation1 = attester_slashing.getAttestation1();
    IndexedAttestation attestation2 = attester_slashing.getAttestation2();

    /* assert is_slashable_attestation_data(attestation_1.data, attestation_2.data)
       assert is_valid_indexed_attestation(state, attestation_1)
       assert is_valid_indexed_attestation(state, attestation_2) */
    assertTrue(is_slashable_attestation_data(attestation1.getData(), attestation2.getData()));
    assertTrue(is_valid_indexed_attestation(state, attestation1));
    assertTrue(is_valid_indexed_attestation(state, attestation2));
  }

  /*
    """
    Process ``AttesterSlashing`` transaction.
    """
   */
  default void process_attester_slashing(MutableBeaconState state, AttesterSlashing attester_slashing) {
    IndexedAttestation attestation1 = attester_slashing.getAttestation1();
    IndexedAttestation attestation2 = attester_slashing.getAttestation2();

    /* slashed_any = False */
    boolean slashed_any = false;

    /*  indices = set(attestation_1.attesting_indices).intersection(attestation_2.attesting_indices)
        for index in sorted(indices):
        if is_slashable_validator(state.validator_registry[index], get_current_epoch(state)):
            slash_validator(state, index)
            slashed_any = True
        assert slashed_any */
    List<ValidatorIndex> intersection = new ArrayList<>(attestation1.getAttestingIndices().listCopy());
    intersection.retainAll(attestation2.getAttestingIndices().listCopy());
    intersection.sort(Comparator.comparingLong(UInt64::longValue));
    for (ValidatorIndex index : intersection) {
      if (is_slashable_validator(state.getValidators().get(index), get_current_epoch(state))) {
        slash_validator(state, index);
        slashed_any = true;
      }
    }
    assertTrue(slashed_any);
  }

  default boolean verify_attestation(BeaconState state, Attestation attestation) {
    /* data = attestation.data
       assert data.index < get_committee_count_at_slot(state, data.slot)
       assert data.target.epoch in (get_previous_epoch(state), get_current_epoch(state))
       assert data.target.epoch == compute_epoch_at_slot(data.slot) */
    AttestationData data = attestation.getData();
    if (!data.getIndex().less(new CommitteeIndex(get_committee_count_at_slot(state, data.getSlot())))) {
      return false;
    }
    if (!data.getTarget().getEpoch().equals(get_previous_epoch(state))
        && !data.getTarget().getEpoch().equals(get_current_epoch(state))) {
      return false;
    }
    if (!data.getTarget().getEpoch().equals(compute_epoch_at_slot(data.getSlot()))) {
      return false;
    }

    /* assert data.slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot <= data.slot + SLOTS_PER_EPOCH */
    if (!data.getSlot().plus(getConstants().getMinAttestationInclusionDelay()).lessEqual(state.getSlot())) {
      return false;
    }
    if (!state.getSlot().lessEqual(data.getSlot().plus(getConstants().getSlotsPerEpoch()))) {
      return false;
    }

    /* committee = get_beacon_committee(state, data.slot, data.index)
    assert len(attestation.aggregation_bits) == len(committee) */
    List<ValidatorIndex> committee =
        get_beacon_committee(state, data.getSlot(), data.getIndex());
    if (attestation.getAggregationBits().size() != committee.size()) {
      return false;
    }

    /* if data.target.epoch == get_current_epoch(state):
         assert data.source == state.current_justified_checkpoint
       else:
         assert data.source == state.previous_justified_checkpoint */
    boolean is_ffg_data_correct;
    if (data.getTarget().getEpoch().equals(get_current_epoch(state))) {
      is_ffg_data_correct = data.getSource().equals(state.getCurrentJustifiedCheckpoint());
    } else {
      is_ffg_data_correct = data.getSource().equals(state.getPreviousJustifiedCheckpoint());
    }

    if (!is_ffg_data_correct) {
      return false;
    }

    return is_valid_indexed_attestation(state, get_indexed_attestation(state, attestation));
  }

  /*
   """
   Process ``Attestation`` operation.
   """
  */
  default void process_attestation(MutableBeaconState state, Attestation attestation) {
    AttestationData data = attestation.getData();

    /*  pending_attestation = PendingAttestation(
        data=data,
        aggregation_bitfield=attestation.aggregation_bitfield,
        inclusion_delay=state.slot - data.slot,
        proposer_index=get_beacon_proposer_index(state)) */
    PendingAttestation pending_attestation = new PendingAttestation(
        attestation.getAggregationBits(),
        data,
        state.getSlot().minus(data.getSlot()),
        get_beacon_proposer_index(state),
        getConstants());
    if (data.getTarget().getEpoch().equals(get_current_epoch(state))) {
      state.getCurrentEpochAttestations().add(pending_attestation);
    } else {
      state.getPreviousEpochAttestations().add(pending_attestation);
    }
  }

  default void verify_deposit(BeaconState state, Deposit deposit) {
    if (!isVerifyDepositProof()) {
      return;
    }

    /* Verify the Merkle branch
    assert is_valid_merkle_branch(
        leaf=hash_tree_root(deposit.data),
        proof=deposit.proof,
        depth=DEPOSIT_CONTRACT_TREE_DEPTH + 1,
        index=deposit.index,
        root=state.latest_eth1_data.deposit_root,
        ) */
    assertTrue(is_valid_merkle_branch(
        hash_tree_root(deposit.getData()),
        deposit.getProof().listCopy(),
        getConstants().getDepositContractTreeDepthPlusOne(), // Add 1 for the `List` length mix-in
        state.getEth1DepositIndex(),
        state.getEth1Data().getDepositRoot()
    ));
  }

  /*
    def process_deposit(state: BeaconState, deposit: Deposit) -> None:
      """
      Process an Eth1 deposit, registering a validator or increasing its balance.
      """
    */
  default void process_deposit(MutableBeaconState state, Deposit deposit) {
    // state.deposit_index += 1
    state.setEth1DepositIndex(state.getEth1DepositIndex().increment());

    BLSPubkey pubkey = deposit.getData().getPubKey();
    Gwei amount = deposit.getData().getAmount();
    ValidatorIndex index = get_validator_index_by_pubkey(state, pubkey);

    /* if pubkey not in validator_pubkeys: */
    if (index.equals(ValidatorIndex.MAX)) {
      /* Verify the deposit signature (proof of possession).
         Invalid signatures are allowed by the deposit contract,
         and hence included on-chain, but must not be processed.
         Note: deposits are valid across forks, hence the deposit domain is retrieved directly from `compute_domain` */
      if (isBlsVerifyProofOfPossession()) {
        DepositMessage deposit_message = DepositMessage.from(deposit.getData());
        if (!bls_verify(
            pubkey,
            hash_tree_root(deposit_message),
            deposit.getData().getSignature(),
            compute_domain(SignatureDomains.DEPOSIT))) {
          return;
        }
      }

      /* Add validator and balance entries
        state.validator_registry.append(Validator(
            pubkey=pubkey,
            withdrawal_credentials=deposit.data.withdrawal_credentials,
            activation_eligibility_epoch=FAR_FUTURE_EPOCH,
            activation_epoch=FAR_FUTURE_EPOCH,
            exit_epoch=FAR_FUTURE_EPOCH,
            withdrawable_epoch=FAR_FUTURE_EPOCH,
            effective_balance=min(amount - amount % EFFECTIVE_BALANCE_INCREMENT, MAX_EFFECTIVE_BALANCE)
        ))
        state.balances.append(amount) */
      state.getValidators().add(new ValidatorRecord(
          pubkey,
          deposit.getData().getWithdrawalCredentials(),
          UInt64s.min(
              amount.minus(Gwei.castFrom(amount.modulo(getConstants().getEffectiveBalanceIncrement()))),
              getConstants().getMaxEffectiveBalance()
          ),
          Boolean.FALSE,
          getConstants().getFarFutureEpoch(),
          getConstants().getFarFutureEpoch(),
          getConstants().getFarFutureEpoch(),
          getConstants().getFarFutureEpoch()
      ));
      state.getBalances().add(amount);
    } else {
      /* Increase balance by deposit amount
          index = validator_pubkeys.index(pubkey)
          increase_balance(state, index, amount) */
      increase_balance(state, index, amount);
    }
  }

  default void verify_voluntary_exit(BeaconState state, SignedVoluntaryExit signed_exit) {
    VoluntaryExit exit = signed_exit.getMessage();

    checkIndexRange(state, exit.getValidatorIndex());
    ValidatorRecord validator = state.getValidators().get(exit.getValidatorIndex());

    /* Verify the validator is active
    assert is_active_validator(validator, get_current_epoch(state)) */
    assertTrue(is_active_validator(validator, get_current_epoch(state)));

    /* Verify the validator has not yet exited
    assert validator.exit_epoch == FAR_FUTURE_EPOCH */
    assertTrue(validator.getExitEpoch().equals(getConstants().getFarFutureEpoch()));

    /* Exits must specify an epoch when they become valid; they are not valid before then
    assert get_current_epoch(state) >= exit.epoch */
    assertTrue(get_current_epoch(state).greaterEqual(exit.getEpoch()));

    /* Verify the validator has been active long enough
    assert get_current_epoch(state) >= validator.activation_epoch + PERSISTENT_COMMITTEE_PERIOD */
    assertTrue(get_current_epoch(state).greaterEqual(
        validator.getActivationEpoch().plus(getConstants().getPersistentCommitteePeriod())));

    /* Verify signature
    domain = get_domain(state, DOMAIN_VOLUNTARY_EXIT, exit.epoch)
    assert bls_verify(validator.pubkey, hash_tree_root(exit), exit.signature, domain) */
    UInt64 domain = get_domain(state, SignatureDomains.VOLUNTARY_EXIT, exit.getEpoch());
    assertTrue(bls_verify(validator.getPubKey(), hash_tree_root(exit), signed_exit.getSignature(), domain));
  }

  /*
    """
    Process ``VoluntaryExit`` transaction.
    """
   */
  default void process_voluntary_exit(MutableBeaconState state, SignedVoluntaryExit exit) {
    initiate_validator_exit(state, exit.getMessage().getValidatorIndex());
  }

  /*
    def verify_block_state_root(state: BeaconState, block: BeaconBlock) -> None:
      assert block.state_root == hash_tree_root(state)
   */
  default void verify_block_state_root(BeaconState state, BeaconBlock block) {
    assertTrue(block.getStateRoot().equals(hash_tree_root(state)));
  }

  default void process_operations(MutableBeaconState state, BeaconBlockBody body) {
    // Verify that outstanding deposits are processed up to the maximum number of deposits
    assertTrue(
        body.getDeposits().size() ==
            Math.min(
                getConstants().getMaxDeposits(),
                state.getEth1Data().getDepositCount().minus(state.getEth1DepositIndex()).getIntValue())
    );

    /* for operations, function in (
        (body.proposer_slashings, process_proposer_slashing),
        (body.attester_slashings, process_attester_slashing),
        (body.attestations, process_attestation),
        (body.deposits, process_deposit),
        (body.voluntary_exits, process_voluntary_exit),
        # @process_shard_receipt_proofs
    ):
        for operation in operations:
            function(state, operation) */

    body.getProposerSlashings().forEach(o -> {
      verify_proposer_slashing(state, o);
      process_proposer_slashing(state, o);
    });

    body.getAttesterSlashings().forEach(o -> {
      verify_attester_slashing(state, o);
      process_attester_slashing(state, o);
    });

    body.getAttestations().forEach(o -> {
      assertTrue(verify_attestation(state, o));
      process_attestation(state, o);
    });

    body.getDeposits().forEach(o -> {
      verify_deposit(state, o);
      process_deposit(state, o);
    });

    body.getVoluntaryExits().forEach(o -> {
      verify_voluntary_exit(state, o);
      process_voluntary_exit(state, o);
    });

    // @process_shard_receipt_proofs
  }

  /*
    def process_block(state: BeaconState, block: BeaconBlock) -> None:
      process_block_header(state, block)
      process_randao(state, block.body)
      process_eth1_data(state, block.body)
      process_operations(state, block.body)
   */
  default void process_block(MutableBeaconState state, BeaconBlock block) {
    process_block_header(state, block);
    process_randao(state, block.getBody());
    process_eth1_data(state, block.getBody());
    process_operations(state, block.getBody());
  }
}
