package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.db.source.SingleValueSource;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import org.ethereum.beacon.discovery.NodeTable;

/** Stores {@link NodeTable} and home node info */
public interface NodeTableStorage {
  NodeTable get();

  SingleValueSource<NodeRecordInfo> getHomeNodeSource();

  void commit();
}
