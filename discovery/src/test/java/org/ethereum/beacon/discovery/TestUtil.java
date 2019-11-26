package org.ethereum.beacon.discovery;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.discovery.enr.EnrField;
import org.ethereum.beacon.discovery.enr.EnrFieldV4;
import org.ethereum.beacon.discovery.enr.IdentitySchema;
import org.ethereum.beacon.discovery.enr.IdentitySchemaV4Interpreter;
import org.ethereum.beacon.discovery.enr.NodeRecord;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import org.ethereum.beacon.discovery.mock.IdentitySchemaV4InterpreterMock;
import org.ethereum.beacon.discovery.storage.NodeSerializerFactory;
import org.javatuples.Pair;
import tech.pegasys.artemis.util.bytes.Bytes4;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

public class TestUtil {
  public static final NodeRecordFactory NODE_RECORD_FACTORY =
      new NodeRecordFactory(new IdentitySchemaV4Interpreter());
  public static final NodeRecordFactory NODE_RECORD_FACTORY_NO_VERIFICATION =
      new NodeRecordFactory(
          new IdentitySchemaV4InterpreterMock()); // doesn't verify ECDSA signature
  public static final SerializerFactory TEST_SERIALIZER =
      new NodeSerializerFactory(NODE_RECORD_FACTORY_NO_VERIFICATION);
  public static final String LOCALHOST = "127.0.0.1";
  static final int SEED = 123456789;

  /**
   * Generates node on 127.0.0.1 with provided port. Node key is random, but always the same for the
   * same port. Signature is not valid if verified.
   *
   * @return <code><private key, node record></code>
   */
  public static Pair<BytesValue, NodeRecord> generateUnverifiedNode(int port) {
    return generateNode(port, false);
  }

  /** Parses string representation of IPv4. Say, `127.0.0.1` */
  public static Bytes4 parseStringIP(String ip) {
    try {
      return Bytes4.wrap(InetAddress.getByName(ip).getAddress());
    } catch (UnknownHostException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * Generates node on 127.0.0.1 with provided port. Node key is random, but always the same for the
   * same port.
   *
   * @param port listen port
   * @param verification whether to use valid signature
   * @return <code><private key, node record></code>
   */
  public static Pair<BytesValue, NodeRecord> generateNode(int port, boolean verification) {
    final Random rnd = new Random(SEED);
    final Bytes4 finalLocalIp = parseStringIP(LOCALHOST);
    for (int i = 0; i < port; ++i) {
      rnd.nextBoolean(); // skip according to input
    }
    byte[] privateKey = new byte[32];
    rnd.nextBytes(privateKey);

    NodeRecordFactory nodeRecordFactory =
        verification ? NODE_RECORD_FACTORY : NODE_RECORD_FACTORY_NO_VERIFICATION;
    NodeRecord nodeRecord =
        nodeRecordFactory.createFromValues(
            UInt64.valueOf(1),
            new ArrayList<Pair<String, Object>>() {
              {
                add(Pair.with(EnrField.ID, IdentitySchema.V4));
                add(Pair.with(EnrField.IP_V4, finalLocalIp));
                add(Pair.with(EnrField.UDP_V4, port));
                add(
                    Pair.with(
                        EnrFieldV4.PKEY_SECP256K1,
                        Functions.derivePublicKeyFromPrivate(BytesValue.wrap(privateKey))));
              }
            });

    nodeRecord.sign(BytesValue.wrap(privateKey));
    return Pair.with(BytesValue.wrap(privateKey), nodeRecord);
  }
}
