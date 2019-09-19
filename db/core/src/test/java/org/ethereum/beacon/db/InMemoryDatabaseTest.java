package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
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

    //TODO: commit and close methods do nothing, is it ok?
}
