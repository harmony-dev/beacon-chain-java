package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


//TODO: need more tests
class BufferSizeObserverTest {

    private WriteBuffer<String, String> buffer;
    private WriteBuffer<String, String> commitTrack;
    private BufferSizeObserver observer;

    @BeforeEach
    void setUp() {
        final HashMapDataSource<String, String> upstream = new HashMapDataSource<>();
        buffer = new WriteBuffer<>(upstream, true);
        assertThat(buffer.getUpstream()).isEqualTo(upstream);
        assertThat(buffer.evaluateSize()).isEqualTo(0);

        commitTrack = new WriteBuffer<>(upstream, true);
        assertThat(commitTrack.getUpstream()).isEqualTo(upstream);
        assertThat(commitTrack.evaluateSize()).isEqualTo(0);

        observer = new BufferSizeObserver(buffer, commitTrack, Long.MAX_VALUE);
    }

    @ParameterizedTest
    @MethodSource("nullArgumentsProvider")
    void testInvalidCreation(WriteBuffer<String, String> buffer, WriteBuffer<String, String> commitTrack, long bufferLimitSize) {
        assertThatThrownBy(() -> new BufferSizeObserver(buffer, commitTrack, bufferLimitSize)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null, Long.MAX_VALUE),
                Arguments.of(null, new WriteBuffer<String, String>(new HashMapDataSource<>(), true), Long.MAX_VALUE),
                Arguments.of(new WriteBuffer<String, String>(new HashMapDataSource<>(), true), null, Long.MAX_VALUE)
        );
    }

    @ParameterizedTest
    @MethodSource("nullKeyValueArgumentsProvider")
    void testPutNullKeyValue(String key, String value) {
        assertThatThrownBy(() -> buffer.put(key, value)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullKeyValueArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("not_null", null)
        );
    }

    @Test
    void testCreate() {

        String TEST_KEY_0 = "TEST_KEY_0";
        String TEST_VALUE_0 = "TEST_VALUE_0";
        String TEST_KEY_1 = "TEST_KEY_1";
        String TEST_VALUE_1 = "TEST_VALUE_1";

        buffer = new WriteBuffer<>(new HashMapDataSource<>(), false);
        observer = BufferSizeObserver.create(buffer, Long.MAX_VALUE);
        buffer.put(TEST_KEY_0, TEST_VALUE_0);


        assertThat(buffer.get(TEST_KEY_0)).isPresent().hasValue(TEST_VALUE_0);
        assertThat(buffer.getCacheEntry(TEST_KEY_0)).isPresent();
        assertTrue(buffer.evaluateSize() >= Long.MIN_VALUE, "Needed db flush");

        observer.flush();
        assertThat(buffer.getCacheEntry(TEST_KEY_0)).isNotPresent();
        assertThat(buffer.getUpstream().get(TEST_KEY_0)).isPresent().hasValue(TEST_VALUE_0);

        buffer.put(TEST_KEY_1, TEST_VALUE_1);

        assertThat(buffer.getUpstream().get(TEST_KEY_1)).isNotPresent();
        assertThat(buffer.getCacheEntry(TEST_KEY_1)).isPresent();

        observer.commit();
        assertThat(buffer.getUpstream().get(TEST_KEY_1)).isPresent().hasValue(TEST_VALUE_1);
    }

    @ParameterizedTest
    @CsvSource({ "test_key, test_value"})
    void testFlush(String key, String value) {
        buffer.put(key, value);
        assertThat(buffer.get(key)).isPresent().hasValue(value);

        observer.flush();
        assertThat(buffer.getUpstream().get(key)).isPresent().hasValue(value);

        buffer.getUpstream().remove(key);
        assertThat(buffer.get(key)).isNotPresent();
        assertThat(buffer.getUpstream().get(key)).isNotPresent();

    }

    @ParameterizedTest
    @CsvSource({ "test_key, test_value"})
    void testCommit(String key, String value) {
        commitTrack.put(key, value);
        assertThat(commitTrack.get(key)).isPresent().hasValue(value);
        assertThat(commitTrack.getUpstream().get(key)).isNotPresent();

        observer.commit();
        assertThat(commitTrack.get(key)).isPresent().hasValue(value);
        assertThat(commitTrack.getUpstream().get(key)).isPresent().hasValue(value);

        commitTrack.getUpstream().remove(key);
        assertThat(commitTrack.get(key)).isNotPresent();
        assertThat(commitTrack.getUpstream().get(key)).isNotPresent();
        assertThat(buffer.getUpstream().get(key)).isNotPresent();
    }
}
