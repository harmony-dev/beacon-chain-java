package org.ethereum.beacon.ssz.access.list;

import java.lang.reflect.Array;
import java.util.List;
import org.ethereum.beacon.ssz.access.SSZField;

public class ArrayAccessor extends AbstractListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return field.fieldType.isArray();
  }

  @Override
  public int getChildrenCount(Object complexObject) {
    return Array.getLength(complexObject);
  }

  @Override
  public Object getChildValue(Object complexObject, int index) {
    return Array.get(complexObject, index);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    SSZField sszField = new SSZField();
    sszField.fieldType = field.fieldType.getComponentType();
    return sszField;
  }


  @Override
  public ListInstanceBuilder createInstanceBuilder(SSZField compositeDescriptor) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        if (!getListElementType(compositeDescriptor).fieldType.isPrimitive()) {
          return children.toArray(new Object[children.size()]);
        } else {
          if (getListElementType(compositeDescriptor).fieldType == byte.class) {
            Object ret = Array.newInstance(byte.class);
            for (int i = 0; i < children.size(); i++) {
              Array.setByte(ret, i, (Byte) children.get(i));
            }
            return ret;
          } else {
            throw new UnsupportedOperationException("Not implemented yet");
          }
        }
      }
    };
  }
}
