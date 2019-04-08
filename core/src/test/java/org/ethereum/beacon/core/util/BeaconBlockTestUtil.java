package org.ethereum.beacon.core.util;

import java.util.Random;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes96;

public class BeaconBlockTestUtil {

  public static BeaconBlockHeader createRandomHeader(Random random) {
    return new BeaconBlockHeader(
        SlotNumber.ZERO,
        Hash32.random(random),
        Hash32.random(random),
        Hash32.random(random),
        BLSSignature.wrap(Bytes96.random(random))
    );
  }
}
