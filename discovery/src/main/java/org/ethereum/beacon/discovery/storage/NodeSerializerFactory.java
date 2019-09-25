package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.discovery.enr.NodeRecordInfo;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.function.Function;

public class NodeSerializerFactory implements SerializerFactory {
  @Override
  public <T> Function<BytesValue, T> getDeserializer(Class<? extends T> objectClass) {
    if ((!objectClass.equals(NodeRecordInfo.class)) && (!objectClass.equals(NodeIndex.class))) {
      throw new RuntimeException(String.format("Type %s is not supported", objectClass));
    }
    return bytes ->
        objectClass.equals(NodeRecordInfo.class)
            ? (T) NodeRecordInfo.fromRlpBytes(bytes)
            : (T) NodeIndex.fromRlpBytes(bytes);
  }

  @Override
  public <T> Function<T, BytesValue> getSerializer(Class<? extends T> objectClass) {
    if ((!objectClass.equals(NodeRecordInfo.class)) && (!objectClass.equals(NodeIndex.class))) {
      throw new RuntimeException(String.format("Type %s is not supported", objectClass));
    }
    return value ->
        objectClass.equals(NodeRecordInfo.class)
            ? ((NodeRecordInfo) value).toRlpBytes()
            : ((NodeIndex) value).toRlpBytes();
  }
}
