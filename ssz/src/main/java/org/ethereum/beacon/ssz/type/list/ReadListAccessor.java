package org.ethereum.beacon.ssz.type.list;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZListAccessor;
import tech.pegasys.artemis.util.collections.ReadList;

public class ReadListAccessor implements SSZListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return field.fieldType.isAssignableFrom(ReadList.class);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    SSZField sszField = new SSZField();
    if (field.fieldGenericType == null) {
      sszField.fieldType = Object.class;
    } else {
      Type listTypeArgument = field.fieldGenericType.getActualTypeArguments()[1];

      if (listTypeArgument instanceof WildcardType) {
        listTypeArgument = ((WildcardType) listTypeArgument).getLowerBounds()[0];
      }

      if (listTypeArgument instanceof Class) {
        sszField.fieldType = (Class<?>) listTypeArgument;
      } else if (listTypeArgument instanceof ParameterizedType) {
        sszField.fieldType = (Class<?>) ((ParameterizedType) listTypeArgument).getRawType();
        sszField.fieldGenericType = (ParameterizedType) listTypeArgument;
      } else {
        throw new RuntimeException("Internal error: unknown list type: " + listTypeArgument);
      }
    }
    return sszField;
  }

  @Override
  public long getChildCount(Object complexObject) {
    return ((ReadList) complexObject).size().longValue();
  }

  @Override
  public Object getChild(Object complexObject, long index) {
    return ((ReadList) complexObject).get(index);
  }
}
