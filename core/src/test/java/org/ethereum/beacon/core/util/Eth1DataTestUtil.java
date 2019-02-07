package org.ethereum.beacon.core.util;

import java.util.Random;
import org.ethereum.beacon.core.state.Eth1Data;
import tech.pegasys.artemis.ethereum.core.Hash32;

public abstract class Eth1DataTestUtil {
  private Eth1DataTestUtil() {}

  public static Eth1Data createRandom(Random random) {
    return new Eth1Data(Hash32.random(random), Hash32.random(random));
  }
}
