package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class WriteBufferTest {

    private WriteBuffer<String, String> buffer;
    private CacheSizeEvaluator<String, String> evaluator;
    private Function<String, Long> keyValueEvaluator;
    private HashMapDataSource dataSource;

    @BeforeEach
    void setUp() {
        keyValueEvaluator = s -> 1L;
        evaluator = CacheSizeEvaluator.getInstance(keyValueEvaluator);
        assertThat(keyValueEvaluator).isNotNull();
        assertThat(evaluator).isNotNull();
        dataSource = new HashMapDataSource();
        buffer = new WriteBuffer<>(dataSource, evaluator, true);
        assertThat(buffer).isNotNull();
    }

    @Test
    void testInvalidWriteBufferCreation() {
        assertThatThrownBy(() -> new WriteBuffer(null, null, false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WriteBuffer(null, null, true)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WriteBuffer(null, evaluator, false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WriteBuffer(new HashMapDataSource(), null, false)).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new WriteBuffer(null,false)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new WriteBuffer(null,true)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetPutRemoveFlushReset()
    {
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        buffer.put("test_key_0","test_value_0");
        buffer.put("test_key_1","test_value_1");
        buffer.put("test_key_2","test_value_2");
        buffer.put("test_key_3","test_value_3");
        buffer.put("test_key_4","test_value_4");
        assertThat(buffer.evaluateSize()).isGreaterThan(0);
        assertThat(buffer.getCacheEntry("test_key_0")).isPresent().hasValue(Optional.of("test_value_0"));
        buffer.doFlush();
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        assertThat(buffer.getUpstream().get("test_key_0")).isPresent().hasValue("test_value_0");
        buffer.reset();
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        assertThat(buffer.getUpstream().get("test_key_0")).isPresent().hasValue("test_value_0");
    }

    @Test
    void testGetPutRemoveFlushResetNoSize()
    {
        buffer = new WriteBuffer<>(new HashMapDataSource<>(), true);
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        buffer.put("test_key_0","test_value_0");
        buffer.put("test_key_1","test_value_1");
        buffer.put("test_key_2","test_value_2");
        buffer.put("test_key_3","test_value_3");
        buffer.put("test_key_4","test_value_4");
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        assertThat(buffer.getCacheEntry("test_key_0")).isPresent().hasValue(Optional.of("test_value_0"));
        buffer.doFlush();
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        assertThat(buffer.getUpstream().get("test_key_0")).isPresent().hasValue("test_value_0");
        buffer.reset();
        assertThat(buffer.evaluateSize()).isEqualTo(0);
        assertThat(buffer.getUpstream().get("test_key_0")).isPresent().hasValue("test_value_0");
    }

    @Test
    void testNullValues() {
        assertThatThrownBy(() -> buffer.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.put("not_null", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.put(null, "not_null")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.remove(null)).isInstanceOf(NullPointerException.class);
    }
}