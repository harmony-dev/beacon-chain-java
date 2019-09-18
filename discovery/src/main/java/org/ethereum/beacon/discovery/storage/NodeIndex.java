package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

import java.util.ArrayList;
import java.util.List;

/** Node Index. Stores several node keys. */
@SSZSerializable
public class NodeIndex {
  @SSZ private List<Hash32> entries;

  public NodeIndex() {
    this.entries = new ArrayList<>();
  }

  public List<Hash32> getEntries() {
    return entries;
  }

  public void setEntries(List<Hash32> entries) {
    this.entries = entries;
  }
}
