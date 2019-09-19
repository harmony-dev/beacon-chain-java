package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDatabaseTest {

    @ParameterizedTest
    @ValueSource(strings = {"TEST_KEY"})
    void testGetBackingDataSource(String param) {
        final InMemoryDatabase database = new InMemoryDatabase();
        final DataSource<BytesValue, BytesValue> dataSource = database.getBackingDataSource();
        final BytesValue key = BytesValue.wrap(param.getBytes());
        final BytesValue value = BytesValue.EMPTY;
        dataSource.put(key, value);

        final Optional<BytesValue> expected = dataSource.get(key);
        assertThat(expected).isPresent().hasValue(value);
        assertThat(database.getBackingDataSource().get(key)).isPresent().hasValue(expected.get());

        dataSource.remove(key);
        assertThat(dataSource.get(key)).isNotPresent();
        assertThat(database.getBackingDataSource().get(key)).isNotPresent();
    }

    @Test
    void testPutGetRemove_DifferentDatabase() {
        final InMemoryDatabase first = new InMemoryDatabase();
        final InMemoryDatabase second = new InMemoryDatabase();

        final BytesValue key = BytesValue.of(123);
        final BytesValue value = BytesValue.of(1, 2, 3);

        final DataSource<BytesValue, BytesValue> firstDS = first.getBackingDataSource();
        final DataSource<BytesValue, BytesValue> secondDS = second.getBackingDataSource();

        firstDS.put(key, value);
        assertThat(first.getBackingDataSource().get(key)).isPresent().hasValue(value);
        assertThat(secondDS.get(key)).isNotPresent();

        firstDS.put(key, value);
        secondDS.put(key, value);
        assertThat(first.getBackingDataSource().get(key)).isPresent().hasValue(value);
        assertThat(second.getBackingDataSource().get(key)).isPresent().hasValue(value);

        firstDS.remove(key);
        assertThat(first.getBackingDataSource().get(key)).isNotPresent();
        assertThat(second.getBackingDataSource().get(key)).isPresent().hasValue(value);
    }

    //TODO: commit and close methods do nothing, is it ok?
}
