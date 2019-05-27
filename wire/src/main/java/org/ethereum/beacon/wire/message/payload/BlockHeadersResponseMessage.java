package org.ethereum.beacon.wire.message.payload;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.wire.message.ResponseMessagePayload;

@SSZSerializable
public class BlockHeadersResponseMessage extends ResponseMessagePayload {

  @SSZ private final List<BeaconBlockHeader> headers;

  public BlockHeadersResponseMessage(List<BeaconBlockHeader> headers) {
    this.headers = headers;
  }

  public List<BeaconBlockHeader> getHeaders() {
    return headers;
  }

  @Override
  public String toString() {
    return "BlockHeadersResponseMessage{" +
        "headers=" + headers +
        '}';
  }
}
