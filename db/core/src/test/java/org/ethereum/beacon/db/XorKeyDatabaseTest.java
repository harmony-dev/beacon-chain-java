package org.ethereum.beacon.db;

import com.pholser.junit.quickcheck.*;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.ethereum.beacon.db.configuration.*;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.XorDataSource;
import org.junit.runner.RunWith;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitQuickcheck.class)
public class XorKeyDatabaseTest {

    private final String TEST_STORAGE_NAME = "TEST_STORAGE_NAME";
    private final String TEST_ENTITY_KEY = "TEST_ENTITY_KEY";

    @Property
    public void createStorage(@From(HashMapDatasourceGenerator.class) DataSource<@From(EmptyBytesValueGenerator.class) BytesValue, @From(EmptyBytesValueGenerator.class) BytesValue> backingDataSource,
                              @From(EmptyFunctionGenerator.class) Function<@From(EmptyBytesValueGenerator.class) BytesValue, @From(EmptyBytesValueGenerator.class) BytesValue> sourceNameHasher) {

        final XorKeyDatabase database = new XorKeyDatabase(backingDataSource, sourceNameHasher) {
            @Override
            public void commit() {
            }

            @Override
            public void close() {
            }
        };

        final DataSource<BytesValue, BytesValue> actual = database.createStorage(TEST_STORAGE_NAME);
        assertThat(actual).isNotNull();
        assertThat(actual).isInstanceOf(XorDataSource.class);

        final BytesValue key = BytesValue.wrap(TEST_ENTITY_KEY.getBytes());
        final BytesValue value = BytesValue.EMPTY;
        actual.put(key, value);
        assertThat(actual.get(key)).isPresent().hasValue(value);
    }

    @Property
    public void getBackingDataSource(@From(HashMapDatasourceGenerator.class) DataSource<@From(EmptyBytesValueGenerator.class) BytesValue, @From(EmptyBytesValueGenerator.class) BytesValue> backingDataSource,
                                     @From(EmptyFunctionGenerator.class) Function<@From(EmptyBytesValueGenerator.class) BytesValue, @From(EmptyBytesValueGenerator.class) BytesValue> sourceNameHasher) {

        final XorKeyDatabase actual = new XorKeyDatabase(backingDataSource, sourceNameHasher) {
            @Override
            public void commit() {
            }

            @Override
            public void close() {
            }
        };

        assertThat(actual).isNotNull();
        assertThat(actual.getBackingDataSource()).isNotNull();
        assertThat(actual.getBackingDataSource()).isEqualTo(backingDataSource);
    }
}
