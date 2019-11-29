package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.CodecSource;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import org.ethereum.beacon.db.source.SingleValueSource;
import org.ethereum.beacon.db.source.impl.DataSourceList;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/** Creates NodeTableStorage containing NodeTable with indexes */
public class NodeTableStorageImpl implements NodeTableStorage {
  public static final String NODE_TABLE_STORAGE_NAME = "node-table";
  public static final String INDEXES_STORAGE_NAME = "node-table-index";
  private static final Hash32 HOME_NODE_KEY =
      Hash32.wrap(Hashes.sha256(BytesValue.wrap("HOME_NODE".getBytes())));
  private final DataSource<BytesValue, BytesValue> nodeTableSource;
  private final DataSource<BytesValue, BytesValue> nodeIndexesSource;
  private final SingleValueSource<NodeRecordInfo> homeNodeSource;
  private final NodeTable nodeTable;

  public NodeTableStorageImpl(Database database, SerializerFactory serializerFactory) {
    DataSource<BytesValue, BytesValue> nodeTableSource =
        database.createStorage(NODE_TABLE_STORAGE_NAME);
    this.nodeTableSource = nodeTableSource;
    DataSource<BytesValue, BytesValue> nodeIndexesSource =
        database.createStorage(INDEXES_STORAGE_NAME);
    this.nodeIndexesSource = nodeIndexesSource;

    DataSource<Hash32, NodeRecordInfo> nodeTable =
        new CodecSource<>(
            nodeTableSource,
            key -> key,
            serializerFactory.getSerializer(NodeRecordInfo.class),
            serializerFactory.getDeserializer(NodeRecordInfo.class));
    HoleyList<NodeIndex> nodeIndexesTable =
        new DataSourceList<>(
            nodeIndexesSource,
            serializerFactory.getSerializer(NodeIndex.class),
            serializerFactory.getDeserializer(NodeIndex.class));
    this.homeNodeSource = SingleValueSource.fromDataSource(nodeTable, HOME_NODE_KEY);
    this.nodeTable = new NodeTableImpl(nodeTable, nodeIndexesTable, homeNodeSource);
  }

  @Override
  public NodeTable get() {
    return nodeTable;
  }

  @Override
  public SingleValueSource<NodeRecordInfo> getHomeNodeSource() {
    return homeNodeSource;
  }

  @Override
  public void commit() {
    nodeTableSource.flush();
    nodeIndexesSource.flush();
  }
}
