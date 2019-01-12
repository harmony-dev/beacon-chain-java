package org.ethereum.beacon.types.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt256;
import tech.pegasys.artemis.util.uint.UInt64;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * UInts codec. Supported:
 * {@link UInt24}, {@link UInt64}, {@link UInt256} ...
 */
public class SSZUInt implements SSZCodec {
  private static Map<Class, NumericType> classToNumericType = new HashMap<>();

  static {
    classToNumericType.put(UInt24.class, NumericType.of(Type.LONG, 24));
    classToNumericType.put(UInt64.class, NumericType.of(Type.LONG, 64));
    classToNumericType.put(UInt256.class, NumericType.of(Type.BIGINT, 256));
  }

  private static Set<String> supportedTypes = new HashSet<>();
  static {
  }

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(UInt24.class);
    supportedClassTypes.add(UInt64.class);
    supportedClassTypes.add(UInt256.class);
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
    NumericType numericType = parseFieldType(field);

    switch (numericType.size) {
      case 24: {
        UInt24 uValue = (UInt24) value;
        Bytes bytes = SSZ.encodeULong(uValue.getValue(), numericType.size);
        writeBytes(bytes, result);
        break;
      }
      case 64: {
        UInt64 uValue = (UInt64) value;
        Bytes bytes = SSZ.encodeULong(uValue.getValue(), numericType.size);
        writeBytes(bytes, result);
        break;
      }
      case 256: {
        UInt256 uValue = (UInt256) value;
        writeBytesValue(uValue.bytes(), result);
        break;
      }
      default: {
        throw new SSZException(String.format("Failed to write value \"%s\" to stream", value));
      }
    }
  }

  private static void writeBytes(Bytes value, OutputStream result)  {
    try {
      result.write(value.toArrayUnsafe());
    } catch (IOException e) {
      throw new SSZException(String.format("Failed to write value \"%s\" to stream", value), e);
    }
  }

  private static void writeBytesValue(BytesValue value, OutputStream result)  {
    try {
      result.write(value.getArrayUnsafe());
    } catch (IOException e) {
      throw new SSZException(String.format("Failed to write value \"%s\" to stream", value), e);
    }
  }

  @Override
  public void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    NumericType numericType = parseFieldType(field);

    try {
      switch (numericType.type) {
        case BIGINT: {
          encodeBigIntList(value, numericType, result);
          break;
        }
        case LONG: {
          encodeLongList(value, numericType, result);
          break;
        }
        default: {
          throwUnsupportedType(field);
        }
      }
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream",
          field.name);
      throw new SSZException(error, ex);
    }
  }

  private void encodeLongList(List<Object> value, NumericType type, OutputStream result) throws IOException {
    long[] data = new long[value.size()];
    for (int i = 0; i < value.size(); ++i) {
      switch (type.size) {
        case 24: {
          data[i] = ((UInt24) value.get(i)).getValue();
          break;
        }
        case 64: {
          data[i] = ((UInt64) value.get(i)).getValue();
          break;
        }
      }
    }
    result.write(SSZ.encodeULongIntList(type.size, data).toArrayUnsafe());
  }

  private void encodeBigIntList(List<Object> value, NumericType type, OutputStream result) throws IOException {
    Bytes[] data = new Bytes[value.size()];
    for (int i = 0; i < value.size(); ++i) {
      switch (type.size) {
        case 256: {
          data[i] = Bytes.of(((UInt256) value.get(i)).bytes().getArrayUnsafe());
          break;
        }
        default: {
          String error = String.format("Unsupported type \"%s\"", type);
          throw new SSZSchemeException(error);
        }
      }
    }
    result.write(SSZ.encodeBytesList(data).toArrayUnsafe());
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);
    switch (numericType.type) {
      case LONG: {
        return decodeLong(numericType, reader);
      }
      case BIGINT: {
        return decodeBigInt(numericType, reader);
      }
    }

    return throwUnsupportedType(field);
  }

  private Object decodeLong(NumericType type, BytesSSZReaderProxy reader) {
    // XXX: reader.readULong is buggy
    switch (type.size) {
      case 24: {
        return UInt24.valueOf(reader.readUnsignedBigInteger(type.size).intValue());
      }
      case 64: {
        return UInt64.valueOf(reader.readUnsignedBigInteger(type.size).longValue());
      }
    }
    String error = String.format("Unsupported type \"%s\"", type);
    throw new SSZSchemeException(error);
  }

  private Object decodeBigInt(NumericType type, BytesSSZReaderProxy reader) {
    switch (type.size) {
      case 256: {
        return UInt256.wrap(BytesValue.of(reader.readUnsignedBigInteger(type.size).toByteArray()));
      }
      default: {
        String error = String.format("Unsupported type \"%s\"", type);
        throw new SSZSchemeException(error);
      }
    }
  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);

    switch (numericType.type) {
      case LONG: {
        switch (numericType.size) {
          case 24: {
            return (List<Object>) (List<?>)  readUInt24List(numericType, reader);
          }
          case 64: {
            return (List<Object>) (List<?>)  readUInt64List(numericType, reader);
          }
        }
      }
      case BIGINT: {
        if (numericType.size == 256) {
          return (List<Object>) (List<?>)  readUInt256List(numericType, reader);
        }
      }
      default: {
        return throwUnsupportedListType(field);
      }
    }
  }

  private List<UInt24> readUInt24List(NumericType numericType, BytesSSZReaderProxy reader) {
    List<BigInteger> longList = reader.readUnsignedBigIntegerList(numericType.size);
    List<UInt24> res = longList.stream()
        .map(BigInteger::intValue)
        .map(UInt24::valueOf)
        .collect(Collectors.toList());

    return res;
  }

  private List<UInt64> readUInt64List(NumericType numericType, BytesSSZReaderProxy reader) {
    List<BigInteger> longList = reader.readUnsignedBigIntegerList(numericType.size);
    List<UInt64> res = longList.stream()
        .map(BigInteger::longValue)
        .map(UInt64::valueOf)
        .collect(Collectors.toList());

    return res;
  }

  private List<UInt256> readUInt256List(NumericType numericType, BytesSSZReaderProxy reader) {
    List<BigInteger> bigIntList = reader.readBigIntegerList(256);
    List<UInt256> res = bigIntList.stream()
        .map(UInt256::of)
        .collect(Collectors.toList());

    return res;
  }

  static class NumericType {
    Type type;
    int size;

    public NumericType() {
    }

    static NumericType of(Type type, int size) {
      NumericType res = new NumericType();
      res.type = type;
      res.size = size;
      return res;
    }
  }

  enum Type {
    LONG("long"),
    BIGINT("bigint");

    private String type;
    private static final Map<String, Type> ENUM_MAP;
    static {
      ENUM_MAP = Stream.of(Type.values()).collect(Collectors.toMap(e -> e.type, identity()));
    }

    Type(String type) {
      this.type = type;
    }

    static Type fromValue(String type) {
      return ENUM_MAP.get(type);
    }

    @Override
    public String toString() {
      return type;
    }
  }

  private NumericType parseFieldType(SSZSchemeBuilder.SSZScheme.SSZField field) {
    return classToNumericType.get(field.type);
  }
}
