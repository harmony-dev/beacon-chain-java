package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.discovery.enr.NodeRecordInfo;

import java.util.Optional;

/** Stores {@link NodeRecordInfo}'s in {@link NodeBucket}'s */
public interface NodeBucketStorage {
  Optional<NodeBucket> get(int index);

  void put(NodeRecordInfo nodeRecordInfo);

  void commit();
}
