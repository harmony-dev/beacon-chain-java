package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class CacheSizeEvaluatorTest {

    private static final int emptySize = 0;

    private long size;
    private CacheSizeEvaluator<String, String> cacheSizeEvaluator;

    @BeforeEach
    void setUp() {
        final Function<String, Long> keyEvaluator = getKeyValueEvaluator();
        cacheSizeEvaluator = new CacheSizeEvaluator<String, String>() {
            @Override
            public long getEvaluatedSize() {
                return size;
            }

            @Override
            public void reset() {
                size = 0;
            }

            @Override
            public void added(String key, String value) {
                size += keyEvaluator.apply(key);
            }

            @Override
            public void removed(String key, String value) {
                size -= keyEvaluator.apply(key);
            }
        };
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(size);
    }

    private Function<String, Long> getKeyValueEvaluator() {
        return s -> 1L;
    }

    @Test
    void testAddedRemoveResetSize(){
        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(emptySize);
        long evaluatedSize = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(evaluatedSize);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
    }

    @Test
    void testNoSizeEvaluator() {
        cacheSizeEvaluator = CacheSizeEvaluator.noSizeEvaluator();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        size = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(size);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
    }

    @Test
    void testKeySizeGetIntance(){
        final Function<String, Long> keyEvaluator = s -> 1L;
        cacheSizeEvaluator = CacheSizeEvaluator.getInstance(keyEvaluator);
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);

        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(emptySize);
        size = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        cacheSizeEvaluator.added("key_3", "value_3");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(size);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);

    }

    @Test
    void testKeyValueSizeGetInstance() {
        final Function<String, Long> keyEvaluator = s -> 1L;
        final Function<String, Long> valueEvaluator = keyEvaluator;
        cacheSizeEvaluator = CacheSizeEvaluator.getInstance(keyEvaluator, valueEvaluator);
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(emptySize);
        size = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        cacheSizeEvaluator.added("key_3", "value_3");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(size);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(emptySize);
    }

    @Test
    void testNullValuesCreation() {
        assertThat(CacheSizeEvaluator.getInstance(null, null)).isNotNull();
        assertThat(CacheSizeEvaluator.getInstance(null)).isNotNull();
    }

    @Test
    void testNullValues() {
        cacheSizeEvaluator.added(null, null);
        cacheSizeEvaluator.added(null, "not_null");
        cacheSizeEvaluator.added("not_null", null);
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(emptySize);
    }

}
