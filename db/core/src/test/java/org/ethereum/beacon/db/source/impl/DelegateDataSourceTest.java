package org.ethereum.beacon.db.source.impl;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DelegateDataSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";

    @Test
    public void testPutGetRemove() {
        final DelegateDataSource<String, String> delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<>());
        assertThat(delegateDataSource).isNotNull();

        delegateDataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(delegateDataSource.get(TEST_KEY)).isPresent();

        delegateDataSource.remove(TEST_KEY);
        assertThat(delegateDataSource.get(TEST_KEY)).isNotPresent();
    }

    @Test(expected = NullPointerException.class)
    public void testNullKey() {
        final DelegateDataSource<String, String> delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<>());
        assertThat(delegateDataSource).isNotNull();

        delegateDataSource.put(null, TEST_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testNullValue() {
        final DelegateDataSource<String, String> delegateDataSource = new DelegateDataSource<>(new HashMapDataSource<>());
        assertThat(delegateDataSource).isNotNull();

        delegateDataSource.put(TEST_KEY, null);
    }
}
