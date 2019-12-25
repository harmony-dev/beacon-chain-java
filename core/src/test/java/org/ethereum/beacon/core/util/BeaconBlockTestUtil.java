package org.ethereum.beacon.core.util;

import java.util.Collections;
import java.util.Random;
import org.ethereum.beacon.core.BeaconBlockBody;
import org.ethereum.beacon.core.BeaconBlockHeader;
import org.ethereum.beacon.core.spec.SpecConstants;
import org.ethereum.beacon.core.state.Eth1Data;
import org.ethereum.beacon.core.types.BLSSignature;
import org.ethereum.beacon.core.types.SlotNumber;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.uint.UInt64;

public class BeaconBlockTestUtil {

  public static BeaconBlockHeader createRandomHeader(Random random) {
    return new BeaconBlockHeader(
        SlotNumber.ZERO,
        Hash32.random(random),
        Hash32.random(random),
        Hash32.random(random));
  }

  public static BeaconBlockBody createRandomBodyWithNoOperations(
      Random random, SpecConstants constants) {
    return new BeaconBlockBody(
        BLSSignature.wrap(Bytes96.random(random)),
        new Eth1Data(
            Hash32.wrap(Bytes32.random(random)),
            UInt64.valueOf(random.nextInt()),
            Hash32.wrap(Bytes32.random(random))),
        Bytes32.random(random),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        constants);
  }
}
