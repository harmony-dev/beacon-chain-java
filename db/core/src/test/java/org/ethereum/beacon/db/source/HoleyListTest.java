package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.*;

import java.util.*;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class HoleyListTest {

    private final Long TEST_KEY_0 = 0L;
    private final Long TEST_KEY_1 = 1L;
    private final Long TEST_KEY_LESS_ZERO = -1L;
    private final String TEST_VALUE = "test_value";
    private final String TEST_DEFAULT_VALUE = "test_default_value";

    private HoleyList<String> list;

    @BeforeEach
    public void setUp() {
        list = new HoleyList<String>() {
            private final Map<Long, String> store = new HashMap<>();

            public long size() {
                return store.size();
            }

            public void put(long idx, String value) {
                store.put(idx, value);
            }

            public Optional<String> get(long idx) {
                return Optional.of(store.get(idx));
            }
        };

        assertThat(list).isNotNull();
    }

    @Test
    public void testAdd() {
        list.put(TEST_KEY_0, TEST_VALUE);
        final long indexToAdd = list.size();
        list.add(TEST_VALUE);

        assertThat(list.size()).isEqualTo(2);
        assertThat(list.get(indexToAdd)).isPresent().hasValue(TEST_VALUE);
    }

    @Test
    public void testAddNull() {
        list.put(TEST_KEY_0, TEST_VALUE);
        list.add(null);

        assertThat(list.size()).isEqualTo(1);
    }

    @Test
    public void testUpdateExistingValue() {
        list.put(TEST_KEY_0, TEST_VALUE);
        assertThat(list.update(TEST_KEY_0, key -> "")).isPresent().hasValue("");
    }

    @Test
    public void testUpdateExistingValueOverIndex() {
        list.put(TEST_KEY_0, TEST_VALUE);
        assertThat(list.update(TEST_KEY_1, key -> "")).isNotPresent();
        assertThat(list.update(TEST_KEY_LESS_ZERO, key -> "")).isNotPresent();
    }

    @Test
    public void testUpdateExistingValueOrPutDefault() {
        list.put(TEST_KEY_0, TEST_VALUE);
        assertThat(list.update(TEST_KEY_0, key -> "", () -> TEST_DEFAULT_VALUE)).isEqualTo("");
        assertThat(list.update(TEST_KEY_1, key -> "", () -> TEST_DEFAULT_VALUE)).isEqualTo(TEST_DEFAULT_VALUE);
    }

}
