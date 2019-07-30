package org.ethereum.beacon.ssz.access.list;

import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.collections.ReadList;
import tech.pegasys.artemis.util.collections.ReadVector;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.Function;

public class ReadListAccessor extends AbstractListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return ReadList.class.isAssignableFrom(field.getRawClass());
  }

  @Override
  public SSZField getListElementType(SSZField listTypeDescriptor) {
    return extractElementType(listTypeDescriptor, 1);
  }

  protected Function<Integer, ? extends Number> resolveIndexConverter(Class<?> indexClass) {
    try {
      Constructor intCtor = indexClass.getConstructor(int.class);
      Function<Integer, ? extends Number> ret =
          i -> {
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
  public ListInstanceBuilder createInstanceBuilder(SSZType sszType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        return sszType.isFixedSize()
            ? ReadVector.wrap(
                children,
                resolveIndexConverter(
                    (Class<?>)
                        sszType
                            .getTypeDescriptor()
                            .getParametrizedType()
                            .getActualTypeArguments()[0]))
            : ReadList.wrap(
                children,
                resolveIndexConverter(
                    (Class<?>)
                        sszType
                            .getTypeDescriptor()
                            .getParametrizedType()
                            .getActualTypeArguments()[0]));
      }
    };
  }
}
