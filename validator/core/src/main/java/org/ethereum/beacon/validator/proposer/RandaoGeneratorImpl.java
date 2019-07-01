package org.ethereum.beacon.validator.proposer;

import static org.ethereum.beacon.core.spec.SignatureDomains.RANDAO;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.validator.RandaoGenerator;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Default implementation of {@link RandaoGenerator}.
 *
 * @see org.ethereum.beacon.validator.BeaconChainProposer
 */
public class RandaoGeneratorImpl implements RandaoGenerator {

  private final BeaconChainSpec spec;
  private final MessageSigner<BLSSignature> signer;

  public RandaoGeneratorImpl(BeaconChainSpec spec, MessageSigner<BLSSignature> signer) {
    this.spec = spec;
    this.signer = signer;
  }

  @Override
  public BLSSignature reveal(EpochNumber epoch, Fork fork) {
    Bytes4 forkVersion = spec.fork_version(epoch, fork);
    Hash32 hash = spec.hash_tree_root(epoch);
    UInt64 domain = spec.bls_domain(RANDAO, forkVersion);
    return signer.sign(hash, domain);
  }
}
