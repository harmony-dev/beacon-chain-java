package org.ethereum.beacon.db.source;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TransactionalDataSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";
    private final String TEST_FLUSH = "flush";

    private TransactionalDataSource.Transaction<String, String> transaction;
    private TransactionalDataSource<TransactionalDataSource.TransactionOptions, String, String> transactionalDataSource;

    @BeforeEach
    public void setUp()
    {
        Map<String, String> store = new ConcurrentHashMap<>();
        transaction = new TransactionalDataSource.Transaction<String, String>() {
            @Override
            public void rollback() {
                store.clear();
            }

            @Override
            public Optional<String> get(@Nonnull String key) {
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public void put(@Nonnull String key, @Nonnull String value) {
                store.put(key, value);
            }

            @Override
            public void remove(@Nonnull String key) {
                store.remove(key);
            }

            @Override
            public void flush() {
                store.put(TEST_FLUSH, TEST_FLUSH);
            }
        };
        assertThat(transaction).isNotNull();


        transactionalDataSource = new TransactionalDataSource<TransactionalDataSource.TransactionOptions, String, String>() {
            @Override
            public Transaction<String, String> startTransaction(TransactionOptions transactionOptions) {
                return transaction;
            }

            @Override
            public Optional<String> get(@Nonnull String key) {
                return transaction.get(key);
            }
        };

        assertThat(transactionalDataSource).isNotNull();
    }

    @Test
    public void testValidStartTransaction() {
        assertThat((TransactionalDataSource.TransactionOptions) () -> TransactionalDataSource.IsolationLevel.Snapshot).isNotNull();
        assertThat((TransactionalDataSource.TransactionOptions) () -> TransactionalDataSource.IsolationLevel.RepeatableReads).isNotNull();
        assertThat((TransactionalDataSource.TransactionOptions) () -> TransactionalDataSource.IsolationLevel.ReadUncommitted).isNotNull();
        assertThat((TransactionalDataSource.TransactionOptions) () -> TransactionalDataSource.IsolationLevel.ReadCommitted).isNotNull();

        TransactionalDataSource.TransactionOptions transactionOptions = () -> TransactionalDataSource.IsolationLevel.Snapshot;
        assertThat(transactionalDataSource.startTransaction(transactionOptions)).isEqualTo(transaction);
        assertThat(transactionOptions.getIsolationLevel()).isIn(TransactionalDataSource.IsolationLevel.values());
    }

    @Test
    public void testInvalidStartTransaction() {
        TransactionalDataSource.TransactionOptions transactionOptions = () -> TransactionalDataSource.IsolationLevel.valueOf("illegalArgument");
        assertThat(transactionOptions).isNotNull();
        assertThat(transactionalDataSource.startTransaction(transactionOptions)).isEqualTo(transaction);
        assertThat(transactionalDataSource.startTransaction(null)).isEqualTo(transaction);
        assertThatThrownBy( () -> transactionOptions.getIsolationLevel().equals(TransactionalDataSource.IsolationLevel.Snapshot)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetPutRemoveElementsInDb() {
        transaction.put(TEST_KEY, TEST_VALUE);
        assertThat(transactionalDataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
        assertThat(transaction.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);
        transaction.remove(TEST_KEY);
        assertThat(transaction.get(TEST_KEY)).isNotPresent();
        transaction.commit();
        assertThat(transaction.get(TEST_FLUSH)).isPresent().hasValue(TEST_FLUSH);
    }

    @Test
    void testRollback() {
        transaction.put("test_key_1", "test_value_1");
        transaction.put("test_key_2", "test_value_2");
        assertThat(transactionalDataSource.get("test_key_1")).isPresent().hasValue("test_value_1");
        assertThat(transaction.get("test_key_1")).isPresent().hasValue("test_value_1");
        transaction.rollback();
        assertThat(transaction.get("test_value_1")).isNotPresent();
        assertThat(transaction.get("test_key_2")).isNotPresent();
    }

    @Test
    void testNullValues() {
        assertThatThrownBy(() -> transaction.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> transaction.put("not_null", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> transaction.put(null, "not_null")).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testException() {
        assertThat(new TransactionalDataSource.TransactionException("exception")).isNotNull();
        assertThat(new TransactionalDataSource.TransactionException("exception", new Throwable(""))).isNotNull();
    }
}