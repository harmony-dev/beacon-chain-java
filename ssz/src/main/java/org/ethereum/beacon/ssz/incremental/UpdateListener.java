package org.ethereum.beacon.ssz.incremental;

public interface UpdateListener {

  void childUpdated(int childIndex);

  UpdateListener fork();
}
