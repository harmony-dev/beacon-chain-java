package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.db.source.SingleValueSource;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;

/** Stores {@link NodeTable} and home node info */
public interface NodeTableStorage {
  SerializerFactory DEFAULT_SERIALIZER = new NodeSerializerFactory();

  NodeTable get();

  SingleValueSource<NodeRecordInfo> getHomeNodeSource();

  void commit();
}
