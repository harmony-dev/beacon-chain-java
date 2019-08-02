package org.ethereum.beacon.ssz.access.list;

import java.util.List;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SSZType;

public class ListAccessor extends AbstractListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return List.class.isAssignableFrom(field.getRawClass());
  }

  @Override
  public SSZField getListElementType(SSZField listTypeDescriptor) {
    return extractElementType(listTypeDescriptor, 0);
  }

  @Override
  public int getChildrenCount(Object complexObject) {
    return ((List) complexObject).size();
  }

  @Override
  public Object getChildValue(Object complexObject, int index) {
    return ((List) complexObject).get((int) index);
  }

  @Override
  public ListInstanceBuilder createInstanceBuilder(SSZType sszType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        return children;
      }
    };
  }
}
