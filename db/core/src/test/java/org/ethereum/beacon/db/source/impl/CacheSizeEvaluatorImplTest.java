package org.ethereum.beacon.db.source.impl;

import org.ethereum.beacon.db.source.CacheSizeEvaluator;
import org.junit.jupiter.api.*;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

class CacheSizeEvaluatorImplTest {

    private static final long EMPTY_SIZE = 0L;
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private CacheSizeEvaluator<String, String> cacheSizeEvaluator;
    private long createdStorageSize;

    @BeforeEach
    void setUp() {
        cacheSizeEvaluator = new CacheSizeEvaluatorImpl<>(key -> (long) key.length(), key -> (long) key.length());
        assertThat(cacheSizeEvaluator).isNotNull();
        createdStorageSize = cacheSizeEvaluator.getEvaluatedSize();
        assertThat(createdStorageSize).isEqualTo(EMPTY_SIZE);
    }

    @Test
    @Tag("FIX")
    void testInvalidInstanceCreation() {
        assertThatThrownBy(() -> new CacheSizeEvaluatorImpl<>(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CacheSizeEvaluatorImpl<>(null, Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CacheSizeEvaluatorImpl<>(Function.identity(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void added() {
        cacheSizeEvaluator.added(KEY, VALUE);
        final long newSize = createdStorageSize + KEY.length() + VALUE.length();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(newSize);
    }

    @Test
    void removed() {
        cacheSizeEvaluator.added(KEY, VALUE);
        cacheSizeEvaluator.removed(KEY, VALUE);
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(createdStorageSize);
    }

    @Test
    void reset() {
        cacheSizeEvaluator.added(KEY, VALUE);
        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(EMPTY_SIZE);
    }
}
