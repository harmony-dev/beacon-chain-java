package org.ethereum.beacon.db;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.ethereum.beacon.db.source.DataSource;
import org.junit.runner.RunWith;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitQuickcheck.class)
public class InMemoryDatabaseTest {

    private final String TEST_KEY = "TEST_KEY";

    @Property
    public void testGetBackingDataSource() {
        final InMemoryDatabase database = new InMemoryDatabase();
        final DataSource<BytesValue, BytesValue> dataSource = database.getBackingDataSource();
        assertThat(dataSource).isNotNull();

        final BytesValue key = BytesValue.wrap(TEST_KEY.getBytes());
        final BytesValue value = BytesValue.EMPTY;
        dataSource.put(key, value);

        assertThat(dataSource.get(key)).isPresent();
    }
}
