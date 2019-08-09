package org.ethereum.beacon.db;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitQuickcheck.class)
public class InMemoryDatabaseTest {

    @Property
    public void getBackingDataSource() {
        final InMemoryDatabase database = new InMemoryDatabase();
        assertThat(database.getBackingDataSource()).isNotNull();
        assertThat(database.getBackingDataSource()).isInstanceOf(HashMapDataSource.class);
    }
}
