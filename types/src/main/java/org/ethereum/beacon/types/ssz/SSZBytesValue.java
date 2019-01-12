package org.ethereum.beacon.types.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes48;
import tech.pegasys.artemis.util.bytes.Bytes96;
import tech.pegasys.artemis.util.bytes.BytesValue;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SSZBytesValue implements SSZCodec {

  private static Set<String> supportedTypes = new HashSet<>();

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(Bytes48.class);
    supportedClassTypes.add(Bytes96.class);
    supportedClassTypes.add(BytesValue.class);
    supportedClassTypes.add(Bytes1.class);
    supportedClassTypes.add(Address.class);
  }
  private static Map<Class, BytesType> classToByteType = new HashMap<>();

  static {
    classToByteType.put(Bytes48.class, BytesType.of(48));
    classToByteType.put(Bytes96.class, BytesType.of(96));
    classToByteType.put(BytesValue.class, new BytesType());
    classToByteType.put(Bytes1.class, BytesType.of(1));
    classToByteType.put(Address.class, BytesType.of(20));
  }

  @Override
  public Set<String> getSupportedSSZTypes() {
    return supportedTypes;
  }

  @Override
  public Set<Class> getSupportedClasses() {
    return supportedClassTypes;
  }

  @Override
  public void encode(Object value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    Bytes res = null;
    BytesValue data = (BytesValue) value;
    res = SSZ.encodeBytes(Bytes.of(data.getArrayUnsafe()));

    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write data of type %s to stream", field.type);
      throw new SSZException(error, e);
    }
  }

  @Override
  public void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    Bytes[] data = repackBytesList((List<BytesValue>) (List<?>) value);

    try {
       result.write(SSZ.encodeBytesList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream",
          field.name);
      throw new SSZException(error, ex);
    }
  }

  private static Bytes[] repackBytesList(List<BytesValue> list) {
    Bytes[] data = new Bytes[list.size()];
    for (int i = 0; i < list.size(); i++) {
      byte[] el = list.get(i).getArrayUnsafe();
      data[i] = Bytes.of(el);
    }

    return data;
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    BytesType bytesType = parseFieldType(field);

    if (bytesType.size == null) {
      return BytesValue.wrap(reader.readBytes().toArrayUnsafe());
    }
    try {
      switch (bytesType.size) {
        case 1: {
          return Bytes1.wrap(reader.readBytes(bytesType.size).toArrayUnsafe());
        }
        case 20: {
          return Address.wrap(BytesValue.of(reader.readBytes(bytesType.size).toArrayUnsafe()));
        }
        case 48: {
          return Bytes48.wrap(reader.readBytes(bytesType.size).toArrayUnsafe());
        }
        case 96: {
          return Bytes96.wrap(reader.readBytes(bytesType.size).toArrayUnsafe());
        }
      }
    } catch (Exception ex) {
      String error = String.format("Failed to read data from stream to field \"%s\"",
          field.name);
      throw new SSZException(error, ex);
    }

    return throwUnsupportedType(field);
  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    BytesType bytesType = parseFieldType(field);

    List<Bytes> bytesList = reader.readHashList(bytesType.size);
    if (bytesType.size == null) {
      return bytesList.stream()
          .map(Bytes::toArrayUnsafe)
          .map(BytesValue::wrap)
          .collect(Collectors.toList());
    }
    List<BytesValue> res = null;
    try {
      switch (bytesType.size) {
        case 1: {
          res = bytesList.stream()
              .map(Bytes::toArrayUnsafe)
              .map(Bytes1::wrap)
              .collect(Collectors.toList());
          break;
        }
        case 20: {
          res = bytesList.stream()
              .map(Bytes::toArrayUnsafe)
              .map(BytesValue::wrap)
              .map(Address::wrap)
              .collect(Collectors.toList());
          break;
        }
        case 48: {
          res = bytesList.stream()
              .map(Bytes::toArrayUnsafe)
              .map(Bytes48::wrap)
              .collect(Collectors.toList());
          break;
        }
        case 96: {
          res = bytesList.stream()
              .map(Bytes::toArrayUnsafe)
              .map(Bytes96::wrap)
              .collect(Collectors.toList());
          break;
        }
      }
    } catch (Exception ex) {
      String error = String.format("Failed to read list data from stream to field \"%s\"",
          field.name);
      throw new SSZException(error, ex);
    }

    return (List<Object>) (List<?>)  res;
  }

  static class BytesType {
    Integer size = null;

    public BytesType() {
    }

    static BytesType of(Integer size) {
      BytesType res = new BytesType();
      res.size = size;
      return res;
    }
  }

  private BytesType parseFieldType(SSZSchemeBuilder.SSZScheme.SSZField field) {
    if (classToByteType.containsKey(field.type)) {
      return classToByteType.get(field.type);
    }

    throw new SSZSchemeException(String.format("Hash of class %s is not supported", field.type));
  }
}
