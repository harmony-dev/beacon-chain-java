package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WriteCacheImplTest {

    private final String TEST_KEY = "test_key";

    @Test
    public void get() {
        final WriteCacheImpl<String, String> dataSource = new WriteCacheImpl<>(new HashMapDataSource<>());
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.get(TEST_KEY)).isEmpty();
    }

    @Test
    public void getCacheEntry() {
        final WriteCacheImpl<String, String> dataSource = new WriteCacheImpl<>(new HashMapDataSource<>());
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getCacheEntry(TEST_KEY)).isEmpty();
    }
}
