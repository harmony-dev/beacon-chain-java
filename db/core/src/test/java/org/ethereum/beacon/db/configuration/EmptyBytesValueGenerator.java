package org.ethereum.beacon.db.configuration;

import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class EmptyBytesValueGenerator extends Generator<BytesValue> {
    public EmptyBytesValueGenerator() {
        super(BytesValue.class);
    }

    @Override
    public BytesValue generate(SourceOfRandomness random, GenerationStatus status) {
        return BytesValue.EMPTY;
    }
}
