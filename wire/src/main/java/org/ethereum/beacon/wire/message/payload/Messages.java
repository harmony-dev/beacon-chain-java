package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.wire.message.RequestMessagePayload;
import org.ethereum.beacon.wire.message.ResponseMessagePayload;
import tech.pegasys.artemis.util.uint.UInt64;

public enum Messages {

  Hello(HelloMessage.METHOD_ID, HelloMessage.class, null),
  Goodbye(GoodbyeMessage.METHOD_ID, GoodbyeMessage.class, null),
  BlockRoots(BlockRootsRequestMessage.METHOD_ID, BlockRootsRequestMessage.class, BlockRootsResponseMessage.class),
  BlockHeaders(BlockHeadersRequestMessage.METHOD_ID, BlockHeadersRequestMessage.class, BlockHeadersResponseMessage.class),
  BlockBodies(BlockBodiesRequestMessage.METHOD_ID, BlockBodiesRequestMessage.class, BlockBodiesResponseMessage.class);

  public static Messages getById(UInt64 id) {
    for (Messages message : Messages.values()) {
      if (id.equals(message.id)) {
        return message;
      }
    }
    return null;
  }

  private final UInt64 id;
  private final Class<? extends RequestMessagePayload> requestClass;
  private final Class<? extends ResponseMessagePayload> responseClass;

  Messages(UInt64 id,
      Class<? extends RequestMessagePayload> requestClass,
      Class<? extends ResponseMessagePayload> responseClass) {
    this.id = id;
    this.requestClass = requestClass;
    this.responseClass = responseClass;
  }

  public UInt64 getId() {
    return id;
  }

  public Class<? extends RequestMessagePayload> getRequestClass() {
    return requestClass;
  }

  public Class<? extends ResponseMessagePayload> getResponseClass() {
    return responseClass;
  }
}
