package org.ethereum.beacon.ssz.type.list;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.SSZListAccessor;

public class ListAccessor implements SSZListAccessor {

  @Override
  public boolean isSupported(SSZField field) {
    return field.fieldType.isAssignableFrom(List.class);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    SSZField sszField = new SSZField();
    if (field.fieldGenericType == null) {
      sszField.fieldType = Object.class;
    } else {
      Type listTypeArgument = field.fieldGenericType.getActualTypeArguments()[0];

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
    return ((List) complexObject).size();
  }

  @Override
  public Object getChild(Object complexObject, long index) {
    return ((List) complexObject).get((int) index);
  }
}
