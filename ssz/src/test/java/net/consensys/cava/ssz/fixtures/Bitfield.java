package net.consensys.cava.ssz.fixtures;

import net.consensys.cava.ssz.annotation.SSZSerializable;

import java.util.*;

/**
 * Bitfield is bit array where every bit represents status of
 * attester with corresponding index
 *
 * All methods that could change payload are cloning source,
 * keeping instances of Bitfield immutable.
 */
@SSZSerializable(encode="getData")
public class Bitfield {

    private final BitSet payload;
    private final int size; // in Bits

    private Bitfield(int size) {
        this.size = calcLength(size) * Byte.SIZE;
        this.payload = new BitSet(size);
    }

    public Bitfield(byte[] data) {
        this.size = data.length * Byte.SIZE;
        this.payload = BitSet.valueOf(data);
    }

    /**
     * Calculates attesters bitfield length
     * @param num  Number of attesters
     * @return  Bitfield length in bytes
     */
    public static int calcLength(int num) {
        return num == 0 ? 0 : (num - 1) / Byte.SIZE + 1;
    }

    /**
     * Creates empty bitfield for estimated number of attesters
     * @param validatorsCount   Number of attesters
     * @return  empty bitfield with correct length
     */
    public static Bitfield createEmpty(int validatorsCount) {
        return new Bitfield(validatorsCount);
    }

    /**
     * Modifies bitfield to represent attester's vote
     * Should place its bit on the right place
     * Doesn't modify original bitfield
     * @param index     Index number of attester
     * @return  bitfield with vote in place
     */
    public Bitfield markVote(int index) {
        Bitfield newBitfield = this.clone();
        newBitfield.payload.set(index);
        return newBitfield;
    }

    /**
     * Checks whether validator with provided index did his vote
     * @param index     Index number of attester
     */
    public boolean hasVoted(int index) {
        return payload.get(index);
    }

    /**
     * Calculate number of votes in provided bitfield
     * @return  number of votes
     */
    public int calcVotes() {
        int votes = 0;
        for (int i = 0; i < size(); ++i) {
            if (hasVoted(i)) ++votes;
        }

        return votes;
    }

    public static Bitfield orBitfield(Bitfield... bitfields) {
        return orBitfield(new ArrayList<>(Arrays.asList(bitfields)));
    }

    /**
     * OR aggregation function
     * OR aggregation of input bitfields
     * @param bitfields  Bitfields
     * @return All bitfields aggregated using OR
     */
    public static Bitfield orBitfield(List<Bitfield> bitfields) {
        if (bitfields.isEmpty()) return null;

        int bitfieldLen = bitfields.get(0).size();
        Bitfield aggBitfield = new Bitfield(bitfieldLen);
        for (int i = 0; i < bitfieldLen; ++i) {
            for (Bitfield bitfield : bitfields) {
                if (aggBitfield.payload.get(i) | bitfield.payload.get(i)) {
                    aggBitfield.payload.set(i);
                }
            }
        }

        return aggBitfield;
    }

    public int size() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bitfield bitfield = (Bitfield) o;
        return size == bitfield.size &&
                Objects.equals(payload, bitfield.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload, size);
    }

    public byte[] getData() {
        return Arrays.copyOf(payload.toByteArray(), (size + 7) / Byte.SIZE);
    }

    public Bitfield clone() {
        return new Bitfield(getData());
    }

}
