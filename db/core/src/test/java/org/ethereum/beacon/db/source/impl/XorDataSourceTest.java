package org.ethereum.beacon.db.source.impl;

import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XorDataSourceTest {

    @Test
    public void testValidSourceCreation() {
        assertThatThrownBy(() -> new XorDataSource<>(null, BytesValue.EMPTY))
                .isInstanceOf(NullPointerException.class);
    }

}
