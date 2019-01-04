package org.ethereum.beacon.util.ssz.type;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import org.ethereum.beacon.util.ssz.SSZSchemeBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Function.identity;

public class BooleanPrimitive implements SSZEncoderDecoder {

  private static Set<String> supportedTypes = new HashSet<>();
  static {
    supportedTypes.add("bool");
  }

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(Boolean.class);
    supportedClassTypes.add(boolean.class);
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
    boolean boolValue = (boolean) value;
    Bytes res = SSZ.encodeBoolean(boolValue);
    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write boolean value \"%s\" to stream", value);
      throw new RuntimeException(error, e);
    }
  }

  @Override
  public void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    try {
      boolean[] data = new boolean[value.size()];
      for (int i = 0; i < value.size(); ++i) {
        data[i] = (boolean) value.get(i);
      }
      result.write(SSZ.encodeBooleanList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream",
          field.name);
      throw new RuntimeException(error, ex);
    }
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return reader.readBoolean();
  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return (List<Object>) (List<?>) reader.readBooleanList();
  }

  static class BytesType {
    Type type;
    int size;

    public BytesType() {
    }

    static BytesType of(Type type, int size) {
      BytesType res = new BytesType();
      res.type = type;
      res.size = size;
      return res;
    }
  }

  enum Type {
    BYTES("bytes"),
    HASH("hash"),
    ADDRESS("address");

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

  private BytesType parseFieldType(SSZSchemeBuilder.SSZScheme.SSZField field) {
    Type type = Type.fromValue(field.extraType);
    if (type.equals(Type.ADDRESS)) {
      if (field.extraSize != null) {
        throw new RuntimeException("Address is fixed 20 bytes type");
      } else {
        return BytesType.of(Type.ADDRESS, 20);
      }
    } else {
      if (field.extraSize == null) {
        throw new RuntimeException(String.format("Type %s required size information", field.extraType));
      } else {
        return BytesType.of(type, field.extraSize);
      }
    }
  }
}
