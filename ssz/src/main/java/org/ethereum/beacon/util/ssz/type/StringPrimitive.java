package org.ethereum.beacon.util.ssz.type;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import org.ethereum.beacon.util.ssz.SSZSchemeBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringPrimitive implements SSZEncoderDecoder {

  private static Set<String> supportedTypes = new HashSet<>();
  static {
    supportedTypes.add("string");
  }

  private static Set<Class> supportedClassTypes = new HashSet<>();
  static {
    supportedClassTypes.add(String.class);
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
    String sValue = (String) value;
    Bytes res = SSZ.encodeString(sValue);
    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write string value \"%s\" to stream", sValue);
      throw new RuntimeException(error, e);
    }
  }

  @Override
  public void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    try {
      String[] data = (String[]) value.toArray(new String[0]);
      result.write(SSZ.encodeStringList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream",
          field.name);
      throw new RuntimeException(error, ex);
    }
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return reader.readString();
  }

  @Override
  public List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return (List<Object>) (List<?>) reader.readStringList();
  }
}
