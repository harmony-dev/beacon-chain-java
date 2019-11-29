package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SingleValueSourceTest {

    private static final String TEST_KEY = "test_key";
    private static final String TEST_VALUE = "test_value";
    private SingleValueSource<String> source;

    @BeforeEach
    void setUp() {
        source = new SingleValueSource<String>() {
            String value;

            @Override
            public Optional<String> get() {
                return Optional.ofNullable(value);
            }

            @Override
            public void set(String value) {
                this.value = value;
            }

            @Override
            public void remove() {
                this.value = null;
            }
        };

        assertThat(source.get()).isEmpty();
    }

    @Tag("FIX")
    @ParameterizedTest
    @MethodSource("invalidFromDefaultDataSourceArgumentsProvider")
    void testInvalidFromDefaultDataSource(DataSource ds, Object key) {
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(ds, key)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> invalidFromDefaultDataSourceArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, TEST_KEY),
                Arguments.of(new HashMapDataSource(), null)
        );
    }

    @Test
    void testValidFromDefaultDataSource() {
        assertThat(SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY)).isNotNull();
    }


    @Tag("FIX")
    @ParameterizedTest
    @MethodSource("invalidFromDataSourceArgumentsProvider")
    void testInvalidFromDataSource(DataSource ds, Object key, Function coder, Function decoder) {
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(ds, key, coder, decoder)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> invalidFromDataSourceArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null, null, null),
                Arguments.of(null, null, null, Function.identity()),
                Arguments.of(null, null, Function.identity(), null),
                Arguments.of(null, null, Function.identity(), Function.identity()),
                Arguments.of(new HashMapDataSource<>(), null, null, null),
                Arguments.of(new HashMapDataSource<>(), TEST_KEY, null, null),
                Arguments.of(new HashMapDataSource<>(), TEST_KEY, Function.identity(), null),
                Arguments.of(new HashMapDataSource<>(), null, Function.identity(), null),
                Arguments.of(new HashMapDataSource<>(), null, null, Function.identity()),
                Arguments.of(new HashMapDataSource<>(), TEST_KEY, null, Function.identity()),
                Arguments.of(null, TEST_KEY, Function.identity(), Function.identity()),
                Arguments.of(null, TEST_KEY, Function.identity(), null),
                Arguments.of(null, TEST_KEY, null, Function.identity()),
                Arguments.of(null, TEST_KEY, null, null),
                Arguments.of(new HashMapDataSource<>(), null, Function.identity(), Function.identity())
        );
    }


    @Test
    void testValidFromDataSource() {
        assertThat(SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, Function.identity(), Function.identity())).isNotNull();
    }

    @Test
    void testGetSetRemoveSourceValue() {
        source = SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, Function.identity(), Function.identity());
        assertThat(source.get()).isNotPresent();
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue(TEST_VALUE);
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    @Test
    void testGetSetRemoveWithEncode() {
        source = SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, key -> encode(key, TEST_VALUE), Function.identity());
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue(TEST_VALUE.concat(TEST_VALUE));
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    @Test
    void testGetSetRemoveWithEncodeDecode() {
        source = SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, key -> encode(key, TEST_VALUE), key -> decode());
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue("");
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    @Test
    void memSourceGetSetRemoveTest() {
        source = SingleValueSource.memSource();
        assertThat(source.get()).isNotPresent();
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue(TEST_VALUE);
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    private String encode(String value, String valueModifier) {
        return value.concat(valueModifier);
    }

    private String decode() {
        return "";
    }
}
