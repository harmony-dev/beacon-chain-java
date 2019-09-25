package org.ethereum.beacon.discovery.storage;

import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.ArrayList;
import java.util.List;

/** Node Index. Stores several node keys. */
public class NodeIndex {
  private List<Hash32> entries;

  public NodeIndex() {
    this.entries = new ArrayList<>();
  }

  public static NodeIndex fromRlpBytes(BytesValue bytes) {
    RlpList internalList = (RlpList) RlpDecoder.decode(bytes.extractArray()).getValues().get(0);
    List<Hash32> entries = new ArrayList<>();
    for (RlpType entry : internalList.getValues()) {
      entries.add(Hash32.wrap(Bytes32.wrap(((RlpString) entry).getBytes())));
    }
    NodeIndex res = new NodeIndex();
    res.setEntries(entries);
    return res;
  }

  public List<Hash32> getEntries() {
    return entries;
  }

  public void setEntries(List<Hash32> entries) {
    this.entries = entries;
  }

  public BytesValue toRlpBytes() {
    List<RlpType> values = new ArrayList<>();
    for (Hash32 hash32 : getEntries()) {
      values.add(RlpString.create(hash32.extractArray()));
    }
    byte[] bytes = RlpEncoder.encode(new RlpList(values));
    return BytesValue.wrap(bytes);
  }
}
