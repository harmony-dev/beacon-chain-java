package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.*;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class CodecSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";

    private CodecSource<String, String, String, String> source;

    @Before
    public void setUp() {
        source = new CodecSource<>(new HashMapDataSource<>(), Function.identity(), Function.identity(), Function.identity());
        assertThat(source).isNotNull();
        assertThat(source.get(TEST_KEY)).isNotPresent();
    }

    @Test
    public void testValidSourceCreation() {
        assertThatThrownBy(() -> new CodecSource(null, Function.identity(), Function.identity(), Function.identity()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource(new HashMapDataSource(), null, Function.identity(), Function.identity()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource(new HashMapDataSource(), Function.identity(), null, Function.identity()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource(new HashMapDataSource(), Function.identity(), Function.identity(), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource.KeyOnly<>(null, Function.identity()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource.KeyOnly<>(new HashMapDataSource<>(), null))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource.ValueOnly<>(null, Function.identity(), Function.identity()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource.ValueOnly<>(new HashMapDataSource<>(), null, Function.identity()))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new CodecSource.ValueOnly<>(new HashMapDataSource<>(), Function.identity(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testPutGetRemove() {
        source.put(TEST_KEY, TEST_VALUE);
        assertThat(source.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);

        source.remove(TEST_KEY);
        assertThat(source.get(TEST_KEY)).isNotPresent();
    }

    @Test
    public void testKeyConversion() {
        source = new CodecSource<>(new HashMapDataSource<>(), key -> encode(key, TEST_KEY), Function.identity(), Function.identity());
        assertThat(source).isNotNull();
        assertThat(source.get(TEST_KEY)).isNotPresent();

        source.put(TEST_KEY, TEST_VALUE);
        assertThat(source.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
    }

    private String encode(String value, String valueModifier) {
        return value.concat(valueModifier);
    }

    @Test
    public void testValueTargetValueToUpValueConversion() {
        source = new CodecSource<>(new HashMapDataSource<>(), key -> encode(key, TEST_KEY), key -> encode(key, TEST_VALUE), Function.identity());
        assertThat(source).isNotNull();
        assertThat(source.get(TEST_KEY)).isNotPresent();

        source.put(TEST_KEY, TEST_VALUE);
        assertThat(source.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE.concat(TEST_VALUE));
    }

    @Test
    public void testValueUpValueToTargetValueConversion() {
        source = new CodecSource<>(new HashMapDataSource<>(), key -> encode(key, TEST_KEY), key -> encode(key, TEST_VALUE), key -> decode());
        assertThat(source).isNotNull();
        assertThat(source.get(TEST_KEY)).isNotPresent();

        source.put(TEST_KEY, TEST_VALUE);
        assertThat(source.get(TEST_KEY)).isPresent().hasValue("");
    }

    private String decode() {
        return "";
    }

}
