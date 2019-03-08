package org.ethereum.beacon.consensus.verifier.operation;

import static org.ethereum.beacon.consensus.verifier.VerificationResult.PASSED;
import static org.ethereum.beacon.consensus.verifier.VerificationResult.failedResult;

import org.ethereum.beacon.consensus.SpecHelpers;
import org.ethereum.beacon.consensus.verifier.OperationVerifier;
import org.ethereum.beacon.consensus.verifier.VerificationResult;
import org.ethereum.beacon.core.BeaconState;
import org.ethereum.beacon.core.operations.Transfer;
import org.ethereum.beacon.core.spec.SignatureDomains;
import org.ethereum.beacon.core.state.ValidatorRecord;
import org.ethereum.beacon.core.types.Gwei;

/**
 * Verifies {@link Transfer} beacon chain operation.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/0.4.0/specs/core/0_beacon-chain.md#transfers-1">Transfers</a>
 *     section in the spec.
 */
public class TransferVerifier implements OperationVerifier<Transfer> {

  private final SpecHelpers spec;

  public TransferVerifier(SpecHelpers spec) {
    this.spec = spec;
  }

  @Override
  public VerificationResult verify(Transfer transfer, BeaconState state) {

    if (transfer.getFrom().greaterEqual(state.getValidatorRegistry().size())) {
      return failedResult(
          "sender index does not exist, registry size: %s", state.getValidatorRegistry().size());
    }

    if (transfer.getTo().greaterEqual(state.getValidatorRegistry().size())) {
      return failedResult(
          "recipient index does not exist, registry size: %s", state.getValidatorRegistry().size());
    }

    Gwei fromBalance = state.getValidatorBalances().get(transfer.getFrom());

    // Verify that state.validator_balances[transfer.from] >= transfer.amount.
    if (fromBalance.less(transfer.getAmount())) {
      return failedResult(
          "insufficient funds to cover amount, balance=%s when transfer.amount=%s",
          fromBalance, transfer.getAmount());
    }

    // Verify that state.validator_balances[transfer.from] >= transfer.fee.
    if (fromBalance.less(transfer.getFee())) {
      return failedResult(
          "insufficient funds to cover the fee, balance=%s when transfer.fee=%s",
          fromBalance, transfer.getFee());
    }

    // Verify that state.validator_balances[transfer.from] == transfer.amount + transfer.fee or
    // state.validator_balances[transfer.from] >= transfer.amount + transfer.fee +
    // MIN_DEPOSIT_AMOUNT.
    if (!fromBalance.less(transfer.getFee().plus(transfer.getAmount()))
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

    ValidatorRecord sender = state.getValidatorRegistry().get(transfer.getFrom());

    // Verify that get_current_epoch(state) >=
    // state.validator_registry[transfer.from].withdrawable_epoch or
    // state.validator_registry[transfer.from].activation_epoch == FAR_FUTURE_EPOCH.
    if (spec.get_current_epoch(state).less(sender.getWithdrawableEpoch())
        && !sender.getActivationEpoch().equals(spec.getConstants().getFarFutureEpoch())) {
      return failedResult("epoch validation failed");
    }

    // Verify that state.validator_registry[transfer.from].withdrawal_credentials ==
    // BLS_WITHDRAWAL_PREFIX_BYTE + hash(transfer.pubkey)[1:]
    if (!sender
        .getWithdrawalCredentials()
        .equals(
            spec.getConstants()
                .getBlsWithdrawalPrefixByte()
                .concat(spec.hash(transfer.getPubkey()).slice(1)))) {
      return failedResult("withdrawal_credentials do not match");
    }

    // Verify that bls_verify(pubkey=transfer.pubkey, message_hash=signed_root(transfer,
    // "signature"),
    // signature=transfer.signature, domain=get_domain(state.fork, slot_to_epoch(transfer.slot),
    // DOMAIN_TRANSFER)).
    if (!spec.bls_verify(
        transfer.getPubkey(),
        spec.signed_root(transfer, "signature"),
        transfer.getSignature(),
        spec.get_domain(
            state.getForkData(),
            spec.slot_to_epoch(transfer.getSlot()),
            SignatureDomains.TRANSFER))) {
      return failedResult("signature verification failed");
    }

    return PASSED;
  }
}
