package org.ethereum.beacon.ssz.access.list;

import java.util.List;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public class ListAccessor extends AbstractListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return List.class.isAssignableFrom(field.fieldType);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    return extractElementType(field, 0);
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
  public ListInstanceBuilder createInstanceBuilder(SSZField listType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        return children;
      }
    };
  }
}