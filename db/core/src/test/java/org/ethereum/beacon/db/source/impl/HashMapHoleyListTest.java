package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.HoleyList;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HashMapHoleyListTest {

    private final Long TEST_KEY = 0L;
    private final String TEST_VALUE = "test_value";

    @Test
    public void size() {
        final HoleyList<String> map = new HashMapHoleyList<>();
        assertThat(map).isNotNull();
        assertThat(map.size()).isEqualTo(0L);

        map.put(TEST_KEY, TEST_VALUE);
        assertThat(map.size()).isEqualTo(1L);
    }

    @Test
    public void putNullValue() {
        final HoleyList<String> map = new HashMapHoleyList<>();
        map.put(TEST_KEY, null);
        assertThat(map.size()).isEqualTo(0L);
    }

    @Test
    public void put() {
        final HoleyList<String> map = new HashMapHoleyList<>();
        map.put(TEST_KEY, TEST_VALUE);
        assertThat(map.size()).isEqualTo(1L);
    }

    @Test
    public void putSameKey() {
        final HoleyList<String> map = new HashMapHoleyList<>();
        map.put(TEST_KEY, TEST_VALUE);
        map.put(1, TEST_VALUE);
        assertThat(map.size()).isEqualTo(2L);

        map.put(TEST_KEY, "NewTestValue");
        assertThat(map.size()).isEqualTo(2L);
    }

    @Test
    public void get() {
        final HoleyList<String> map = new HashMapHoleyList<>();
        map.put(TEST_KEY, TEST_VALUE);
        assertThat(map.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
        assertThat(map.get(1)).isNotPresent();
    }
}
