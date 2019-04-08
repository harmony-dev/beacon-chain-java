package org.ethereum.beacon.ssz.access.list;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;
import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.access.SSZField;
import tech.pegasys.artemis.util.collections.ReadList;

public class ReadListAccessor extends AbstractListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return ReadList.class.isAssignableFrom(field.getRawClass());
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    return extractElementType(field, 1);
  }

  protected Function<Integer, ? extends Number> resolveIndexConverter(Class<?> indexClass) {
    try {
      Constructor intCtor = indexClass.getConstructor(int.class);
      Function<Integer, ? extends Number> ret = i -> {
        try {
          return (Number) intCtor.newInstance(i);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      };
      return ret;
    } catch (NoSuchMethodException e) {
    }
    throw new SSZSerializeException("Index converter not found for " + indexClass);
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
            listType.getParametrizedType().getActualTypeArguments()[0]));
      }
    };
  }
}
