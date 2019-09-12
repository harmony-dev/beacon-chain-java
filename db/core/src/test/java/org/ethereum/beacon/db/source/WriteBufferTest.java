package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class WriteBufferTest {

    private WriteBuffer<String, String> buffer;
    private static CacheSizeEvaluator<String, String> evaluator;
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

    @ParameterizedTest
    @MethodSource("invalidArgumentsProvider")
    void testInvalidWriteBufferCreation(DataSource ds, CacheSizeEvaluator evaluator, boolean upstreamFlush) {
        assertThatThrownBy(() -> new WriteBuffer(ds, evaluator, upstreamFlush)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> invalidArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null, false),
                Arguments.of(null, null, true),
                Arguments.of(null, evaluator, false),
                Arguments.of(null, evaluator, true),
                Arguments.of(new HashMapDataSource<>(), null, false),
                Arguments.of(new HashMapDataSource<>(), null, true)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidArgsProvider")
    void testInvalidArgsWriteBufferCreation(DataSource ds, boolean upstreamFlush) {
        assertThatThrownBy(() -> new WriteBuffer(ds, upstreamFlush)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> invalidArgsProvider() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(null, true)
        );
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

    @ParameterizedTest
    @MethodSource("nullArgumentsProvider")
    void testNullValues(String key, String value) {
        assertThatThrownBy(() -> buffer.put(key, value)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.get(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.remove(null)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("not_null", null),
                Arguments.of(null, "not_null")
        );
    }
}