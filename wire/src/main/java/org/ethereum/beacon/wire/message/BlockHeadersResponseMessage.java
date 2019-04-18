package org.ethereum.beacon.wire.message;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.types.SlotNumber;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.ethereum.core.Hash32;

@SSZSerializable
public class BlockHeadersResponseMessage extends Message {

  @SSZ private final List<BeaconBlockHeader> headers;

  public BlockHeadersResponseMessage(List<BeaconBlockHeader> headers) {
    this.headers = headers;
  }

  public List<BeaconBlockHeader> getHeaders() {
    return headers;
  }
}
