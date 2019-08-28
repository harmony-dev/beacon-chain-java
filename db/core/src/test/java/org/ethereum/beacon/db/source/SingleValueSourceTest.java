package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.*;

import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SingleValueSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";
    private SingleValueSource<String> source;


    @BeforeEach
    public void setUp() {
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

        assertThat(source).isNotNull();
        assertThat(source.get()).isEmpty();
    }

    @Test
    public void testInvalidFromDefaultDataSource() {
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, TEST_KEY))
                .isInstanceOf(NullPointerException.class);

        assertThat(SingleValueSource.fromDataSource(new HashMapDataSource<>(), null)).isNotNull();
        assertThat(SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY)).isNotNull();
    }

    @Test
    public void testInvalidFromDataSource() {
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, null, null, Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, null, Function.identity(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, null, Function.identity(), Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(new HashMapDataSource<>(), null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, Function.identity(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(new HashMapDataSource<>(), null, Function.identity(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(new HashMapDataSource<>(), null, null, Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, null, Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, TEST_KEY, Function.identity(), Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, TEST_KEY, Function.identity(), null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, TEST_KEY, null, Function.identity()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> SingleValueSource.fromDataSource(null, TEST_KEY, null, null))
                .isInstanceOf(NullPointerException.class);


        assertThat(SingleValueSource.fromDataSource(new HashMapDataSource<>(), null, Function.identity(), Function.identity())).isNotNull();
        assertThat(SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, Function.identity(), Function.identity())).isNotNull();
    }

    @Test
    public void testGetSetRemoveSourceValue() {
        source = SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, Function.identity(), Function.identity());
        assertThat(source).isNotNull();
        assertThat(source.get()).isNotPresent();
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue(TEST_VALUE);
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    @Test
    public void testGetSetRemoveWithEncode() {
        source = SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, key -> encode(key, TEST_VALUE), Function.identity());
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue(TEST_VALUE.concat(TEST_VALUE));
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    @Test
    public void testGetSetRemoveWithEncodeDecode() {
        source = SingleValueSource.fromDataSource(new HashMapDataSource<>(), TEST_KEY, key -> encode(key, TEST_VALUE), key -> decode());
        source.set(TEST_VALUE);
        assertThat(source.get()).isPresent().hasValue("");
        source.remove();
        assertThat(source.get()).isEmpty();
    }

    @Test
    public void memSourceGetSetRemoveTest() {
        source = SingleValueSource.memSource();
        assertThat(source).isNotNull();
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
