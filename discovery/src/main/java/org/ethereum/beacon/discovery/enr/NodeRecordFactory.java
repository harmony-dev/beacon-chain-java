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
      EnrScheme enrScheme,
      UInt64 seq,
      BytesValue signature,
      Pair<String, Object>... fieldKeyPairs) {
    return createFromValues(enrScheme, seq, signature, Arrays.asList(fieldKeyPairs));
  }

  public NodeRecord createFromValues(
      EnrScheme enrScheme,
      UInt64 seq,
      BytesValue signature,
      List<Pair<String, Object>> fieldKeyPairs) {

    EnrSchemeInterpreter enrSchemeInterpreter = interpreters.get(enrScheme);
    if (enrSchemeInterpreter == null) {
      throw new RuntimeException(
          String.format("No ethereum record interpreter found for identity scheme %s", enrScheme));
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
    RlpString id = (RlpString) values.get(2);
    if (!"id".equals(new String(id.getBytes()))) {
      throw new RuntimeException(
          String.format("Unable to deserialize ENR with no id field at 2-3 records, [%s]", values));
    }

    RlpString idVersion = (RlpString) values.get(3);
    EnrScheme nodeIdentity = EnrScheme.fromString(new String(idVersion.getBytes()));
    if (nodeIdentity == null) {
      throw new RuntimeException(
          String.format(
              "Unknown node identity scheme '%s', couldn't create node record.",
              idVersion.asString()));
    }

    EnrSchemeInterpreter enrSchemeInterpreter = interpreters.get(nodeIdentity);
    if (enrSchemeInterpreter == null) {
      throw new RuntimeException(
          String.format(
              "No ethereum record interpreter found for identity scheme %s", nodeIdentity));
    }

    return NodeRecord.fromRawFields(
        enrSchemeInterpreter,
        UInt64.fromBytesBigEndian(
            Bytes8.leftPad(BytesValue.wrap(((RlpString) values.get(1)).getBytes()))),
        BytesValue.wrap(((RlpString) values.get(0)).getBytes()),
        values.subList(4, values.size()));
  }

  public NodeRecord fromBytes(byte[] bytes) {
    // record    = [signature, seq, k, v, ...]
    RlpList rlpList = (RlpList) RlpDecoder.decode(bytes).getValues().get(0);
    return fromRlpList(rlpList);
  }
}
