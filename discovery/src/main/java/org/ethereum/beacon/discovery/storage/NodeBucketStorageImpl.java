package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.db.Database;
import org.ethereum.beacon.db.source.DataSource;
import org.ethereum.beacon.db.source.HoleyList;
import org.ethereum.beacon.db.source.impl.DataSourceList;
import org.ethereum.beacon.discovery.Functions;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.Optional;

/**
 * Stores {@link NodeRecordInfo}'s in {@link NodeBucket}'s calculating index number of bucket as
 * {@link Functions#logDistance(Bytes32, Bytes32)} from homeNodeId and ignoring index above {@link
 * #MAXIMUM_BUCKET}
 */
public class NodeBucketStorageImpl implements NodeBucketStorage {
  public static final String NODE_BUCKET_STORAGE_NAME = "node-bucket-table";
  public static final int MAXIMUM_BUCKET = 256;
  private final HoleyList<NodeBucket> nodeBucketsTable;
  private final Bytes32 homeNodeId;

  public NodeBucketStorageImpl(
      Database database, SerializerFactory serializerFactory, Bytes32 homeNodeId) {
    DataSource<BytesValue, BytesValue> nodeBucketsSource =
        database.createStorage(NODE_BUCKET_STORAGE_NAME);
    this.nodeBucketsTable =
        new DataSourceList<>(
            nodeBucketsSource,
            serializerFactory.getSerializer(NodeBucket.class),
            serializerFactory.getDeserializer(NodeBucket.class));
    this.homeNodeId = homeNodeId;
  }

  @Override
  public Optional<NodeBucket> get(int index) {
    return nodeBucketsTable.get(index);
  }

  @Override
  public void put(NodeRecordInfo nodeRecordInfo) {
    int logDistance = Functions.logDistance(homeNodeId, nodeRecordInfo.getNode().getNodeId());
    if (logDistance <= MAXIMUM_BUCKET) {
      Optional<NodeBucket> nodeBucketOpt = nodeBucketsTable.get(logDistance);
      if (nodeBucketOpt.isPresent()) {
        NodeBucket nodeBucket = nodeBucketOpt.get();
        boolean updated = nodeBucket.put(nodeRecordInfo);
        if (updated) {
          nodeBucketsTable.put(logDistance, nodeBucket);
        }
      } else {
        NodeBucket nodeBucket = new NodeBucket();
        nodeBucket.put(nodeRecordInfo);
        nodeBucketsTable.put(logDistance, nodeBucket);
      }
    }
  }

  @Override
  public void commit() {}
}
