package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class CacheSizeEvaluatorTest {

    private long size;
    private CacheSizeEvaluator<String, String> cacheSizeEvaluator;

    @BeforeEach
    void setUp() {
        size = 0;
        final Function<String, Long> keyEvaluator = s -> 1L;
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
        assertThat(cacheSizeEvaluator).isNotNull();
    }

    @Test
    void testAddedRemoveResetSize(){
        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(0);
        long evaluatedSize = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(evaluatedSize);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
    }

    @Test
    void testNoSizeEvaluator() {
        cacheSizeEvaluator = CacheSizeEvaluator.noSizeEvaluator();
        assertThat(cacheSizeEvaluator).isNotNull();
        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
        size = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(size);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
    }

    @Test
    void testKeySizeGetIntance(){
        final Function<String, Long> keyEvaluator = s -> 1L;
        cacheSizeEvaluator = CacheSizeEvaluator.getInstance(keyEvaluator);
        assertThat(cacheSizeEvaluator).isNotNull();

        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(0);
        size = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        cacheSizeEvaluator.added("key_3", "value_3");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(size);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);

    }

    @Test
    void testKeyValueSizeGetInstance() {
        final Function<String, Long> keyEvaluator = s -> 1L;
        final Function<String, Long> valueEvaluator = keyEvaluator;
        cacheSizeEvaluator = CacheSizeEvaluator.getInstance(keyEvaluator, valueEvaluator);
        assertThat(cacheSizeEvaluator).isNotNull();
        cacheSizeEvaluator.added("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(0);
        size = cacheSizeEvaluator.getEvaluatedSize();
        cacheSizeEvaluator.removed("key_0", "value_0");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
        cacheSizeEvaluator.added("key_1", "value_1");
        cacheSizeEvaluator.added("key_2", "value_2");
        cacheSizeEvaluator.added("key_3", "value_3");
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(size);

        cacheSizeEvaluator.reset();
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isEqualTo(0);
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
        assertThat(cacheSizeEvaluator.getEvaluatedSize()).isGreaterThan(0);
    }

}