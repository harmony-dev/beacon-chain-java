package org.ethereum.beacon.db.source.impl;


import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.assertj.core.api.Assertions.*;

class XorDataSourceTest {

    @Test
    void testDataSourceCreation() {
        assertThatThrownBy(() -> new XorDataSource<>(null, BytesValue.EMPTY))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new XorDataSource<>(null, null))
                .isInstanceOf(NullPointerException.class);

        assertThat(new XorDataSource<>(new HashMapDataSource<>(), null)).isNotNull();
        assertThat(new XorDataSource<>(new HashMapDataSource<>(), BytesValue.of(1,2,3))).isNotNull();
    }
}
