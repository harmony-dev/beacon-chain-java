package org.ethereum.beacon.ssz.type.list;

import java.lang.reflect.Array;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZListAccessor;

public class ArrayAccessor implements SSZListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return field.fieldType.isArray();
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    SSZField sszField = new SSZField();
    sszField.fieldType = field.fieldType.getComponentType();
    return sszField;
  }

  @Override
  public long getChildCount(Object complexObject) {
    return Array.getLength(complexObject);
  }

  @Override
  public Object getChild(Object complexObject, long index) {
    return Array.get(complexObject, (int) index);
  }
}
