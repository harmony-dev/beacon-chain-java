package org.ethereum.beacon.emulator.config.main.action;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Configuration of task action defined by user */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ActionRun.class, name = "run"),
  @JsonSubTypes.Type(value = ActionEmulate.class, name = "emulate"),
  @JsonSubTypes.Type(value = ActionDeposit.class, name = "deposit")
})
public abstract class Action {}
