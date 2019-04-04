package org.ethereum.beacon.ssz.type.list;

import java.lang.reflect.Array;
import java.util.List;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.scheme.SSZListType;
import org.ethereum.beacon.ssz.type.SSZListAccessor;

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
  public Object getChild(Object complexObject, int index) {
    return Array.get(complexObject, index);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    SSZField sszField = new SSZField();
    sszField.fieldType = field.fieldType.getComponentType();
    return sszField;
  }


  @Override
  public InstanceBuilder createInstanceBuilder(SSZListType listType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        if (!listType.getElementType().getClass().isPrimitive()) {
          return children.toArray(new Object[children.size()]);
        } else {
          if (listType.getElementType().getTypeDescriptor().fieldType == byte.class) {
            Object ret = Array.newInstance(listType.getElementType().getClass());
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
