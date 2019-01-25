package org.ethereum.beacon.core.spec;

/**
 * Honest validator parameters.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/59b301f7af7b91925083bcf68d460a44bbc8254e/specs/validator/0_beacon-chain-validator.md#misc">Misc</a>
 *     in Honest Validator.
 */
public interface HonestValidatorParameters {

  long ETH1_FOLLOW_DISTANCE = 1L << 10; // 1024 blocks, ~4 hours

  long getEth1FollowDistance();
}
