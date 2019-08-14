package org.ethereum.beacon.db.source.impl;

import org.junit.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class DelegateDataSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";

    private DelegateDataSource<String, String> delegateDataSource;

    @Before
    public void setUp() {
        delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<>());
        assertThat(delegateDataSource).isNotNull();
    }

    @Test
    public void testPutGetRemove() {
        delegateDataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(delegateDataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);

        delegateDataSource.remove(TEST_KEY);
        assertThat(delegateDataSource.get(TEST_KEY)).isNotPresent();
    }

    @Test
    public void testNullKey() {
        assertThatThrownBy(() -> delegateDataSource.put(null, TEST_VALUE)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testNullValue() {
        assertThatThrownBy(() -> delegateDataSource.put(TEST_KEY, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testFlush() {
        delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<String, String>() {
            public void flush() {
                store.clear();
            }
        });
        assertThat(delegateDataSource).isNotNull();

        delegateDataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(delegateDataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);

        delegateDataSource.flush();
        assertThat(delegateDataSource.get(TEST_KEY)).isNotPresent();
    }
}
