package org.ethereum.beacon.util.ssz.type;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.util.ssz.SSZSchemeBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BooleanPrimitive implements SSZCodec {

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
  public Set<Class> getSupportedClasses() {
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
      throw new SSZException(error, e);
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
      throw new SSZException(error, ex);
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
}
