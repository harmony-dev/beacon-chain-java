package org.ethereum.beacon.wire.message.payload;

import org.ethereum.beacon.wire.message.RequestMessagePayload;
import org.ethereum.beacon.wire.message.ResponseMessagePayload;

public enum MessageType {

  Hello(HelloMessage.METHOD_ID, HelloMessage.class, null),
  Goodbye(GoodbyeMessage.METHOD_ID, GoodbyeMessage.class, null),
  BlockRoots(BlockRootsRequestMessage.METHOD_ID, BlockRootsRequestMessage.class, BlockRootsResponseMessage.class),
  BlockHeaders(BlockHeadersRequestMessage.METHOD_ID, BlockHeadersRequestMessage.class, BlockHeadersResponseMessage.class),
  BlockBodies(BlockBodiesRequestMessage.METHOD_ID, BlockBodiesRequestMessage.class, BlockBodiesResponseMessage.class);

  public static MessageType getById(int id) {
    for (MessageType message : MessageType.values()) {
      if (id == message.id) {
        return message;
      }
    }
    return null;
  }

  public static MessageType getByClass(Class<?> messageClass) {
    for (MessageType message : MessageType.values()) {
      if (message.requestClass.isAssignableFrom(messageClass)
          || (message.responseClass != null
              && message.responseClass.isAssignableFrom(messageClass))) {
        return message;
      }
    }
    return null;
  }

  private final int id;
  private final Class<? extends RequestMessagePayload> requestClass;
  private final Class<? extends ResponseMessagePayload> responseClass;

  MessageType(int id,
      Class<? extends RequestMessagePayload> requestClass,
      Class<? extends ResponseMessagePayload> responseClass) {
    this.id = id;
    this.requestClass = requestClass;
    this.responseClass = responseClass;
  }

  public int getId() {
    return id;
  }

  public Class<? extends RequestMessagePayload> getRequestClass() {
    return requestClass;
  }

  public Class<? extends ResponseMessagePayload> getResponseClass() {
    return responseClass;
  }

  public boolean isNotification() {
    return getResponseClass() == null;
  }
}
