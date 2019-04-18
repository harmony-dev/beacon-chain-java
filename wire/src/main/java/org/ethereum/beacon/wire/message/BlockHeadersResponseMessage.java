package org.ethereum.beacon.wire.message;

import java.util.List;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

@SSZSerializable
public class BlockHeadersResponseMessage extends ResponseMessagePayload<BlockHeadersRequestMessage> {

  @SSZ private final List<BeaconBlockHeader> headers;

  public BlockHeadersResponseMessage(
      BlockHeadersRequestMessage request,
      List<BeaconBlockHeader> headers) {
    super(request);
    this.headers = headers;
  }

  public List<BeaconBlockHeader> getHeaders() {
    return headers;
  }
}
