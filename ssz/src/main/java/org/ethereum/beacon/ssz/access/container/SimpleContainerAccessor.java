package org.ethereum.beacon.ssz.access.container;


import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ethereum.beacon.ssz.creator.ObjectCreator;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.SSZSerializeException;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.javatuples.Pair;

public class SimpleContainerAccessor implements SSZContainerAccessor {

  protected class BasicAccessor implements ContainerAccessor {
    private final SSZField containerDescriptor;
    private final SSZScheme scheme;
    private final Map<String, Method> getters;

    public BasicAccessor(SSZField containerDescriptor) {
      this.containerDescriptor = containerDescriptor;
      scheme = sszSchemeBuilder.build(containerDescriptor.fieldType);
      getters = new HashMap<>();
      try {
        for (PropertyDescriptor pd :
            Introspector.getBeanInfo(containerDescriptor.fieldType).getPropertyDescriptors()) {
          getters.put(pd.getReadMethod().getName(), pd.getReadMethod());
        }
      } catch (IntrospectionException e) {
        throw new RuntimeException(String.format("Couldn't enumerate all getters in class %s", containerDescriptor.fieldType.getName()), e);
      }
    }

    @Override
    public List<SSZField> getChildDescriptors() {
      return scheme.getFields();
    }

    protected Object getContainerInstance(Object value) {
      return value;
    }

    @Override
    public Object getChildValue(Object containerInstance, int childIndex) {
      SSZField field = getChildDescriptors().get(childIndex);
      Method getter = getters.get(field.getter);
      try {
        if (getter != null) { // We have getter
          return getter.invoke(getContainerInstance(containerInstance));
        } else { // Trying to access field directly
          return containerDescriptor.fieldType.getField(field.name)
              .get(getContainerInstance(containerInstance));
        }
      } catch (Exception e) {
        throw new SSZSchemeException(String.format("Failed to get value from field %s, "
            + "you should either have public field or public getter for it", field.name), e);
      }
    }
  }

  protected class BasicInstanceBuilder implements ContainerInstanceBuilder {
    private final SSZField containerDescriptor;
    private final Map<SSZField, Object> children = new HashMap<>();
    private final List<SSZField> childDescriptors;

    public BasicInstanceBuilder(SSZField containerDescriptor) {
      this.containerDescriptor = containerDescriptor;
      childDescriptors = getAccessor(containerDescriptor).getChildDescriptors();
    }

    @Override
    public void setChild(SSZField childDescriptor, Object childValue) {
      children.put(childDescriptor, childValue);
    }

    @Override
    public void setChild(int idx, Object childValue) {
      setChild(childDescriptors.get(idx), childValue);
    }

    @Override
    public Object build() {
      List<Pair<SSZField, Object>> values = new ArrayList<>();
      for (SSZField childDescriptor : childDescriptors) {
        Object value = children.get(childDescriptor);
        if (value == null) {
          throw new SSZSerializeException("Can't create " + containerDescriptor + " container instance, missing field " + childDescriptor);
        }
        values.add(Pair.with(childDescriptor, value));
      }
      return objectCreator.createObject(containerDescriptor.fieldType, values);
    }
  }

  private final SSZSchemeBuilder sszSchemeBuilder;
  private final ObjectCreator objectCreator;

  public SimpleContainerAccessor(SSZSchemeBuilder sszSchemeBuilder,
      ObjectCreator objectCreator) {
    this.sszSchemeBuilder = sszSchemeBuilder;
    this.objectCreator = objectCreator;
  }

  @Override
  public boolean isSupported(SSZField containerDescriptor) {
    if (!containerDescriptor.fieldType.isAnnotationPresent(SSZSerializable.class)) {
      return false;
    }
    if (getAccessor(containerDescriptor).getChildDescriptors().isEmpty()) {
      return false;
    }
    return true;
  }

  @Override
  public ContainerAccessor getAccessor(SSZField containerDescriptor) {
    return new BasicAccessor(containerDescriptor);
  }

  @Override
  public ContainerInstanceBuilder createInstanceBuilder(SSZField containerDescriptor) {
    return new BasicInstanceBuilder(containerDescriptor);
  }
}
