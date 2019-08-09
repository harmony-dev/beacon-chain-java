package org.ethereum.beacon.db.configuration;

import com.pholser.junit.quickcheck.generator.*;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;

public class HashMapDatasourceGenerator extends Generator<DataSource> {
    public HashMapDatasourceGenerator() {
        super(DataSource.class);
    }

    @Override
    public HashMapDataSource generate(SourceOfRandomness random, GenerationStatus status) {
        return new HashMapDataSource();
    }
}
