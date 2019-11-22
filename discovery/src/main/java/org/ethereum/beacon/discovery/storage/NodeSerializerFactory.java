package org.ethereum.beacon.discovery.storage;

import org.ethereum.beacon.chain.storage.impl.SerializerFactory;
import org.ethereum.beacon.discovery.NodeRecordInfo;
import org.ethereum.beacon.discovery.enr.NodeRecordFactory;
import tech.pegasys.artemis.util.bytes.BytesValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Serializer for {@link NodeRecordInfo}, {@link NodeIndex} and {@link NodeBucket} */
public class NodeSerializerFactory implements SerializerFactory {
  private final Map<Class, Function<BytesValue, Object>> deserializerMap = new HashMap<>();
  private final Map<Class, Function<Object, BytesValue>> serializerMap = new HashMap<>();

  public NodeSerializerFactory(NodeRecordFactory nodeRecordFactory) {
    deserializerMap.put(
        NodeRecordInfo.class, bytes1 -> NodeRecordInfo.fromRlpBytes(bytes1, nodeRecordFactory));
    serializerMap.put(NodeRecordInfo.class, o -> ((NodeRecordInfo) o).toRlpBytes());
    deserializerMap.put(NodeIndex.class, NodeIndex::fromRlpBytes);
    serializerMap.put(NodeIndex.class, o -> ((NodeIndex) o).toRlpBytes());
    deserializerMap.put(
        NodeBucket.class, bytes -> NodeBucket.fromRlpBytes(bytes, nodeRecordFactory));
    serializerMap.put(NodeBucket.class, o -> ((NodeBucket) o).toRlpBytes());
  }

  @Override
  public <T> Function<BytesValue, T> getDeserializer(Class<? extends T> objectClass) {
    if (!deserializerMap.containsKey(objectClass)) {
      throw new RuntimeException(String.format("Type %s is not supported", objectClass));
    }
    return bytes -> (T) deserializerMap.get(objectClass).apply(bytes);
  }

  @Override
  public <T> Function<T, BytesValue> getSerializer(Class<? extends T> objectClass) {
    if (!serializerMap.containsKey(objectClass)) {
      throw new RuntimeException(String.format("Type %s is not supported", objectClass));
    }
    return value -> serializerMap.get(objectClass).apply(value);
  }
}
