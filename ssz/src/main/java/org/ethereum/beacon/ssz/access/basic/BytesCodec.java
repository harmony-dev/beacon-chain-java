package org.ethereum.beacon.ssz.access.basic;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.visitor.SSZReader;
import org.ethereum.beacon.ssz.visitor.SSZWriter;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import tech.pegasys.artemis.ethereum.core.Address;
import tech.pegasys.artemis.util.bytes.Bytes1;
import tech.pegasys.artemis.util.bytes.Bytes32;
import tech.pegasys.artemis.util.bytes.Bytes4;
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

/**
 * SSZ Codec designed to work with fixed size bytes data classes, check list in {@link
 * #getSupportedClasses()}
 */
public class BytesCodec implements SSZBasicAccessor {

  private static Set<String> supportedTypes = new HashSet<>();

  private static Set<Class> supportedClassTypes = new HashSet<>();
  private static Map<Class, BytesType> classToByteType = new HashMap<>();

  static {
    supportedClassTypes.add(Bytes1.class);
    supportedClassTypes.add(Bytes4.class);
    supportedClassTypes.add(Bytes32.class);
    supportedClassTypes.add(Bytes48.class);
    supportedClassTypes.add(Bytes96.class);
    supportedClassTypes.add(Address.class);
  }

  static {
    classToByteType.put(Bytes1.class, BytesType.of(1));
    classToByteType.put(Bytes4.class, BytesType.of(4));
    classToByteType.put(Bytes32.class, BytesType.of(32));
    classToByteType.put(Bytes48.class, BytesType.of(48));
    classToByteType.put(Bytes96.class, BytesType.of(96));
    classToByteType.put(Address.class, BytesType.of(20));
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
  public Set<String> getSupportedSSZTypes() {
    return supportedTypes;
  }

  @Override
  public Set<Class> getSupportedClasses() {
    return supportedClassTypes;
  }

  @Override
  public int getSize(SSZField field) {
    Integer size = parseFieldType(field).size;
    return size == null ? -1 : size;
  }

  @Override
  public void encode(Object value, SSZField field, OutputStream result) {
    Bytes res = null;
    BytesValue data = (BytesValue) value;
    BytesType bytesType = parseFieldType(field);
    if (bytesType.size == null) {
      res = SSZWriter.encodeBytes(Bytes.of(data.getArrayUnsafe()));
    } else {
      res = SSZWriter.encodeBytes(Bytes.of(data.getArrayUnsafe()), bytesType.size);
    }

    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write data of type %s to stream",
          field.getRawClass());
      throw new SSZException(error, e);
    }
  }

  @Override
  public Object decode(SSZField field, SSZReader reader) {
    BytesType bytesType = parseFieldType(field);

    if (bytesType.size == null) {
      return BytesValue.wrap(reader.readBytes().toArrayUnsafe());
    }
    try {
      switch (bytesType.size) {
        case 1:
        {
          return Bytes1.wrap(reader.readHash(bytesType.size).toArrayUnsafe());
        }
        case 4:
        {
          return Bytes4.wrap(reader.readHash(bytesType.size).toArrayUnsafe());
        }
        case 20:
          {
            return Address.wrap(BytesValue.of(reader.readHash(bytesType.size).toArrayUnsafe()));
          }
        case 32:
        {
          return Bytes32.wrap(reader.readHash(bytesType.size).toArrayUnsafe());
        }
        case 48:
        {
          return Bytes48.wrap(reader.readHash(bytesType.size).toArrayUnsafe());
        }
        case 96:
          {
            return Bytes96.wrap(reader.readHash(bytesType.size).toArrayUnsafe());
          }
      }
    } catch (Exception ex) {
      String error = String.format("Failed to read data from stream to field \"%s\"",
          field.getName());
      throw new SSZException(error, ex);
    }

    return throwUnsupportedType(field);
  }

  private BytesType parseFieldType(SSZField field) {
    if (classToByteType.containsKey(field.getRawClass())) {
      return classToByteType.get(field.getRawClass());
    }

    throw new SSZSchemeException(String.format("Hash of class %s is not supported",
        field.getRawClass()));
  }

  static class BytesType {
    public static BytesType DYNAMIC = new BytesType(null);
    final Integer size;

    BytesType(Integer size) {
      this.size = size;
    }

    static BytesType of(Integer size) {
      return new BytesType(size);
    }
  }
}
