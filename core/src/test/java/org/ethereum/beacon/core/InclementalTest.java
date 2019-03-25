package org.ethereum.beacon.core;

import org.ethereum.beacon.core.Incremental.ContainerUpdateTracker;
import org.ethereum.beacon.core.types.Gwei;
import org.ethereum.beacon.core.types.ValidatorIndex;
import org.ethereum.beacon.ssz.annotation.SSZ;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import tech.pegasys.artemis.util.collections.WriteList;
import tech.pegasys.artemis.util.collections.WriteVector;
import tech.pegasys.artemis.util.uint.UInt64;

public class InclementalTest {

  @SSZSerializable
  static class Container1 implements Incremental<ContainerUpdateTracker>{
    private @SSZ UInt64 uint1;
    private @SSZ Container2 container;
    private @SSZ WriteVector<ValidatorIndex, Gwei> balances;
    private @SSZ WriteList<ValidatorIndex, ValidatorStruct> validators;

    private ContainerUpdateTracker updateTracker;

    @Override
    public void installUpdateTracker(ContainerUpdateTracker updateTracker) {
      this.updateTracker = updateTracker;
    }

    public void setUint1(UInt64 uint1) {
      this.uint1 = uint1;
      updateTracker.elementUpdated("uint1");
    }

    public void setContainer(Container2 container) {
      this.container = container;
      updateTracker.elementUpdated("container");
    }

    public UInt64 getUint1() {
      return uint1;
    }

    public Container2 getContainer() {
      return container;
    }

    public WriteVector<ValidatorIndex, Gwei> getBalances() {
      return balances;
    }

    public WriteList<ValidatorIndex, ValidatorStruct> getValidators() {
      return validators;
    }
  }

  @SSZSerializable
  static class ValidatorStruct {
    @SSZ UInt64 uint20;
    @SSZ boolean boo21;
  }

  @SSZSerializable
  static class Container2 {
    @SSZ UInt64 uint10;
    @SSZ boolean bool1;
  }
}
