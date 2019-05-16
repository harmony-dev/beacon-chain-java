package org.ethereum.beacon.ssz.access.basic;

import net.consensys.cava.bytes.Bytes;
import org.ethereum.beacon.ssz.visitor.SSZReader;
import org.ethereum.beacon.ssz.visitor.SSZWriter;
import net.consensys.cava.ssz.SSZException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;

/** {@link SSZBasicAccessor} for {@link Boolean} and {@link boolean} */
public class BooleanPrimitive implements SSZBasicAccessor {

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
  public void encode(Object value, SSZField field, OutputStream result) {
    boolean boolValue = (boolean) value;
    Bytes res = SSZWriter.encodeBoolean(boolValue);
    try {
      result.write(res.toArrayUnsafe());
    } catch (IOException e) {
      String error = String.format("Failed to write boolean value \"%s\" to stream", value);
      throw new SSZException(error, e);
    }
  }

  @Override
  public Object decode(SSZField field, SSZReader reader) {
    return reader.readBoolean();
  }
}
