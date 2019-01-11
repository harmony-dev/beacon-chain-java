package org.ethereum.beacon.types.ssz;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.type.SSZCodec;
import tech.pegasys.artemis.util.uint.UInt24;
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
 * FIXME: INT and BIGINTs are unused in current implementation
 */
public class SSZUInt implements SSZCodec {
  private static Map<Class, NumericType> classToNumericType = new HashMap<>();

  static {
    classToNumericType.put(UInt64.class, NumericType.of(Type.LONG, 64));
    classToNumericType.put(UInt24.class, NumericType.of(Type.LONG, 24));
  }

  private static Set<String> supportedTypes = new HashSet<>();
  static {
  }

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(UInt64.class);
    supportedClassTypes.add(UInt24.class);
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

    switch (numericType.type) {
      case INT: {
        encodeInt(value, numericType, result);
        break;
      }
      case LONG: {
        encodeLong(value, numericType, result);
        break;
      }
      case BIGINT: {
        encodeBigInt(value, numericType, result);
        break;
      }
      default: {
        throwUnsupportedType(field);
      }
    }
  }

  private static void encodeInt(Object value, NumericType type, OutputStream result)  {
    encodeLong((int) value, type.size, result);
  }

  private void encodeLong(Object value, NumericType type, OutputStream result) {
    switch (type.size) {
      case 24: {
        UInt24 uValue = (UInt24) value;
        encodeLong(uValue.getValue(), type.size, result);
        break;
      }
      case 64: {
        UInt64 uValue = (UInt64) value;
        encodeLong(uValue.getValue(), type.size, result);
        break;
      }
      default: {
        throw new SSZException(String.format("Failed to write value \"%s\" to stream", value));
      }
    }
  }

  private static void encodeLong(long value, int bitLength, OutputStream result) {
    Bytes res = SSZ.encodeULong(value, bitLength);
    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      throw new SSZException(String.format("Failed to write value \"%s\" to stream", value), e);
    }
  }

  private static void encodeBigInt(Object value, NumericType type, OutputStream result)  {
    BigInteger valueBI = (BigInteger) value;
    Bytes res = SSZ.encodeBigInteger(valueBI, type.size);

    try {
      result.write(res.toArrayUnsafe());
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
        case INT: {
          encodeIntList(value, numericType, result);
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

  private void encodeIntList(List<Object> value, NumericType type, OutputStream result) throws IOException {
    int[] data = new int[value.size()];
    for (int i = 0; i < value.size(); ++i) {
      data[i] = (int) value.get(i);
    }
    result.write(SSZ.encodeUIntList(type.size, data).toArrayUnsafe());
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
    BigInteger[] data = (BigInteger[]) value.toArray(new BigInteger[0]);
    result.write(SSZ.encodeBigIntegerList(type.size, data).toArrayUnsafe());
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);
    switch (numericType.type) {
      case INT: {
        return decodeInt(numericType, reader);
      }
      case LONG: {
        return decodeLong(numericType, reader);
      }
      case BIGINT: {
        return decodeBigInt(numericType, reader);
      }
    }

    return throwUnsupportedType(field);
  }

  private Object decodeInt(NumericType type, BytesSSZReaderProxy reader) {
    return reader.readUInt(type.size);
  }

  private Object decodeLong(NumericType type, BytesSSZReaderProxy reader) {
    // reader.readULong is buggy
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
    return reader.readUnsignedBigInteger(type.size);
  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);

    switch (numericType.type) {
      case INT: {
        return (List<Object>) (List<?>) reader.readUIntList(numericType.size);
      }
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
        return (List<Object>) (List<?>)  reader.readUnsignedBigIntegerList(numericType.size);
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
    INT("int"),
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
