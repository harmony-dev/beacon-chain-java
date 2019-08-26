package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @Test
    void inMemoryDB() {
        final Database database = Database.inMemoryDB();
        assertThat(database).isNotNull();
    }

    @Tag("FIX")
    @ParameterizedTest
    @MethodSource("invalidArgumentsProvider")
    void testInvalidCreation(String path, long bufferLimitsInBytes) {
        assertThatThrownBy(() -> Database.rocksDB(path, bufferLimitsInBytes)).isInstanceOf(NullPointerException.class);
    }

    private static Stream<Arguments> invalidArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, 0),
                Arguments.of("", 0)
        );
    }

    @ParameterizedTest
    @MethodSource("validArgumentsProvider")
    void testValidCreation(String path, long bufferLimitsInBytes) {
        final Database database = Database.rocksDB(path, bufferLimitsInBytes);
        assertThat(database).isNotNull();
    }

    private static Stream<Arguments> validArgumentsProvider() {
        return Stream.of(
                Arguments.of("/tmp", 0),
                Arguments.of("/tmp", Long.MAX_VALUE),
                Arguments.of("/tmp", Long.MIN_VALUE)
        );
    }


    private static final String TEST_STORAGE_NAME = "TEST_STORAGE_NAME";

    private boolean committed;
    private boolean closed;
    private String storageName;

    @Test
    void testDatabaseCommitCloseCreateStorage() {
        final DataSource<BytesValue, BytesValue> dataSource = new HashMapDataSource<>();
        committed = false;
        closed = false;
        storageName = null;

        final Database db = new Database() {
            @Override
            public DataSource<BytesValue, BytesValue> createStorage(String name) {
                storageName = name;
                return dataSource;
            }

            @Override
            public void commit() {
                committed = true;
            }

            @Override
            public void close() {
                closed = true;
            }
        };

        assertThat(db).isNotNull();
        final DataSource<BytesValue, BytesValue> storage = db.createStorage(TEST_STORAGE_NAME);
        assertThat(storage).isNotNull();
        assertThat(storage).isEqualTo(dataSource);
        assertThat(storageName).isEqualTo(TEST_STORAGE_NAME);

        assertFalse(committed);
        db.commit();
        assertTrue(committed);

        assertFalse(closed);
        db.close();
        assertTrue(closed);
    }
}
