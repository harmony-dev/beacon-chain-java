package org.ethereum.beacon.ssz.type.list;

import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import tech.pegasys.artemis.util.collections.ReadList;

public class ReadListAccessor extends AbstractListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return field.fieldType.isAssignableFrom(ReadList.class);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    return extractElementType(field, 1);
  }

  protected Function<Integer, ? extends Number> resolveIndexConverter(Class indexClass) {
    if (indexClass.equals(Integer.class)) {
      return Integer::valueOf;
    } else {
      throw new UnsupportedOperationException("Index converter not found for " + indexClass);
    }
  }

  @Override
  public int getChildrenCount(Object complexObject) {
    return ((ReadList) complexObject).size().intValue();
  }

  @Override
  public Object getChildValue(Object complexObject, int index) {
    return ((ReadList) complexObject).get(index);
  }

  @Override
  public ListInstanceBuilder createInstanceBuilder(SSZField listType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        return ReadList.wrap(children, resolveIndexConverter((Class<?>)
            listType.fieldGenericType.getActualTypeArguments()[0]));
      }
    };
  }
}
