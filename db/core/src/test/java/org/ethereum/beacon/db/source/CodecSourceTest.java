package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CodecSourceTest {

    private CodecSource<String, String, String, String> source;

    @BeforeEach
    public void setUp() {
        source = new CodecSource<>(new HashMapDataSource<>(), Function.identity(), Function.identity(), Function.identity());
        assertThat(source).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("nullArgumentsProvider")
    public void testInvalidSourceCreation(DataSource ds, Function f1, Function f2, Function f3) {
        assertThatThrownBy(() -> new CodecSource(ds, f1, f2, f3))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> nullArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, Function.identity(), Function.identity(), Function.identity()),
                Arguments.of(new HashMapDataSource(), null, Function.identity(), Function.identity()),
                Arguments.of(new HashMapDataSource(), Function.identity(), null, Function.identity()),
                Arguments.of(new HashMapDataSource(), Function.identity(), Function.identity(), null)
        );
    }

    @ParameterizedTest
    @MethodSource("keyOnlyCodecSourceArgumentsProvider")
    public void testInvalidKeyOnlyCreation(DataSource ds, Function f1) {
        assertThatThrownBy(() -> new CodecSource.KeyOnly<>(ds, f1))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> keyOnlyCodecSourceArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, Function.identity()),
                Arguments.of(new HashMapDataSource(), null)
        );
    }

    @ParameterizedTest
    @MethodSource("valueOnlyCodecSourceArgumentsProvider")
    public void testInvalidValueOnlyCreation(DataSource ds, Function f1, Function f2) {
        assertThatThrownBy(() -> new CodecSource.ValueOnly<>(ds, f1, f2))
                .isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> valueOnlyCodecSourceArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, Function.identity(), Function.identity()),
                Arguments.of(new HashMapDataSource(), null, Function.identity()),
                Arguments.of(new HashMapDataSource(), Function.identity(), null)
        );
    }

    @ParameterizedTest
    @MethodSource("keyValueArgumentsProvider")
    public void testPutGetRemove(String key, String value) {
        assertThat(source.get(key)).isNotPresent();

        source.put(key, value);
        assertThat(source.get(key)).isPresent().hasValue(value);

        source.remove(key);
        assertThat(source.get(key)).isNotPresent();
    }

    private static Stream<Arguments> keyValueArgumentsProvider() {
        return Stream.of(
                Arguments.of("test_key", "test_value")
        );
    }

    @ParameterizedTest
    @MethodSource("keyValueArgumentsProvider")
    public void testKeyConversion(String key, String value) {
        source = new CodecSource<>(new HashMapDataSource<>(), k -> encode(k, key), Function.identity(), Function.identity());
        assertThat(source).isNotNull();
        assertThat(source.get(key)).isNotPresent();

        source.put(key, value);
        assertThat(source.get(key)).isPresent().hasValue(value);
    }

    private String encode(String value, String valueModifier) {
        return value.concat(valueModifier);
    }

    @ParameterizedTest
    @MethodSource("keyValueArgumentsProvider")
    public void testValueTargetValueToUpValueConversion(String key, String value) {
        source = new CodecSource<>(new HashMapDataSource<>(), k -> encode(k, key), k -> encode(k, value), Function.identity());
        assertThat(source).isNotNull();
        assertThat(source.get(key)).isNotPresent();

        source.put(key, value);
        assertThat(source.get(key)).isPresent().hasValue(value.concat(value));
    }

    @ParameterizedTest
    @MethodSource("keyValueArgumentsProvider")
    public void testValueUpValueToTargetValueConversion(String key, String value) {
        source = new CodecSource<>(new HashMapDataSource<>(), k -> encode(k, key), k -> encode(k, value), k -> decode());
        assertThat(source).isNotNull();
        assertThat(source.get(key)).isNotPresent();

        source.put(key, value);
        assertThat(source.get(key)).isPresent().hasValue("");
    }

    private String decode() {
        return "";
    }

}
