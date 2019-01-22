package org.ethereum.beacon.validator;

import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt64;

public interface ValidatorTaskManager {

  void start();

  void stop();

  void scheduleAtStart(UInt64 slot, UInt24 index, Runnable routine);

  void scheduleInTheMiddle(UInt64 slot, UInt24 index, Runnable routine);
}
