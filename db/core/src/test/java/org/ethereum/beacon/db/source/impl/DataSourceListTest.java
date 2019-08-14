package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.DataSource;
import org.junit.*;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

public class DataSourceListTest {

    private final BytesValue SIZE_KEY = BytesValue.fromHexString("FFFFFFFFFFFFFFFF");
    private final Long TEST_KEY_0 = 0L;
    private final Long TEST_KEY_1 = 1L;
    private final Long TEST_KEY_LESS_ZERO = -1L;
    private final String TEST_VALUE = "test_value";

    private DataSourceList<String> dataSourceList;

    @Before
    public void setUp() {
        dataSourceList = new DataSourceList<>(new HashMapDataSource<>(), serialize(), deserialize());
        assertThat(dataSourceList).isNotNull();
        assertThat(dataSourceList.size()).isEqualTo(0);
        assertThat(dataSourceList.get(TEST_KEY_0)).isNotPresent();
    }

    @Test
    public void testValidSourceCreation() {
        assertThatThrownBy(() -> new DataSourceList<>(new HashMapDataSource<>(), null, deserialize()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new DataSourceList<>(new HashMapDataSource<>(), serialize(), null))
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
        dataSourceList.put(TEST_KEY_0, TEST_VALUE);
        assertThat(dataSourceList.size()).isEqualTo(1);
        assertThat(dataSourceList.get(TEST_KEY_0)).isPresent().hasValue("");

        dataSourceList.put(TEST_KEY_1, TEST_VALUE);
        assertThat(dataSourceList.size()).isEqualTo(2);
    }

    @Test
    public void testPutNull() {
        dataSourceList.put(TEST_KEY_0, null);
        assertThat(dataSourceList.size()).isEqualTo(0);
        assertThat(dataSourceList.get(TEST_KEY_0)).isNotPresent();
    }

    @Test
    public void testPutOverSize() {
        dataSourceList.put(TEST_KEY_1, TEST_VALUE);
        assertThat(dataSourceList.size()).isEqualTo(2);
    }

    @Test
    public void testGetOverIndex() {
        dataSourceList.put(TEST_KEY_0, TEST_VALUE);
        assertThat(dataSourceList.get(TEST_KEY_LESS_ZERO)).isEmpty();
        assertThat(dataSourceList.get(TEST_KEY_1)).isEmpty();
    }

    @Test
    public void testSizeBySIZE_KEY() {
        final DataSource<BytesValue, BytesValue> dataSource = new HashMapDataSource<>();
        dataSource.put(SIZE_KEY, BytesValue.wrap("-1000".getBytes()));
        dataSourceList = new DataSourceList<>(dataSource, serialize(), deserialize());
        assertThat(dataSourceList.size()).isEqualTo(0L);
    }
}
