package org.ethereum.beacon.start.common.util;

import com.google.common.primitives.Bytes;
import org.ethereum.beacon.crypto.BLS381;
import org.ethereum.beacon.crypto.Hashes;
import org.ethereum.beacon.crypto.bls.bc.BCParameters;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Key pair generation utilities for simulation purposes.
 */
public class SimulationKeyPairGenerator {
  private static BigInteger CURVE_ORDER = BCParameters.ORDER;

  /**
   * Generate public/private key pairs according to mocked start spec.
   * @see <a
   *     href="https://github.com/ethereum/eth2.0-pm/tree/master/interop/mocked_start#pubkeyprivkey-generation">
   *     Pubkey/privkey generation</a>
   *
   * @param count - amount of key parirs to generate
   * @return the generated key pairs
   */
  public static List<BLS381.KeyPair> generateInteropKeys(int startIndex, int count) {
    List<BLS381.KeyPair> ret = new ArrayList<>();
    for (int i = startIndex; i < startIndex + count; i++) {
      byte[] res = BigInteger.valueOf(i).toByteArray();
      Bytes.reverse(res);
      BytesValue wrap = BytesValue.wrap(Bytes.ensureCapacity(res, 32, 0));
      byte[] res2 = Hashes.sha256(wrap).extractArray();
      Bytes.reverse(res2);

      BigInteger privKey = new BigInteger(1, res2).mod(CURVE_ORDER);
      ret.add(BLS381.KeyPair.create(BLS381.PrivateKey.create(privKey)));
    }
    return ret;
  }

  public static List<BLS381.KeyPair> generateRandomKeys(int seed, int start, int count) {
    Random random = new Random(seed);
    for (int i = 0; i < start; i++) {
      Bytes32.random(random);
    }
    List<BLS381.KeyPair> ret = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      ret.add(BLS381.KeyPair.create(BLS381.PrivateKey.create(Bytes32.random(random))));
    }
    return ret;
  }
}
