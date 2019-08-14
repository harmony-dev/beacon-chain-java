package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.HoleyList;
import org.junit.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HashMapHoleyListTest {

    private final Long TEST_KEY = 0L;
    private final Long TEST_KEY_1 = 1L;
    private final Long TEST_KEY_LESS_ZERO = -1L;
    private final String TEST_VALUE = "test_value";
    private final String TEST_VALUE_NEW = "NewTestValue";

    private HoleyList<String> map;

    @Before
    public void setUp() {
        map = new HashMapHoleyList<>();
        assertThat(map).isNotNull();
        assertThat(map.size()).isEqualTo(0L);
    }

    @Test
    public void testSize() {
        map.put(TEST_KEY, TEST_VALUE);
        assertThat(map.size()).isEqualTo(1L);
    }

    @Test
    public void testPutNullValue() {
        map.put(TEST_KEY, null);
        assertThat(map.size()).isEqualTo(0L);
        assertThat(map.get(TEST_KEY)).isNotPresent();
    }

    @Test
    public void testPutGet() {
        map.put(TEST_KEY, TEST_VALUE);
        assertThat(map.size()).isEqualTo(1L);
        assertThat(map.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
    }

    @Test
    public void testPutSameKey() {
        map.put(TEST_KEY, TEST_VALUE);
        map.put(TEST_KEY_1, TEST_VALUE);
        assertThat(map.size()).isEqualTo(2L);

        map.put(TEST_KEY, TEST_VALUE_NEW);
        assertThat(map.size()).isEqualTo(2L);
        assertThat(map.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE_NEW);
    }

    @Test
    public void testGetOverIndex() {
        map.put(TEST_KEY, TEST_VALUE);
        assertThat(map.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
        assertThat(map.get(TEST_KEY_1)).isNotPresent();
        assertThat(map.get(TEST_KEY_LESS_ZERO)).isNotPresent();
    }
}
