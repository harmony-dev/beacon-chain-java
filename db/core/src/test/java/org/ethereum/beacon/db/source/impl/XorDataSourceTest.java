package org.ethereum.beacon.db.source.impl;


import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XorDataSourceTest {

    @Test
    void testDataSourceCreation() {
        assertThatThrownBy(() -> new XorDataSource<>(null, BytesValue.EMPTY))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new XorDataSource<>(null, null))
                .isInstanceOf(NullPointerException.class);

        final HashMapDataSource<BytesValue, Object> upstream = new HashMapDataSource<>();
        XorDataSource<Object> actual = new XorDataSource<>(upstream, null);
        assertThat(actual.getUpstream()).isEqualTo(upstream);

        actual = new XorDataSource<>(upstream, BytesValue.of(1, 2, 3));
        assertThat(actual.getUpstream()).isEqualTo(upstream);
    }
}
