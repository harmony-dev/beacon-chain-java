package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordV5;

import java.util.List;
import java.util.function.Supplier;

public interface NodeTableStorageFactory {
  NodeTableStorage create(
      Database database,
      SerializerFactory serializerFactory,
      Supplier<NodeRecordV5> homeNodeSupplier,
      Supplier<List<NodeRecord>> bootNodes);
}
