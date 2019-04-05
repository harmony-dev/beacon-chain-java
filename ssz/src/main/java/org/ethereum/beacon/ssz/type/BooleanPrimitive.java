package org.ethereum.beacon.ssz.type;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

/** {@link SSZCodec} for {@link Boolean} and {@link boolean} */
public class BooleanPrimitive implements SSZCodec {

  private static Set<String> supportedTypes = new HashSet<>();
  private static Set<Class> supportedClassTypes = new HashSet<>();

  static {
    supportedTypes.add("bool");
  }

  static {
    supportedClassTypes.add(Boolean.class);
    supportedClassTypes.add(boolean.class);
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
    return 1;
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
  public void encodeList(
      List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    try {
      boolean[] data = new boolean[value.size()];
      for (int i = 0; i < value.size(); ++i) {
        data[i] = (boolean) value.get(i);
      }
      result.write(SSZ.encodeBooleanList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream", field.name);
      throw new SSZException(error, ex);
    }
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return reader.readBoolean();
  }

  @Override
  public List<Object> decodeList(
      SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return (List<Object>) (List<?>) reader.readBooleanList();
  }
}
