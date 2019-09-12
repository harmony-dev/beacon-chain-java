package org.ethereum.beacon.wire.impl.libp2p.encoding;

import org.javatuples.Pair;

public interface RpcMessageCodec<TRequest, TResponse> {

  MessageCodec<TRequest> getRequestMessageCodec();

  MessageCodec<Pair<TResponse, Throwable>> getResponseMessageCodec();
}
