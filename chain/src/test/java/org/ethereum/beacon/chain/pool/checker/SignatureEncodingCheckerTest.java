package org.ethereum.beacon.chain.pool.checker;

import org.ethereum.beacon.chain.pool.PoolTestConfigurator;
import org.ethereum.beacon.chain.pool.ReceivedAttestation;
import org.ethereum.beacon.core.operations.Attestation;
import org.ethereum.beacon.core.operations.attestation.AttestationData;
import org.ethereum.beacon.core.operations.attestation.Crosslink;
import org.ethereum.beacon.core.state.Checkpoint;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.EpochNumber;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.types.p2p.NodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureEncodingCheckerTest extends PoolTestConfigurator {

    private SignatureEncodingChecker checker;

    @BeforeEach
    void setUp() {
        checker = new SignatureEncodingChecker();
        assertThat(checker).isNotNull();
    }

    @Test
    void testValidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final Attestation message = createAttestation(BytesValue.of(1, 2, 3));
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);

        assertThat(checker.check(attestation)).isTrue();
    }

    @Test
    void testInvalidAttestation() {
        final NodeId sender = new NodeId(new byte[100]);
        final AttestationData attestationData = new AttestationData(
                Hashes.sha256(BytesValue.fromHexString("aa")),
                new Checkpoint(EpochNumber.of(231), Hashes.sha256(BytesValue.fromHexString("bb"))),
                new Checkpoint(EpochNumber.of(2), Hashes.sha256(BytesValue.fromHexString("cc"))),
                Crosslink.EMPTY);
        final BytesValue value = BytesValue.of(1, 2, 3);
        final Attestation message = new Attestation(
                Bitlist.of(value.size() * 8, value, specConstants.getMaxValidatorsPerCommittee().getValue()),
                attestationData,
                Bitlist.of(8, BytesValue.fromHexString("bb"), specConstants.getMaxValidatorsPerCommittee().getValue()),
                BLSSignature.wrap(Bytes96.fromHexString("cc")),
                specConstants);
        final ReceivedAttestation attestation = new ReceivedAttestation(sender, message);

        assertThat(checker.check(attestation)).isFalse();
    }
}
