package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.Test;

import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class LinkedDataSourceTest {

    @Test
    public void setUpstream() {
        final LinkedDataSource<String, String, String, String> dataSource = new WriteCacheImpl<>(new HashMapDataSource<>());
        assertThatThrownBy(() -> dataSource.setUpstream(new HashMapDataSource<>())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testValidSourceCreation() {
        assertThatThrownBy(() -> new WriteCacheImpl<String, String>(null)).isInstanceOf(NullPointerException.class);
    }

}
