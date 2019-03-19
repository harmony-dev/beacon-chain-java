package org.ethereum.beacon.test.runner;

import java.util.Optional;

/** Test runner */
public interface Runner {
  /** Returns string error in case of any */
  Optional<String> run();
}
