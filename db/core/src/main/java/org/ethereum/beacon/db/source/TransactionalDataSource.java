package org.ethereum.beacon.db.source;

/**
 * {@link DataSource} supporting transactions.
 */
public interface TransactionalDataSource<TxOptions extends TransactionalDataSource.TransactionOptions,
    KeyType, ValueType> extends ReadonlyDataSource<KeyType, ValueType> {

  enum IsolationLevel {
    Snapshot,
    RepeatableReads,
    ReadCommitted,
    ReadUncommitted
  }

  interface TransactionOptions {

    IsolationLevel getIsolationLevel();
  }

  interface Transaction<KeyType, ValueType> extends DataSource<KeyType, ValueType> {

    /**
     * Atomically commits all the changes to the underlying storage.
     * @throws TransactionException if conflicting changes were detected
     */
    default void commit() throws TransactionException {
      flush();
    }

    /**
     * Drops all the changes
     */
    void rollback();
  }

  /**
   * Starts a transaction with specified options
   * @param transactionOptions Transaction options
   * @return An isolated {@link DataSource}.
   * @throws IllegalArgumentException if transaction options supplied are not supported by implementation
   */
  Transaction<KeyType, ValueType> startTransaction(TxOptions transactionOptions);

  class TransactionException extends RuntimeException {
    public TransactionException(final String message) {
      super(message);
    }

    public TransactionException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }
}
