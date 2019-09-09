package org.ethereum.beacon.wire.impl.plain.channel.beacon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.exceptions.WireRpcRemoteError;
import org.ethereum.beacon.wire.impl.plain.channel.Channel;
import org.ethereum.beacon.wire.impl.plain.channel.ChannelCodec;
import org.ethereum.beacon.wire.impl.plain.channel.RpcMessage;
import org.ethereum.beacon.wire.message.RequestMessage;
import org.ethereum.beacon.wire.message.RequestMessagePayload;
import org.ethereum.beacon.wire.message.ResponseMessage;
import org.ethereum.beacon.wire.message.ResponseMessagePayload;
import org.ethereum.beacon.wire.message.payload.MessageType;
import tech.pegasys.artemis.util.bytes.BytesValue;

class BeaconPayloadCodec extends ChannelCodec<
      RpcMessage<RequestMessage, ResponseMessage>,
      RpcMessage<RequestMessagePayload, ResponseMessagePayload>> {

  private static final Logger logger = LogManager.getLogger(BeaconPayloadCodec.class);

  private static final Object CONTEXT_REQUEST_MESSAGE_ID = new Object();

  public BeaconPayloadCodec(
      Channel<RpcMessage<RequestMessage, ResponseMessage>> inChannel,
      SSZSerializer sszSerializer) {

    super(inChannel, msg -> decode(sszSerializer, msg), msg -> encode(sszSerializer, msg));
  }

  static RpcMessage<RequestMessagePayload, ResponseMessagePayload> decode(
      SSZSerializer sszSerializer, RpcMessage<RequestMessage, ResponseMessage> msg) {

    if (msg.isRequest()) {
      MessageType messageType = MessageType.getById(msg.getRequest().getMethodId());
      RequestMessagePayload messagePayload = sszSerializer
          .decode(msg.getRequest().getBody(), messageType.getRequestClass());
      return msg.copyWithRequest(messagePayload);
    } else {
      int methodId = (int) msg.getRequestContext(CONTEXT_REQUEST_MESSAGE_ID);
      if (msg.getResponse().get().getResponseCode() == 0) {
        ResponseMessagePayload messagePayload =
            sszSerializer.decode(
                msg.getResponse().get().getResult(),
                MessageType.getById(methodId).getResponseClass());
        return msg.copyWithResponse(messagePayload);
      } else {
        return msg.copyWithResponseError(deserializeError(sszSerializer,
                msg.getResponse().get().getResponseCode(), msg.getResponse().get().getResult()));
      }
    }
  }

  static  RpcMessage<RequestMessage, ResponseMessage> encode(
      SSZSerializer sszSerializer, RpcMessage<RequestMessagePayload, ResponseMessagePayload> msg) {
    if (msg.isRequest()) {
      int methodId = msg.getRequest().getMethodId();
      msg.setRequestContext(CONTEXT_REQUEST_MESSAGE_ID, methodId);
      BytesValue payloadBytes = sszSerializer.encode2(msg.getRequest());
      return msg.copyWithRequest(new RequestMessage(methodId, payloadBytes));
    } else {
      if (msg.getError().isPresent()) {
        return msg.copyWithResponse(serializeError(sszSerializer, msg.getError().get()));
      } else {
        BytesValue payloadBytes = sszSerializer.encode2(msg.getResponse().get());
        return msg.copyWithResponse(new ResponseMessage(0, payloadBytes));
      }
    }
  }

  protected static ResponseMessage serializeError(SSZSerializer sszSerializer, Throwable t) {
    return new ResponseMessage(0xFF, BytesValue.EMPTY);
  }

  protected static Throwable deserializeError(SSZSerializer sszSerializer, int respCode, BytesValue data) {
    return new WireRpcRemoteError("Remote peer call error: code = " + respCode + ", payload: " + data);
  }
}
