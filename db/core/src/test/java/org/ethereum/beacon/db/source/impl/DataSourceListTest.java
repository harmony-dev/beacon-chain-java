package org.ethereum.beacon.db.source.impl;

import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

public class DataSourceListTest {

    private final String TEST_VALUE = "test_value";

    @Test
    public void testValidSourceCreation() {
        assertThatThrownBy(() -> new DataSourceList<String>(new HashMapDataSource<>(), null, deserialize()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new DataSourceList<String>(new HashMapDataSource<>(), serialize(), null))
                .isInstanceOf(NullPointerException.class);
    }

    private Function<String, BytesValue> serialize() {
        return key -> BytesValue.EMPTY;
    }

    private Function<BytesValue, String> deserialize() {
        return key -> "";
    }

    @Test
    public void testPutGetSizeSerialization() {
        final DataSourceList<String> dataSource = new DataSourceList<>(new HashMapDataSource<>(), serialize(), deserialize());
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.size()).isEqualTo(0);
        assertThat(dataSource.get(0)).isNotPresent();

        dataSource.put(0, TEST_VALUE);
        assertThat(dataSource.size()).isEqualTo(1);
        assertThat(dataSource.get(0)).isPresent().hasValue("");

        dataSource.put(1, TEST_VALUE);
        assertThat(dataSource.size()).isEqualTo(2);
    }

    @Test
    public void testPutNull() {
        final DataSourceList<String> dataSource = new DataSourceList<>(new HashMapDataSource<>(), serialize(), deserialize());
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.size()).isEqualTo(0);
        assertThat(dataSource.get(0)).isNotPresent();

        dataSource.put(0, null);
        assertThat(dataSource.size()).isEqualTo(0);
        assertThat(dataSource.get(0)).isNotPresent();
    }

    @Test
    public void testPutOverSize() {
        final DataSourceList<String> dataSource = new DataSourceList<>(new HashMapDataSource<>(), serialize(), deserialize());
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.size()).isEqualTo(0);

        dataSource.put(1, TEST_VALUE);
        assertThat(dataSource.size()).isEqualTo(2);
    }

    @Test
    public void testGetOverIndex() {
        final DataSourceList<String> dataSource = new DataSourceList<>(new HashMapDataSource<>(), serialize(), deserialize());
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.size()).isEqualTo(0);

        dataSource.put(0, TEST_VALUE);
        assertThat(dataSource.get(-1)).isEmpty();
        assertThat(dataSource.get(1)).isEmpty();
    }
}
