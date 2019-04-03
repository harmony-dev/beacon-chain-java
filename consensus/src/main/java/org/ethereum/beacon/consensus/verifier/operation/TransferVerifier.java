package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Gwei;
import tech.pegasys.artemis.util.uint.UInt64s;

/**
 * Verifies {@link Transfer} beacon chain operation.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/0.4.0/specs/core/0_beacon-chain.md#transfers-1">Transfers</a>
 *     section in the spec.
 */
public class TransferVerifier implements OperationVerifier<Transfer> {

  private final BeaconChainSpec spec;

  public TransferVerifier(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(Transfer transfer, BeaconState state) {

    if (transfer.getSender().greaterEqual(state.getValidatorRegistry().size())) {
      return failedResult(
          "sender index does not exist, registry size: %s", state.getValidatorRegistry().size());
    }

    if (transfer.getRecipient().greaterEqual(state.getValidatorRegistry().size())) {
      return failedResult(
          "recipient index does not exist, registry size: %s", state.getValidatorRegistry().size());
    }

    Gwei fromBalance = state.getValidatorBalances().get(transfer.getSender());

    // Verify the amount and fee aren't individually too big (for anti-overflow purposes)
    if (fromBalance.less(UInt64s.max(transfer.getAmount(), transfer.getFee()))) {
      return failedResult(
          "insufficient funds to cover amount or fee, balance=%s when transfer.amount=%s, transfer.fee=%s",
          fromBalance, transfer.getAmount(), transfer.getFee());
    }

    /* assert (
        state.validator_balances[transfer.sender] == transfer.amount + transfer.fee or
        state.validator_balances[transfer.sender] >= transfer.amount + transfer.fee + MIN_DEPOSIT_AMOUNT
       ) */
    if (!fromBalance.equals(transfer.getFee().plus(transfer.getAmount()))
        && fromBalance.less(
            transfer
                .getAmount()
                .plus(transfer.getFee())
                .plus(spec.getConstants().getMinDepositAmount()))) {
      return failedResult(
          "insufficient funds to cover transfer, balance=%s when transfer total=%s",
          fromBalance, transfer.getFee().plus(transfer.getAmount()));
    }

    // Verify that state.slot == transfer.slot.
    if (!state.getSlot().equals(transfer.getSlot())) {
      return failedResult("transfer slot is invalid, state.slot=%s when transfer.slot=%s");
    }

    ValidatorRecord sender = state.getValidatorRegistry().get(transfer.getSender());

    /* assert (
        get_current_epoch(state) >= state.validator_registry[transfer.sender].withdrawable_epoch or
        state.validator_registry[transfer.sender].activation_epoch == FAR_FUTURE_EPOCH
       )*/
    if (spec.get_current_epoch(state).less(sender.getWithdrawableEpoch())
        && !sender.getActivationEpoch().equals(spec.getConstants().getFarFutureEpoch())) {
      return failedResult("epoch validation failed");
    }

    /* assert (
        state.validator_registry[transfer.sender].withdrawal_credentials ==
        BLS_WITHDRAWAL_PREFIX_BYTE + hash(transfer.pubkey)[1:]
       ) */
    if (!sender
        .getWithdrawalCredentials()
        .equals(
            spec.getConstants()
                .getBlsWithdrawalPrefixByte()
                .concat(spec.hash(transfer.getPubkey()).slice(1)))) {
      return failedResult("withdrawal_credentials do not match");
    }

    /* assert bls_verify(
        pubkey=transfer.pubkey,
        message_hash=signed_root(transfer),
        signature=transfer.signature,
        domain=get_domain(state.fork, slot_to_epoch(transfer.slot), DOMAIN_TRANSFER)
       ) */
    if (!spec.bls_verify(
        transfer.getPubkey(),
        spec.signed_root(transfer),
        transfer.getSignature(),
        spec.get_domain(
            state.getFork(),
            spec.slot_to_epoch(transfer.getSlot()),
            SignatureDomains.TRANSFER))) {
      return failedResult("signature verification failed");
    }

    return PASSED;
  }
}
