package org.ethereum.beacon.ssz.access.basic;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
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
import org.ethereum.beacon.ssz.access.SSZCodec;

import static java.util.function.Function.identity;

/**
 * {@link SSZCodec} for all primitive Java integers and their default wrappers
 *
 * <p>All numerics are considered unsigned, bit size could be clarified by {@link
 * SSZField#extraSize}
 */
public class UIntPrimitive implements SSZCodec {
  private static final int DEFAULT_BYTE_SIZE = 8;
  private static final int DEFAULT_SHORT_SIZE = 16;
  private static final int DEFAULT_INT_SIZE = 32;
  private static final int DEFAULT_LONG_SIZE = 64;
  private static final int DEFAULT_BIGINT_SIZE = 512;

  private static Map<Class, NumericType> classToNumericType = new HashMap<>();
  private static Set<String> supportedTypes = new HashSet<>();
  private static Set<Class> supportedClassTypes = new HashSet<>();

  static {
    classToNumericType.put(byte.class, NumericType.of(Type.INT, DEFAULT_BYTE_SIZE));
    classToNumericType.put(Byte.class, NumericType.of(Type.INT, DEFAULT_BYTE_SIZE));
    classToNumericType.put(int.class, NumericType.of(Type.INT, DEFAULT_INT_SIZE));
    classToNumericType.put(Integer.class, NumericType.of(Type.INT, DEFAULT_INT_SIZE));
    classToNumericType.put(short.class, NumericType.of(Type.INT, DEFAULT_SHORT_SIZE));
    classToNumericType.put(Short.class, NumericType.of(Type.INT, DEFAULT_SHORT_SIZE));
    classToNumericType.put(long.class, NumericType.of(Type.LONG, DEFAULT_LONG_SIZE));
    classToNumericType.put(Long.class, NumericType.of(Type.LONG, DEFAULT_LONG_SIZE));
    classToNumericType.put(BigInteger.class, NumericType.of(Type.BIGINT, DEFAULT_BIGINT_SIZE));
  }

  static {
    supportedTypes.add("uint");
  }

  static {
    supportedClassTypes.add(byte.class);
    supportedClassTypes.add(Byte.class);
    supportedClassTypes.add(int.class);
    supportedClassTypes.add(Integer.class);
    supportedClassTypes.add(short.class);
    supportedClassTypes.add(Short.class);
    supportedClassTypes.add(long.class);
    supportedClassTypes.add(Long.class);
    supportedClassTypes.add(BigInteger.class);
  }

  private static void encodeInt(Object value, NumericType type, OutputStream result) {
    encodeLong(((Number) value).intValue() & type.mask, type.size, result);
  }

  private static void encodeLong(Object value, NumericType type, OutputStream result) {
    encodeLong((long) value, type.size, result);
  }

