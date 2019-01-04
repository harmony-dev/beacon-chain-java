package org.ethereum.beacon.util.ssz.type;

import javafx.util.Pair;
import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import org.ethereum.beacon.util.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.util.ssz.SSZSerializer;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.ethereum.beacon.util.ssz.SSZSerializer.LENGTH_PREFIX_BYTE_SIZE;

public class Container implements SSZEncoderDecoder {

  private SSZSerializer sszSerializer;

  private static Set<String> supportedTypes = new HashSet<>();
  static {
    supportedTypes.add("container");
  }

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(Object.class);
  }

  @Override
  public Set<String> getSupportedTypes() {
    return supportedTypes;
  }

  @Override
  public Set<Class> getSupportedClassTypes() {
    return supportedClassTypes;
  }

  @Override
  public void encode(Object value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    byte[] data = sszSerializer.encode(value, field.type);

    if (field.skipContainer == null || field.skipContainer == false) {
      try {
        // Prepend data with its length
        result.write(SSZ.encodeInt32(data.length).toArrayUnsafe());
      } catch (IOException e) {
        throw new RuntimeException("Failed to write data length to stream", e);
      }
    }

    try {
      result.write(data);
    } catch (IOException e) {
      String error = String.format("Failed to write container from field \"%s\" to stream",
          field.name);
      throw new RuntimeException(error, e);
    }
  }

  @Override
  public void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    try {
        Bytes[] data = packContainerList(value, field);
        result.write(SSZ.encodeBytesList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream", field.name);
      throw new RuntimeException(error, ex);
    }
  }

  private Bytes[] packContainerList(List<Object> values, SSZSchemeBuilder.SSZScheme.SSZField field) {
    Bytes[] res = new Bytes[values.size()];
    for (int i = 0; i < values.size(); ++i) {
      byte[] data = sszSerializer.encode(values.get(i), field.type);
      Bytes curValue;
      if (field.skipContainer) {
        curValue = Bytes.of(data);
      } else {
        Bytes prefix = SSZ.encodeInt32(data.length);
        Bytes payload = Bytes.of(data);
        curValue = Bytes.concatenate(prefix, payload);
      }
      res[i] = curValue;
    }

    return res;
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return decodeImpl(field, reader).getKey();
  }

  public Pair<Object, Integer> decodeImpl(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    Bytes data = reader.readBytes();
    int dataSize = data.size();

    if (field.skipContainer != null && field.skipContainer) {
      Bytes lengthPrefix = SSZ.encodeUInt32(dataSize);
      byte[] container = Bytes.concatenate(lengthPrefix, data).toArrayUnsafe();
      return new Pair<>(sszSerializer.decode(container, field.type), dataSize);
    } else {
      return new Pair<>(sszSerializer.decode(data.toArrayUnsafe(), field.type),
          dataSize + LENGTH_PREFIX_BYTE_SIZE);
    }

  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    int remainingData = reader.readInt32();
    List<Object> res = new ArrayList<>();
    while (remainingData > 0) {
      Pair<Object, Integer> decodeRes = decodeImpl(field, reader);
      res.add(decodeRes.getKey());
      remainingData -= decodeRes.getValue();
    }
    return res;
  }

  @Override
  public void registerIn(SSZSerializer serializer) {
    this.sszSerializer = serializer;
    SSZEncoderDecoder.super.registerIn(serializer);
  }
}
