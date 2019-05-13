package org.ethereum.beacon.ssz.access.basic;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import net.consensys.cava.ssz.SSZ;
import net.consensys.cava.ssz.SSZException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;

/** {@link SSZBasicAccessor} for {@link String} */
public class StringPrimitive implements SSZBasicAccessor {

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
  public void encode(Object value, SSZField field, OutputStream result) {
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
  public Object decode(SSZField field, BytesSSZReaderProxy reader) {
    return reader.readString();
  }
}
