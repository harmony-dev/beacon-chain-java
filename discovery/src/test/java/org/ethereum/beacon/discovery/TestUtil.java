package org.ethereum.beacon.discovery;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.discovery.enr.EnrScheme;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.mock.EnrSchemeV4InterpreterMock;
import org.ethereum.beacon.discovery.storage.NodeSerializerFactory;
import org.ethereum.beacon.util.Utils;
import org.javatuples.Pair;
import org.web3j.crypto.ECKeyPair;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class TestUtil {
  public static final NodeRecordFactory NODE_RECORD_FACTORY_NO_VERIFICATION =
      new NodeRecordFactory(new EnrSchemeV4InterpreterMock()); // doesn't verify ECDSA signature
  public static final SerializerFactory TEST_SERIALIZER =
      new NodeSerializerFactory(NODE_RECORD_FACTORY_NO_VERIFICATION);
  static final int SEED = 123456789;

  /**
   * Generates node on 127.0.0.1 with provided port. Node key is random, but always the same for the
   * same port. Signature is not valid if verified.
   *
   * @return <code><private key, node record></code>
   */
  public static Pair<BytesValue, NodeRecord> generateNode(int port) {
    final Random rnd = new Random(SEED);
    Bytes4 localIp = null;
    try {
      localIp = Bytes4.wrap(InetAddress.getByName("127.0.0.1").getAddress());
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
    final Bytes4 finalLocalIp = localIp;
    for (int i = 0; i < port; ++i) {
      rnd.nextBoolean(); // skip according to input
    }
    byte[] privateKey = new byte[32];
    rnd.nextBytes(privateKey);
    ECKeyPair ecKeyPair = ECKeyPair.create(privateKey);
    final BytesValue pubKey =
        BytesValue.wrap(Utils.extractBytesFromUnsignedBigInt(ecKeyPair.getPublicKey()));
    NodeRecord nodeRecord =
        NODE_RECORD_FACTORY_NO_VERIFICATION.createFromValues(
            EnrScheme.V4,
            UInt64.valueOf(1),
            Bytes96.EMPTY,
            new ArrayList<Pair<String, Object>>() {
              {
                add(Pair.with(NodeRecord.FIELD_IP_V4, finalLocalIp));
                add(Pair.with(NodeRecord.FIELD_UDP_V4, port));
                add(Pair.with(NodeRecord.FIELD_PKEY_SECP256K1, pubKey));
              }
            });
    return Pair.with(BytesValue.wrap(privateKey), nodeRecord);
  }
}
