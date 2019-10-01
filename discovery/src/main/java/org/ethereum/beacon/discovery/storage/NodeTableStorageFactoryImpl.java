package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordV4;
import tech.pegasys.artemis.util.bytes.Bytes32;

import java.util.List;
import java.util.function.Supplier;

public class NodeTableStorageFactoryImpl implements NodeTableStorageFactory {

  private boolean isStorageEmpty(NodeTableStorage nodeTableStorage) {
    return nodeTableStorage.get().getHomeNode() == null;
  }

  /**
   * @return {@link NodeTableStorage} from `database` but if it doesn't exist, creates new one with
   *     home node provided by `homeNodeSupplier` and boot nodes provided with `bootNodesSupplier`.
   *     Uses `serializerFactory` for node records serialization.
   */
  @Override
  public NodeTableStorage createTable(
      Database database,
      SerializerFactory serializerFactory,
      Supplier<NodeRecordV4> homeNodeSupplier,
      Supplier<List<NodeRecord>> bootNodesSupplier) {
    NodeTableStorage nodeTableStorage = new NodeTableStorageImpl(database, serializerFactory);

    // Init storage with boot nodes if its empty
    if (isStorageEmpty(nodeTableStorage)) {
      nodeTableStorage
          .getHomeNodeSource()
          .set(NodeRecordInfo.createDefault(homeNodeSupplier.get()));
      bootNodesSupplier
          .get()
          .forEach(
              nodeRecord -> {
                if (!(nodeRecord instanceof NodeRecordV4)) {
                  throw new RuntimeException("Only V4 node records are supported as boot nodes");
                }
                NodeRecordInfo nodeRecordInfo = NodeRecordInfo.createDefault(nodeRecord);
                nodeTableStorage.get().save(nodeRecordInfo);
              });
    }
    ;

    return nodeTableStorage;
  }

  @Override
  public NodeBucketStorage createBuckets(
      Database database, SerializerFactory serializerFactory, Bytes32 homeNodeId) {
    return new NodeBucketStorageImpl(database, serializerFactory, homeNodeId);
  }
}
