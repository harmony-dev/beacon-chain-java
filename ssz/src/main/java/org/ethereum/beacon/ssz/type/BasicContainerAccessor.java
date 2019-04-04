package org.ethereum.beacon.ssz.type;


import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

public class BasicContainerAccessor implements SSZContainerAccessor {

  protected class BasicAccessor implements Accessor {
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
            + "you should either have public field or public getter for it", field.name));
      }
    }
  }

  private final SSZSchemeBuilder sszSchemeBuilder;

  public BasicContainerAccessor(SSZSchemeBuilder sszSchemeBuilder) {
    this.sszSchemeBuilder = sszSchemeBuilder;
  }

  @Override
  public boolean isSupported(SSZField containerDescriptor) {
    if (!containerDescriptor.fieldType.isAnnotationPresent(SSZSerializable.class)) {
      return false;
    }
    if (createAccessor(containerDescriptor).getChildDescriptors().isEmpty()) {
      return false;
    }
    return true;
  }

  @Override
  public Accessor createAccessor(SSZField containerDescriptor) {
    return new BasicAccessor(containerDescriptor);
  }

  @Override
  public InstanceBuilder createInstanceBuilder(SSZField containerDescriptor) {
    throw new UnsupportedOperationException();
  }
}
