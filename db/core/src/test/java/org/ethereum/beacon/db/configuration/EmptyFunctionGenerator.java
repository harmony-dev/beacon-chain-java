package org.ethereum.beacon.db.configuration;

import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import java.util.function.Function;

public class EmptyFunctionGenerator extends Generator<Function> {
    public EmptyFunctionGenerator() {
        super(Function.class);
    }

    @Override
    public Function generate(SourceOfRandomness random, GenerationStatus status) {
        return Function.identity();
    }
}
