package org.ethereum.beacon.emulator.config.main.conract;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmulatorContract.class, name = "emulator"),
    @JsonSubTypes.Type(value = EthereumJContract.class, name = "ethereumj"),
})
public abstract class Contract {}
