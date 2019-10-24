package org.ethereum.beacon.wire.impl.libp2p.encoding;

import io.netty.buffer.ByteBuf;
import org.ethereum.beacon.ssz.SSZSerializer;
import org.ethereum.beacon.wire.exceptions.WireRpcMalformedException;
import org.ethereum.beacon.wire.exceptions.WireRpcRemoteError;
import org.ethereum.beacon.wire.message.ErrorCode;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class SSZMessageCodec<TRequest, TResponse> implements RpcMessageCodec<TRequest, TResponse> {

  public static RpcMessageCodecFactory createFactory(SSZSerializer sszSerializer) {
    return new RpcMessageCodecFactory() {
      @Override
      public <TRequest, TResponse> RpcMessageCodec<TRequest, TResponse> create(
          Class<TRequest> reqClass,
          Class<TResponse> respClass) {
        return new SSZMessageCodec<>(sszSerializer, reqClass, respClass);
      }
    };
  }

  private final SSZSerializer sszSerializer;
  private final Class<TRequest> requestClass;
  private final Class<TResponse> responseClass;

  public SSZMessageCodec(SSZSerializer sszSerializer, Class<TRequest> requestClass,
      Class<TResponse> responseClass) {
    this.sszSerializer = sszSerializer;
    this.requestClass = requestClass;
    this.responseClass = responseClass;
  }

  @Override
  public MessageCodec<TRequest> getRequestMessageCodec() {
    return new Request();
  }

  @Override
  public MessageCodec<Pair<TResponse, Throwable>> getResponseMessageCodec() {
    return new Response();
  }

  private void serializeMsg(Object msg, ByteBuf buf) {
    byte[] msgBytes = sszSerializer.encode(msg);
    Util.writeRawVarint32(buf, msgBytes.length);
    buf.writeBytes(msgBytes);
  }

  private <TMessage> TMessage deserializeMsg(ByteBuf buf, Class<TMessage> clazz) {
    int msgSize = Util.readRawVarint32(buf);
    if (msgSize != buf.readableBytes()) {
      throw new WireRpcMalformedException("Size in header (" + msgSize + ") doesn't match payload size: " + buf.readableBytes());
    }
    return sszSerializer.decode(BytesValue.wrapBuffer(buf), clazz);
  }

  class Request implements MessageCodec<TRequest> {

    @Override
    public void serialize(TRequest msg, ByteBuf buf) {
      serializeMsg(msg, buf);
    }

    @Override
    public TRequest deserialize(ByteBuf buf) {
      return deserializeMsg(buf, requestClass);
    }
  }

  class Response implements MessageCodec<Pair<TResponse, Throwable>> {

    @Override
    public void serialize(Pair<TResponse, Throwable> msg, ByteBuf buf) {
      if (msg.getValue1() != null) {
        buf.writeByte(ErrorCode.ServerError.getCode());
        byte[] errMsgBytes = msg.getValue1().getMessage().getBytes();
        Util.writeRawVarint32(buf, errMsgBytes.length);
        buf.writeBytes(errMsgBytes);
      } else {
        buf.writeByte(ErrorCode.OK.getCode());
        serializeMsg(msg.getValue0(), buf);
      }
    }

    @Override
    public Pair<TResponse, Throwable> deserialize(ByteBuf buf) {
      byte errCode = buf.readByte();
      ErrorCode error = ErrorCode.fromCode(errCode);
      TResponse message = null;
      Throwable err = null;
      if (error == ErrorCode.OK) {
        message = deserializeMsg(buf, responseClass);
      } else {
        int msgSize = Util.readRawVarint32(buf);
        if (msgSize != buf.readableBytes()) {
          throw new WireRpcMalformedException("Size in header (" + msgSize + ") doesn't match payload size: " + buf.readableBytes());
        }
        byte[] msgBytes = new byte[buf.readableBytes()];
        buf.readBytes(msgBytes);
        String errMsg = new String(msgBytes);
        err = new WireRpcRemoteError("Error: " + err + ": " + errMsg);
      }
      return Pair.with(message, err);
    }
  }

}
