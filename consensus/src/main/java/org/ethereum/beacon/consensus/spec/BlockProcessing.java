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
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.Deposit;
import org.ethereum.beacon.core.operations.ProposerSlashing;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.operations.VoluntaryExit;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.operations.slashing.AttesterSlashing;
import org.ethereum.beacon.core.operations.slashing.IndexedAttestation;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.PendingAttestation;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.BLSPubkey;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.core.types.ValidatorIndex;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32s;
import tech.pegasys.artemis.util.uint.UInt64;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Block processing part.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/v0.7.1/specs/core/0_beacon-chain.md#block-processing">Block
 *     processing</a> in the spec.
 */
public interface BlockProcessing extends HelperFunction {

  default void verify_block_header(BeaconState state, BeaconBlock block) {
    /* Verify that the slots match
      assert block.slot == state.slot */
    assertTrue(block.getSlot().equals(state.getSlot()));
    /* Verify that the parent matches
      assert block.previous_block_root == signing_root(state.latest_block_header) */
    assertTrue(block.getParentRoot().equals(signing_root(state.getLatestBlockHeader())));

    /* Verify proposer is not slashed
    proposer = state.validator_registry[get_beacon_proposer_index(state)]
    assert not proposer.slashed */
    ValidatorRecord proposer = state.getValidatorRegistry().get(get_beacon_proposer_index(state));
    assertTrue(!proposer.getSlashed());

    /* Verify proposer signature
    assert bls_verify(proposer.pubkey, signing_root(block), block.signature, get_domain(state, DOMAIN_BEACON_PROPOSER)) */
    assertTrue(bls_verify(
        proposer.getPubKey(),
        signing_root(block),
        block.getSignature(),
        get_domain(state, BEACON_PROPOSER)
    ));
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
        hash_tree_root(block.getBody()),
        BLSSignature.ZERO));
  }

  default void verify_randao(BeaconState state, BeaconBlockBody body) {
    /* proposer = state.validator_registry[get_beacon_proposer_index(state)]
    Verify that the provided randao value is valid
    assert bls_verify(proposer.pubkey, hash_tree_root(get_current_epoch(state)), block.body.randao_reveal, get_domain(state, DOMAIN_RANDAO)) */
    ValidatorRecord proposer = state.getValidatorRegistry().get(get_beacon_proposer_index(state));
    assertTrue(
        bls_verify(
            proposer.getPubKey(),
            hash_tree_root(get_current_epoch(state)),
            body.getRandaoReveal(),
            get_domain(state, RANDAO)
        )
    );
  }

  default void process_randao(MutableBeaconState state, BeaconBlockBody body) {
    // Mix it in
    state.getLatestRandaoMixes().set(get_current_epoch(state).modulo(getConstants().getEpochsPerHistoricalVector()),
        Hash32.wrap(Bytes32s.xor(
            get_randao_mix(state, get_current_epoch(state)),
            hash(body.getRandaoReveal()))));
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
      state.setLatestEth1Data(body.getEth1Data());
    }
  }

  default void verify_proposer_slashing(BeaconState state, ProposerSlashing proposer_slashing) {
    checkIndexRange(state, proposer_slashing.getProposerIndex());
    ValidatorRecord proposer = state.getValidatorRegistry().get(proposer_slashing.getProposerIndex());

    /* Verify that the epoch is the same
      assert slot_to_epoch(proposer_slashing.header_1.slot) == slot_to_epoch(proposer_slashing.header_2.slot) */
    assertTrue(slot_to_epoch(proposer_slashing.getHeader1().getSlot())
        .equals(slot_to_epoch(proposer_slashing.getHeader2().getSlot())));

    /* But the headers are different
      assert proposer_slashing.header_1 != proposer_slashing.header_2 */
    assertTrue(!proposer_slashing.getHeader1().equals(proposer_slashing.getHeader2()));

    /* Check proposer is slashable
    assert is_slashable_validator(proposer, get_current_epoch(state)) */
    assertTrue(is_slashable_validator(proposer, get_current_epoch(state)));

    /* Signatures are valid
    for header in (proposer_slashing.header_1, proposer_slashing.header_2):
        domain = get_domain(state, DOMAIN_BEACON_PROPOSER, slot_to_epoch(header.slot))
        assert bls_verify(proposer.pubkey, signing_root(header), header.signature, domain) */
    Stream.of(proposer_slashing.getHeader1(), proposer_slashing.getHeader2()).forEach(header -> {
      UInt64 domain = get_domain(state, BEACON_PROPOSER, slot_to_epoch(header.getSlot()));
      assertTrue(bls_verify(
          proposer.getPubKey(),
          signing_root(header),
          header.getSignature(),
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
       assert validate_indexed_attestation(state, attestation_1)
       assert validate_indexed_attestation(state, attestation_2) */
    assertTrue(is_slashable_attestation_data(attestation1.getData(), attestation2.getData()));
    assertTrue(validate_indexed_attestation(state, attestation1));
    assertTrue(validate_indexed_attestation(state, attestation2));
  }

  /*
    """
    Process ``AttesterSlashing`` transaction.
    """
   */
  default void process_attester_slashing(MutableBeaconState state, AttesterSlashing attester_slashing) {
    IndexedAttestation attestation1 = attester_slashing.getAttestation1();
    IndexedAttestation attestation2 = attester_slashing.getAttestation2();


    /* slashed_any = False
       attesting_indices_1 = attestation1.custody_bit_0_indices + attestation1.custody_bit_1_indices
       attesting_indices_2 = attestation2.custody_bit_0_indices + attestation2.custody_bit_1_indices */
    boolean slashed_any = false;
    List<ValidatorIndex> attesting_indices_1 = new ArrayList<>();
    attesting_indices_1.addAll(attestation1.getCustodyBit0Indices().listCopy());
    attesting_indices_1.addAll(attestation1.getCustodyBit1Indices().listCopy());
    List<ValidatorIndex> attesting_indices_2 = new ArrayList<>();
    attesting_indices_2.addAll(attestation2.getCustodyBit0Indices().listCopy());
    attesting_indices_2.addAll(attestation2.getCustodyBit1Indices().listCopy());

    /*  for index in set(attesting_indices_1).intersection(attesting_indices_2):
        if is_slashable_validator(state.validator_registry[index], get_current_epoch(state)):
            slash_validator(state, index)
            slashed_any = True
        assert slashed_any */
    List<ValidatorIndex> intersection = new ArrayList<>(attesting_indices_1);
    intersection.retainAll(attesting_indices_2);
    intersection.sort(Comparator.comparingLong(UInt64::longValue));
    for (ValidatorIndex index : intersection) {
      if (is_slashable_validator(state.getValidatorRegistry().get(index), get_current_epoch(state))) {
        slash_validator(state, index);
        slashed_any = true;
      }
    }
    assertTrue(slashed_any);
  }

  default boolean verify_attestation(BeaconState state, Attestation attestation) {
    AttestationData data = attestation.getData();

    /* attestation_slot = get_attestation_data_slot(state, data)
       assert attestation_slot + MIN_ATTESTATION_INCLUSION_DELAY <= state.slot <= attestation_slot + SLOTS_PER_EPOCH */
    SlotNumber attestation_slot = get_attestation_data_slot(state, data);

    if (!attestation_slot.plus(getConstants().getMinAttestationInclusionDelay()).lessEqual(state.getSlot())) {
      return false;
    }
    if (!state.getSlot().lessEqual(attestation_slot.plus(getConstants().getSlotsPerEpoch()))) {
      return false;
    }

    // assert data.target_epoch in (get_previous_epoch(state), get_current_epoch(state))
    if (!data.getTarget().getEpoch().equals(get_previous_epoch(state))
        && !data.getTarget().getEpoch().equals(get_current_epoch(state))) {
      return false;
    }

    /* if data.target_epoch == get_current_epoch(state):
        ffg_data = (state.current_justified_epoch, state.current_justified_root, get_current_epoch(state))
        parent_crosslink = state.current_crosslinks[data.crosslink.shard]
       else:
        ffg_data = (state.previous_justified_epoch, state.previous_justified_root, get_previous_epoch(state))
        parent_crosslink = state.previous_crosslinks[data.crosslink.shard] */
    Crosslink parent_crosslink;
    boolean is_ffg_data_correct;
    if (data.getTarget().getEpoch().equals(get_current_epoch(state))) {
      is_ffg_data_correct = data.getTarget().getEpoch().equals(get_current_epoch(state))
          && data.getSource().getEpoch().equals(state.getCurrentJustifiedEpoch())
          && data.getSource().getRoot().equals(state.getCurrentJustifiedRoot());
      parent_crosslink = state.getCurrentCrosslinks().get(data.getCrosslink().getShard());
    } else {
      is_ffg_data_correct = data.getTarget().getEpoch().equals(get_previous_epoch(state))
          && data.getSource().getEpoch().equals(state.getPreviousJustifiedEpoch())
          && data.getSource().getRoot().equals(state.getPreviousJustifiedRoot());
      parent_crosslink = state.getPreviousCrosslinks().get(data.getCrosslink().getShard());
    }

    /*  assert ffg_data == (data.source_epoch, data.source_root, data.target_epoch)
        assert data.crosslink.start_epoch == parent_crosslink.end_epoch
        assert data.crosslink.end_epoch == min(data.target_epoch, parent_crosslink.end_epoch + MAX_EPOCHS_PER_CROSSLINK)
        assert data.crosslink.parent_root == hash_tree_root(parent_crosslink)
        assert data.crosslink.data_root == ZERO_HASH  # [to be removed in phase 1]
        validate_indexed_attestation(state, convert_to_indexed(state, attestation)) */
    if (!is_ffg_data_correct) {
      return false;
    }
    if (!data.getCrosslink().getStartEpoch().equals(parent_crosslink.getEndEpoch())) {
      return false;
    }
    if (!data.getCrosslink().getEndEpoch().equals(UInt64s.min(
        data.getTarget().getEpoch(),
        parent_crosslink.getEndEpoch().plus(getConstants().getMaxEpochsPerCrosslink())))) {
      return false;
    }
    if (!data.getCrosslink().getParentRoot().equals(hash_tree_root(parent_crosslink))) {
      return false;
    }
    if (!data.getCrosslink().getDataRoot().equals(Hash32.ZERO)) {
      return false;
    }
    return validate_indexed_attestation(state, convert_to_indexed(state, attestation));
  }

  /*
   """
   Process ``Attestation`` operation.
   """
  */
  default void process_attestation(MutableBeaconState state, Attestation attestation) {
    AttestationData data = attestation.getData();
    SlotNumber attestation_slot = get_attestation_data_slot(state, data);

    /*  pending_attestation = PendingAttestation(
        data=data,
        aggregation_bitfield=attestation.aggregation_bitfield,
        inclusion_delay=state.slot - attestation_slot,
        proposer_index=get_beacon_proposer_index(state)) */
    PendingAttestation pending_attestation = new PendingAttestation(
        attestation.getAggregationBits(),
        data,
        state.getSlot().minus(attestation_slot),
        get_beacon_proposer_index(state));
    if (data.getTarget().getEpoch().equals(get_current_epoch(state))) {
      state.getCurrentEpochAttestations().add(pending_attestation);
    } else {
      state.getPreviousEpochAttestations().add(pending_attestation);
    }
  }

  default void verify_deposit(BeaconState state, Deposit deposit) {
    /* Verify the Merkle branch
    assert verify_merkle_branch(
        leaf=hash_tree_root(deposit.data),
        proof=deposit.proof,
        depth=DEPOSIT_CONTRACT_TREE_DEPTH,
        index=deposit.index,
        root=state.latest_eth1_data.deposit_root,
        ) */
    assertTrue(verify_merkle_branch(
        hash_tree_root(deposit.getData()),
        deposit.getProof().listCopy(),
        getConstants().getDepositContractTreeDepth(),
        state.getDepositIndex(),
        state.getLatestEth1Data().getDepositRoot()
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
    state.setDepositIndex(state.getDepositIndex().increment());

    BLSPubkey pubkey = deposit.getData().getPubKey();
    Gwei amount = deposit.getData().getAmount();
    ValidatorIndex index = get_validator_index_by_pubkey(state, pubkey);

    /* if pubkey not in validator_pubkeys: */
    if (index.equals(ValidatorIndex.MAX)) {
      /* Verify the deposit signature (proof of possession).
         Invalid signatures are allowed by the deposit contract,
         and hence included on-chain, but must not be processed.
         Note: deposits are valid across forks, hence the deposit domain is retrieved directly from `bls_domain` */
      if (isBlsVerifyProofOfPossession() &&
          !bls_verify(
              pubkey,
              signing_root(deposit.getData()),
              deposit.getData().getSignature(),
              bls_domain(SignatureDomains.DEPOSIT))
      ) {
        return;
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
      state.getValidatorRegistry().add(new ValidatorRecord(
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

  default void verify_voluntary_exit(BeaconState state, VoluntaryExit exit) {
    checkIndexRange(state, exit.getValidatorIndex());
    ValidatorRecord validator = state.getValidatorRegistry().get(exit.getValidatorIndex());

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
    assert bls_verify(validator.pubkey, signing_root(exit), exit.signature, domain) */
    UInt64 domain = get_domain(state, SignatureDomains.VOLUNTARY_EXIT, exit.getEpoch());
    assertTrue(bls_verify(validator.getPubKey(), signing_root(exit), exit.getSignature(), domain));
  }

  /*
    """
    Process ``VoluntaryExit`` transaction.
    """
   */
  default void process_voluntary_exit(MutableBeaconState state, VoluntaryExit exit) {
    initiate_validator_exit(state, exit.getValidatorIndex());
  }

  default void verify_transfer(BeaconState state, Transfer transfer) {
    /* Verify the amount and fee are not individually too big (for anti-overflow purposes)
    assert state.balances[transfer.sender] >= max(transfer.amount, transfer.fee) */
    assertTrue(state.getBalances().get(transfer.getSender())
        .greater(UInt64s.max(transfer.getAmount(), transfer.getFee())));

    /* A transfer is valid in only one slot
    assert state.slot == transfer.slot */
    assertTrue(state.getSlot().equals(transfer.getSlot()));

    /* Sender must be not yet eligible for activation, withdrawn, or transfer balance over MAX_EFFECTIVE_BALANCE
    assert (
        state.validator_registry[transfer.sender].activation_eligibility_epoch == FAR_FUTURE_EPOCH or
        get_current_epoch(state) >= state.validator_registry[transfer.sender].withdrawable_epoch or
        transfer.amount + transfer.fee + MAX_EFFECTIVE_BALANCE <= state.balances[transfer.sender]
    ) */
    assertTrue(
        state.getValidatorRegistry().get(transfer.getSender()).getActivationEligibilityEpoch()
            .equals(getConstants().getFarFutureEpoch())
        || get_current_epoch(state)
            .greaterEqual(state.getValidatorRegistry().get(transfer.getSender()).getWithdrawableEpoch())
        || transfer.getAmount().plus(transfer.getFee()).plus(getConstants().getMaxEffectiveBalance())
            .lessEqual(state.getBalances().get(transfer.getSender()))
    );

    /* Verify that the pubkey is valid
    assert (
        state.validator_registry[transfer.sender].withdrawal_credentials ==
        BLS_WITHDRAWAL_PREFIX + hash(transfer.pubkey)[1:]
    ) */
    assertTrue(state.getValidatorRegistry().get(transfer.getSender()).getWithdrawalCredentials()
        .equals(int_to_bytes1(getConstants().getBlsWithdrawalPrefix()).concat(hash(transfer.getPubkey()).slice(1))));

    /* Verify that the signature is valid
    assert bls_verify(transfer.pubkey, signing_root(transfer), transfer.signature, get_domain(state, DOMAIN_TRANSFER)) */
    assertTrue(bls_verify(
        transfer.getPubkey(),
        signing_root(transfer),
        transfer.getSignature(),
        get_domain(state, SignatureDomains.TRANSFER))
    );
  }

  /*
    """
    Process ``Transfer`` transaction.
    """
   */
  default void process_transfer(MutableBeaconState state, Transfer transfer) {
    /* Process the transfer
    decrease_balance(state, transfer.sender, transfer.amount + transfer.fee)
    increase_balance(state, transfer.recipient, transfer.amount)
    increase_balance(state, get_beacon_proposer_index(state), transfer.fee) */
    decrease_balance(state, transfer.getSender(), transfer.getAmount().plus(transfer.getFee()));
    increase_balance(state, transfer.getRecipient(), transfer.getAmount());
    increase_balance(state, get_beacon_proposer_index(state), transfer.getFee());

    /* Verify balances are not dust
    assert not (0 < state.balances[transfer.sender] < MIN_DEPOSIT_AMOUNT)
    assert not (0 < state.balances[transfer.recipient] < MIN_DEPOSIT_AMOUNT) */
    assertTrue(!(state.getBalances().get(transfer.getSender()).greater(Gwei.ZERO)
        && state.getBalances().get(transfer.getSender()).less(getConstants().getMinDepositAmount())));
    assertTrue(!(state.getBalances().get(transfer.getRecipient()).greater(Gwei.ZERO)
        && state.getBalances().get(transfer.getRecipient()).less(getConstants().getMinDepositAmount())));
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
                state.getLatestEth1Data().getDepositCount().minus(state.getDepositIndex()).getIntValue())
    );
    // Verify that there are no duplicate transfers
    assertTrue(body.getTransfers().size() == body.getTransfers().stream().distinct().count());

    /* for operations, max_operations, function in (
        (body.proposer_slashings, MAX_PROPOSER_SLASHINGS, process_proposer_slashing),
        (body.attester_slashings, MAX_ATTESTER_SLASHINGS, process_attester_slashing),
        (body.attestations, MAX_ATTESTATIONS, process_attestation),
        (body.deposits, MAX_DEPOSITS, process_deposit),
        (body.voluntary_exits, MAX_VOLUNTARY_EXITS, process_voluntary_exit),
        (body.transfers, MAX_TRANSFERS, process_transfer),
      ):
          assert len(operations) <= max_operations
          for operation in operations:
              function(state, operation) */
    assertTrue(body.getProposerSlashings().size() <= getConstants().getMaxProposerSlashings());
    assertTrue(body.getAttesterSlashings().size() <= getConstants().getMaxAttesterSlashings());
    assertTrue(body.getAttestations().size() <= getConstants().getMaxAttestations());
    assertTrue(body.getDeposits().size() <= getConstants().getMaxDeposits());
    assertTrue(body.getVoluntaryExits().size() <= getConstants().getMaxVoluntaryExits());
    assertTrue(body.getTransfers().size() <= getConstants().getMaxTransfers());

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

    body.getTransfers().forEach(o -> {
      verify_transfer(state, o);
      process_transfer(state, o);
    });
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
