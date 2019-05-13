package org.ethereum.beacon.ssz.access.basic;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

/**
 * {@link SSZBasicAccessor} for {@link byte[]}
 *
 * <p>Supports several SSZ types in one byte array container:
 *
 * <ul>
 *   <li><b>bytes</b> - just some bytes value
 *   <li><b>hash</b> - hash with fixed byte size
 *   <li><b>address</b> - standard 20 bytes/160 bits address
 * </ul>
 *
 * Type could be clarified by {@link
 * SSZField#getExtraType()}
 */
public class BytesPrimitive implements SSZBasicAccessor {

  private static Set<String> supportedTypes = new HashSet<>();
  private static Set<Class> supportedClassTypes = new HashSet<>();

  static {
    supportedTypes.add("bytes");
  }

  static {
    supportedClassTypes.add(byte[].class);
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
    BytesType byteType = parseFieldType(field);
    Bytes res = null;
    byte[] data = (byte[]) value;
    if (byteType.size == null) {
      res = SSZ.encodeByteArray(data);
    } else {
      res = SSZ.encodeHash(Bytes.wrap(data)); // w/o length prefix
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
  public Object decode(SSZField field, BytesSSZReaderProxy reader) {
    BytesType bytesType = parseFieldType(field);
    return (bytesType.size == null)
        ? reader.readBytes().toArrayUnsafe()
        : reader.readHash(bytesType.size).toArrayUnsafe();
  }

  private BytesType parseFieldType(SSZField field) {
    return BytesType.of(Type.BYTES, field.getExtraSize());
  }

  enum Type {
    BYTES("bytes");

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

  static class BytesType {
    final Type type;
    final Integer size;

    BytesType(Type type, Integer size) {
      this.type = type;
      this.size = size;
    }

    static BytesType of(Type type, Integer size) {
      return new BytesType(type, size);
    }
  }
}
