package net.consensys.cava.ssz.fixtures;

import net.consensys.cava.ssz.annotation.SSZ;
import net.consensys.cava.ssz.annotation.SSZSerializable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.sun.org.apache.bcel.internal.classfile.Utility.toHexString;

/**
 * Slot attestation data
 */
@SSZSerializable
public class AttestationRecord {

    // Slot number
    private final long slot;
    // Shard ID
    @SSZ(type=SSZ.UInt24)
    private final int shardId;
    // List of block hashes that this signature is signing over that
    // are NOT part of the current chain, in order of oldest to newest
    @SSZ(type=SSZ.Hash32)
    private final List<byte[]> obliqueParentHashes;
    // Block hash in the shard that we are attesting to
    @SSZ(type=SSZ.Hash32)
    private final byte[] shardBlockHash;
    // Who is participating
    @SSZ(type=SSZ.Bytes, skipContainer = true)
    private final Bitfield attesterBitfield;
    // Last justified block
    private final long justifiedSlot;
    @SSZ(type=SSZ.Hash32)
    private final byte[] justifiedBlockHash;
    // The actual signature
    private final Sign.Signature aggregateSig;

    public AttestationRecord(long slot, int shardId, List<byte[]> obliqueParentHashes, byte[] shardBlockHash,
                             Bitfield attesterBitfield, long justifiedSlot, byte[] justifiedBlockHash,
                             Sign.Signature aggregateSig) {
        this.slot = slot;
        this.shardId = shardId;
        this.obliqueParentHashes = obliqueParentHashes;
        this.shardBlockHash = shardBlockHash;
        this.attesterBitfield = attesterBitfield;
        this.justifiedSlot = justifiedSlot;
        this.justifiedBlockHash = justifiedBlockHash;
        this.aggregateSig = aggregateSig;
    }

    public long getSlot() {
        return slot;
    }

    public int getShardId() {
        return shardId;
    }

    public List<byte[]> getObliqueParentHashes() {
        return obliqueParentHashes;
    }

    public byte[] getShardBlockHash() {
        return shardBlockHash;
    }

    public Bitfield getAttesterBitfield() {
        return attesterBitfield;
    }

    public long getJustifiedSlot() {
        return justifiedSlot;
    }

    public byte[] getJustifiedBlockHash() {
        return justifiedBlockHash;
    }

    public Sign.Signature getAggregateSig() {
        return aggregateSig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttestationRecord that = (AttestationRecord) o;
        return slot == that.slot &&
            shardId == that.shardId &&
            justifiedSlot == that.justifiedSlot &&
            Objects.equals(obliqueParentHashes, that.obliqueParentHashes) &&
            Arrays.equals(shardBlockHash, that.shardBlockHash) &&
            Objects.equals(attesterBitfield, that.attesterBitfield) &&
            Arrays.equals(justifiedBlockHash, that.justifiedBlockHash) &&
            Objects.equals(aggregateSig, that.aggregateSig);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append("AttestationRecord{")
                .append("slot=").append(slot)
                .append(", shardId=").append(shardId)
                .append(", obliqueParentHashes=[").append(obliqueParentHashes.size()).append(" item(s)]")
                .append(", shardBlockHash=").append(toHexString(shardBlockHash))
                .append(", attesterBitfield=").append(attesterBitfield)
                .append(", justifiedSlot=").append(justifiedSlot)
                .append(", justifiedBlockHash=").append(toHexString(justifiedBlockHash))
                .append(", aggregateSig=[").append(aggregateSig).append("}");

        return builder.toString();
    }
}