  private static void encodeLong(long value, int bitLength, OutputStream result) {
    Bytes res = SSZ.encodeULong(value, bitLength);
    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      throw new SSZException(String.format("Failed to write value \"%s\" to stream", value), e);
    }
  }

  private static void encodeBigInt(Object value, NumericType type, OutputStream result) {
    BigInteger valueBI = (BigInteger) value;
    Bytes res = SSZ.encodeBigInteger(valueBI, type.size);

    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      throw new SSZException(String.format("Failed to write value \"%s\" to stream", value), e);
    }
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
    return parseFieldType(field).size / 8;
  }

  @Override
  public void encode(Object value, SSZField field, OutputStream result) {
    NumericType numericType = parseFieldType(field);

    switch (numericType.type) {
      case INT:
        {
          encodeInt(value, numericType, result);
          break;
        }
      case LONG:
        {
          encodeLong(value, numericType, result);
          break;
        }
      case BIGINT:
        {
          encodeBigInt(value, numericType, result);
          break;
        }
      default:
        {
          throwUnsupportedType(field);
        }
    }
  }

  @Override
  public void encodeList(
      List<Object> value, SSZField field, OutputStream result) {
    NumericType numericType = parseFieldType(field);

    try {
      switch (numericType.type) {
        case BIGINT:
          {
            encodeBigIntList(value, numericType, result);
            break;
          }
        case INT:
          {
            encodeIntList(value, numericType, result);
            break;
          }
        case LONG:
          {
            encodeLongList(value, numericType, result);
            break;
          }
        default:
          {
            throwUnsupportedType(field);
          }
      }
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream",
          field.getName());
      throw new SSZException(error, ex);
    }
  }

  private void encodeIntList(List<Object> value, NumericType type, OutputStream result)
      throws IOException {
    int[] data = new int[value.size()];
    for (int i = 0; i < value.size(); ++i) {
      data[i] = (int) value.get(i);
    }
    result.write(SSZ.encodeUIntList(type.size, data).toArrayUnsafe());
  }

  private void encodeLongList(List<Object> value, NumericType type, OutputStream result)
      throws IOException {
    long[] data = new long[value.size()];
    for (int i = 0; i < value.size(); ++i) {
      data[i] = (long) value.get(i);
    }
    result.write(SSZ.encodeULongIntList(type.size, data).toArrayUnsafe());
  }

  private void encodeBigIntList(List<Object> value, NumericType type, OutputStream result)
      throws IOException {
    BigInteger[] data = value.toArray(new BigInteger[0]);
    result.write(SSZ.encodeBigIntegerList(type.size, data).toArrayUnsafe());
  }

  @Override
  public Object decode(SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);
    switch (numericType.type) {
      case INT:
        {
          return decodeInt(numericType, reader);
        }
      case LONG:
        {
          return decodeLong(numericType, reader);
        }
      case BIGINT:
        {
          return decodeBigInt(numericType, reader);
        }
    }

    return throwUnsupportedType(field);
  }

  private Object decodeInt(NumericType type, BytesSSZReaderProxy reader) {
    return reader.readUInt(type.size);
  }

  private Object decodeLong(NumericType type, BytesSSZReaderProxy reader) {
    return reader.readULong(type.size);
  }

  private Object decodeBigInt(NumericType type, BytesSSZReaderProxy reader) {
    return reader.readUnsignedBigInteger(type.size);
  }

  @Override
  public List decodeList(
      SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);

    switch (numericType.type) {
      case INT:
        {
          return reader.readUIntList(numericType.size);
        }
      case LONG:
        {
          return reader.readULongIntList(numericType.size);
        }
      case BIGINT:
        {
          return reader.readUnsignedBigIntegerList(numericType.size);
        }
      default:
        {
          return throwUnsupportedListType(field);
        }
    }
  }

  private NumericType parseFieldType(SSZField field) {
    if (field.getExtraSize() != null && field.getExtraSize() % Byte.SIZE != 0) {
      String error =
          String.format(
              "Size of numeric field in bits should match whole bytes, found %s",
              field.getExtraSize());
      throw new SSZSchemeException(error);
    }

    NumericType res = classToNumericType.get(field.getRawClass());
    if (field.getExtraSize() != null) {
      res = NumericType.of(res.type, field.getExtraSize());
    }

    return res;
  }

  enum Type {
    INT("int"),
    LONG("long"),
    BIGINT("bigint");

    private static final Map<String, Type> ENUM_MAP;

    static {
      ENUM_MAP = Stream.of(Type.values()).collect(Collectors.toMap(e -> e.type, identity()));
    }

    private String type;

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

  static class NumericType {
    final Type type;
    final int size;
    final int mask;

    NumericType(Type type, int size) {
      this.type = type;
      this.size = size;
      int mask = 0;
      for (int i = 0; i < size; i++) {
        mask = 1 | mask << 1;
      }
      this.mask = mask;
    }

    static NumericType of(Type type, int size) {
      return new NumericType(type, size);
    }
  }
}
