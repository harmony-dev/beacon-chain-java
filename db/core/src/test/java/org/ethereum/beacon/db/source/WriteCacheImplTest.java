package org.ethereum.beacon.db.source;

import com.pholser.junit.quickcheck.*;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.ethereum.beacon.db.configuration.HashMapDatasourceGenerator;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitQuickcheck.class)
public class WriteCacheImplTest {

    private final String TEST_ENTITY_KEY = "TEST_ENTITY_KEY";

    @Property
    public void get(@From(HashMapDatasourceGenerator.class) DataSource dataSource) {
        final CacheDataSource<String, String> cacheDataSource = new WriteCacheImpl<String, String>(dataSource);
        assertThat(cacheDataSource.get(TEST_ENTITY_KEY)).isNotPresent();
    }

    @Property
    public void getCacheEntry(@From(HashMapDatasourceGenerator.class) DataSource dataSource) {
        final CacheDataSource<String, String> cacheDataSource = new WriteCacheImpl<String, String>(dataSource);
        assertThat(cacheDataSource.get(TEST_ENTITY_KEY)).isNotPresent();
    }
}
