package org.ethereum.beacon.ssz.access.basic;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import tech.pegasys.artemis.util.bytes.BytesValue;
import tech.pegasys.artemis.util.uint.UInt24;
import tech.pegasys.artemis.util.uint.UInt256;
import tech.pegasys.artemis.util.uint.UInt64;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * SSZ Codec designed to work with fixed numeric data classes, check list in {@link
 * #getSupportedClasses()}
 */
public class UIntCodec implements SSZBasicAccessor {
  private static Map<Class, NumericType> classToNumericType = new HashMap<>();
  private static Set<String> supportedTypes = new HashSet<>();
  private static Set<Class> supportedClassTypes = new HashSet<>();

  static {
    classToNumericType.put(UInt24.class, NumericType.of(Type.LONG, 24));
    classToNumericType.put(UInt64.class, NumericType.of(Type.LONG, 64));
    classToNumericType.put(UInt256.class, NumericType.of(Type.BIGINT, 256));
  }

  static {
  }

  static {
    supportedClassTypes.add(UInt24.class);
    supportedClassTypes.add(UInt64.class);
    supportedClassTypes.add(UInt256.class);
  }

  private static void writeBytes(Bytes value, OutputStream result) {
    try {
      result.write(value.toArrayUnsafe());
    } catch (IOException e) {
      throw new SSZException(String.format("Failed to write value \"%s\" to stream", value), e);
    }
  }

  private static void writeBytesValue(BytesValue value, OutputStream result) {
    try {
      result.write(value.getArrayUnsafe());
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

    switch (numericType.size) {
      case 24:
        {
          UInt24 uValue = (UInt24) value;
          Bytes bytes = SSZ.encodeULong(uValue.getValue(), numericType.size);
          writeBytes(bytes, result);
          break;
        }
      case 64:
        {
          UInt64 uValue = (UInt64) value;
          Bytes bytes = SSZ.encodeULong(uValue.getValue(), numericType.size);
          writeBytes(bytes, result);
          break;
        }
      case 256:
        {
          UInt256 uValue = (UInt256) value;
          writeBytesValue(uValue.bytes(), result);
          break;
        }
      default:
        {
          throw new SSZException(String.format("Failed to write value \"%s\" to stream", value));
        }
    }
  }

  @Override
  public Object decode(SSZField field, BytesSSZReaderProxy reader) {
    NumericType numericType = parseFieldType(field);
    switch (numericType.type) {
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

  private Object decodeLong(NumericType type, BytesSSZReaderProxy reader) {
    // XXX: reader.readULong is buggy
    switch (type.size) {
      case 24:
        {
          return UInt24.valueOf(reader.readUnsignedBigInteger(type.size).intValue());
        }
      case 64:
        {
          return UInt64.valueOf(reader.readUnsignedBigInteger(type.size).longValue());
        }
    }
    String error = String.format("Unsupported type \"%s\"", type);
    throw new SSZSchemeException(error);
  }

  private Object decodeBigInt(NumericType type, BytesSSZReaderProxy reader) {
    switch (type.size) {
      case 256:
        {
          return UInt256.wrap(
              BytesValue.of(reader.readUnsignedBigInteger(type.size).toByteArray()));
        }
      default:
        {
          String error = String.format("Unsupported type \"%s\"", type);
          throw new SSZSchemeException(error);
        }
    }
  }

  private NumericType parseFieldType(SSZField field) {
    return classToNumericType.get(field.getRawClass());
  }

  enum Type {
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

    NumericType(Type type, int size) {
      this.type = type;
      this.size = size;
    }

    static NumericType of(Type type, int size) {
      NumericType res = new NumericType(type, size);
      return res;
    }
  }
}
