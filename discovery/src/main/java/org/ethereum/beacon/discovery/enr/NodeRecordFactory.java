package org.ethereum.beacon.discovery.enr;

import org.javatuples.Pair;
import org.web3j.rlp.RlpDecoder;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpType;
import tech.pegasys.artemis.util.bytes.Bytes8;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt64;

import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ethereum.beacon.discovery.enr.NodeRecord.FIELD_ID;

public class NodeRecordFactory {
  public static final NodeRecordFactory DEFAULT =
      new NodeRecordFactory(new EnrSchemeV4Interpreter());
  Map<EnrScheme, EnrSchemeInterpreter> interpreters = new HashMap<>();

  public NodeRecordFactory(EnrSchemeInterpreter... enrSchemeInterpreters) {
    for (EnrSchemeInterpreter enrSchemeInterpreter : enrSchemeInterpreters) {
      interpreters.put(enrSchemeInterpreter.getScheme(), enrSchemeInterpreter);
    }
  }

  @SafeVarargs
  public final NodeRecord createFromValues(
      UInt64 seq, BytesValue signature, Pair<String, Object>... fieldKeyPairs) {
    return createFromValues(seq, signature, Arrays.asList(fieldKeyPairs));
  }

  public NodeRecord createFromValues(
      UInt64 seq, BytesValue signature, List<Pair<String, Object>> fieldKeyPairs) {
    Pair<String, Object> schemePair = null;
    for (Pair<String, Object> pair : fieldKeyPairs) {
      if (FIELD_ID.equals(pair.getValue0())) {
        schemePair = pair;
        break;
      }
    }
    if (schemePair == null) {
      throw new RuntimeException("ENR scheme is not defined in key-value pairs");
    }

    EnrSchemeInterpreter enrSchemeInterpreter = interpreters.get(schemePair.getValue1());
    if (enrSchemeInterpreter == null) {
      throw new RuntimeException(
          String.format(
              "No ethereum record interpreter found for identity scheme %s",
              schemePair.getValue1()));
    }

    return NodeRecord.fromValues(enrSchemeInterpreter, seq, signature, fieldKeyPairs);
  }

  public NodeRecord fromBase64(String enrBase64) {
    return fromBytes(Base64.getUrlDecoder().decode(enrBase64));
  }

  public NodeRecord fromBytes(BytesValue bytes) {
    return fromBytes(bytes.extractArray());
  }

  public NodeRecord fromRlpList(RlpList rlpList) {
    List<RlpType> values = rlpList.getValues();
    if (values.size() < 4) {
      throw new RuntimeException(
          String.format("Unable to deserialize ENR with less than 4 fields, [%s]", values));
    }

    // TODO: repair as id is not first now
    EnrScheme nodeIdentity = null;
    boolean idFound = false;
    for (int i = 2; i < values.size(); i += 2) {
      RlpString id = (RlpString) values.get(i);
      if (!"id".equals(new String(id.getBytes()))) {
        continue;
      }

      RlpString idVersion = (RlpString) values.get(i + 1);
      nodeIdentity = EnrScheme.fromString(new String(idVersion.getBytes()));
      if (nodeIdentity == null) {  // no interpreter for such id
        throw new RuntimeException(
            String.format(
                "Unknown node identity scheme '%s', couldn't create node record.",
                idVersion.asString()));
      }
      idFound = true;
      break;
    }
    if (!idFound) { // no `id` key-values
      throw new RuntimeException("Unknown node identity scheme, not defined in record ");
    }

    EnrSchemeInterpreter enrSchemeInterpreter = interpreters.get(nodeIdentity);
    if (enrSchemeInterpreter == null) {
      throw new RuntimeException(
          String.format(
              "No Ethereum record interpreter found for identity scheme %s", nodeIdentity));
    }

    return NodeRecord.fromRawFields(
        enrSchemeInterpreter,
        UInt64.fromBytesBigEndian(
            Bytes8.leftPad(BytesValue.wrap(((RlpString) values.get(1)).getBytes()))),
        BytesValue.wrap(((RlpString) values.get(0)).getBytes()),
        values.subList(2, values.size()));
  }

  public NodeRecord fromBytes(byte[] bytes) {
    // record    = [signature, seq, k, v, ...]
    RlpList rlpList = (RlpList) RlpDecoder.decode(bytes).getValues().get(0);
    return fromRlpList(rlpList);
  }
}
