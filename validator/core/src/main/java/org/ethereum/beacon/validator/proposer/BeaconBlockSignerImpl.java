package org.ethereum.beacon.validator.proposer;

import static org.ethereum.beacon.core.spec.SignatureDomains.BEACON_PROPOSER;

import org.ethereum.beacon.consensus.BeaconChainSpec;
import org.ethereum.beacon.core.BeaconBlock;
import org.ethereum.beacon.core.state.Fork;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.validator.BeaconBlockSigner;
import org.ethereum.beacon.validator.crypto.MessageSigner;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.uint.UInt64;

/**
 * Default implementation of block signer.
 *
 * @see BeaconBlockSigner
 */
public class BeaconBlockSignerImpl implements BeaconBlockSigner {

  private final BeaconChainSpec spec;

  public BeaconBlockSignerImpl(BeaconChainSpec spec) {
    this.spec = spec;
  }

  @Override
  public BeaconBlock sign(BeaconBlock block, Fork fork, MessageSigner<BLSSignature> signer) {
    Hash32 proposalRoot = spec.signing_root(block);
    Bytes4 forkVersion = spec.fork_version(spec.slot_to_epoch(block.getSlot()), fork);
    UInt64 domain = spec.bls_domain(BEACON_PROPOSER, forkVersion);
    BLSSignature signature = signer.sign(proposalRoot, domain);

    return BeaconBlock.Builder.fromBlock(block).withSignature(signature).build();
  }
}
