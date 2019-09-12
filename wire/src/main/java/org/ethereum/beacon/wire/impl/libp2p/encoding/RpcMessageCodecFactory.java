package org.ethereum.beacon.wire.impl.libp2p.encoding;

public interface RpcMessageCodecFactory {

  <TRequest, TResponse> RpcMessageCodec<TRequest, TResponse> create(Class<TRequest> reqClass,
      Class<TResponse> respClass);
}
