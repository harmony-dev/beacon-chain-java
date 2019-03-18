package org.ethereum.beacon.emulator.config.main.plan;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

/** Configuration of application tasks */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
    @JsonSubTypes.Type(value = GeneralPlan.class, name = "general"),
    @JsonSubTypes.Type(value = SimulationPlan.class, name = "simulation"),
})
public abstract class Plan {
}
