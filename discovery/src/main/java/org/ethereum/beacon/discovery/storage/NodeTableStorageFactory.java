package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface NodeTableStorageFactory {
  /**
   * Creates storage for nodes table
   *
   * @param database Database
   * @param serializerFactory Serializer factory
   * @param homeNodeProvider Home node provider, accepts old sequence number of home node, usually
   *     sequence number is increased by 1 on each restart and ENR is signed with new sequence
   *     number
   * @param bootNodesSupplier boot nodes provider
   *
   * @return {@link NodeTableStorage} from `database` but if it doesn't exist, creates new one with
   *     home node provided by `homeNodeSupplier` and boot nodes provided with `bootNodesSupplier`.
   *     Uses `serializerFactory` for node records serialization.
   */
  NodeTableStorage createTable(
      Database database,
      SerializerFactory serializerFactory,
      Function<UInt64, NodeRecord> homeNodeProvider,
      Supplier<List<NodeRecord>> bootNodesSupplier);

  NodeBucketStorage createBucketStorage(
      Database database, SerializerFactory serializerFactory, NodeRecord homeNode);
}
