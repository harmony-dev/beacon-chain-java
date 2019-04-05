package org.ethereum.beacon.ssz.access.basic;

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
import org.ethereum.beacon.ssz.access.SSZCodec;

/** {@link SSZCodec} for {@link String} */
public class StringPrimitive implements SSZCodec {

  private static Set<String> supportedTypes = new HashSet<>();
  private static Set<Class> supportedClassTypes = new HashSet<>();

  static {
    supportedTypes.add("string");
  }

  static {
    supportedClassTypes.add(String.class);
  }

  @Override
  public int getSize(SSZField field) {
    return -1;
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
    String sValue = (String) value;
    Bytes res = SSZ.encodeString(sValue);
    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write string value \"%s\" to stream", sValue);
      throw new SSZException(error, e);
    }
  }

  @Override
  public void encodeList(
      List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result) {
    try {
      String[] data = value.toArray(new String[0]);
      result.write(SSZ.encodeStringList(data).toArrayUnsafe());
    } catch (IOException ex) {
      String error = String.format("Failed to write data from field \"%s\" to stream", field.name);
      throw new SSZException(error, ex);
    }
  }

  @Override
  public Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return reader.readString();
  }

  @Override
  public List<Object> decodeList(
      SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader) {
    return (List<Object>) (List<?>) reader.readStringList();
  }
}
