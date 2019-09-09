package org.ethereum.beacon.emulator.config.main.network;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = NettyNetwork.class, name = "netty"),
    @JsonSubTypes.Type(value = Libp2pNetwork.class, name = "libp2p"),
})
public abstract class Network {}
