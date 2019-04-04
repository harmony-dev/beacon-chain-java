package org.ethereum.beacon.ssz.type.list;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.scheme.SSZListType;
import org.ethereum.beacon.ssz.type.SSZListAccessor;
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
  public Object getChild(Object complexObject, int index) {
    return ((ReadList) complexObject).get(index);
  }

  @Override
  public InstanceBuilder createInstanceBuilder(SSZListType listType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        return ReadList.wrap(children, resolveIndexConverter((Class<?>)
            listType.getTypeDescriptor().fieldGenericType.getActualTypeArguments()[0]));
      }
    };
  }
}
