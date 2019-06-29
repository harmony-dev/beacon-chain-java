package org.ethereum.beacon.ssz.access.list;

import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.access.SSZListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;

public class SubclassListAccessor implements SSZListAccessor {
  private final SSZListAccessor superclassAccessor;

  public SubclassListAccessor(SSZListAccessor superclassAccessor) {
    this.superclassAccessor = superclassAccessor;
  }

  @Override
  public int getChildrenCount(Object value) {
    return superclassAccessor.getChildrenCount(value);
  }

  @Override
  public Object getChildValue(Object value, int idx) {
    return superclassAccessor.getChildValue(value, idx);
  }

  @Override
  public SSZField getListElementType(SSZField listTypeDescriptor) {
    return superclassAccessor.getListElementType(new SSZField(getSerializableClass(
        listTypeDescriptor.getRawClass())));
  }

  @Override
  public ListInstanceBuilder createInstanceBuilder(SSZField listType) {
    ListInstanceBuilder instanceBuilder = superclassAccessor.createInstanceBuilder(listType);
    return new ListInstanceBuilder() {
      @Override
      public void addChild(Object childValue) {
        instanceBuilder.addChild(childValue);
      }

      @Override
      public void setChild(int idx, Object childValue) {
        instanceBuilder.setChild(idx, childValue);
      }

      @Override
      public Object build() {
        Object superclassInstance = instanceBuilder.build();
        SSZField serializableField = getSerializableField(listType);
        return ConstructorObjCreator.createInstanceWithConstructor(
            listType.getRawClass(), new Class[] {serializableField.getRawClass()}, new Object[] {superclassInstance});
      }
    };
  }

  public static SSZField getSerializableField(SSZField field) {
    return new SSZField(getSerializableClass(field.getRawClass()),
        field.getFieldAnnotation(),
        field.getExtraType(),
        field.getExtraSize(),
        field.getName(),
        field.getGetter());
  }

  @Override
  public CompositeInstanceAccessor getInstanceAccessor(SSZField compositeDescriptor) {
    return superclassAccessor.getInstanceAccessor(compositeDescriptor);
  }

  @Override
  public boolean isSupported(SSZField field) {
    return superclassAccessor.isSupported(field);
  }

  /**
   *  If the field class specifies {@link SSZSerializable#serializeAs()} attribute
   *  returns the specified class.
   *  Else returns type value.
   */
  public static Class<?> getSerializableClass(Class<?> type) {
    SSZSerializable fieldClassAnnotation = type.getAnnotation(SSZSerializable.class);
    if (fieldClassAnnotation != null && fieldClassAnnotation.serializeAs() != void.class) {
      // the class of the field wants to be serialized as another class
      return fieldClassAnnotation.serializeAs();
    } else {
      return type;
    }
  }
}

