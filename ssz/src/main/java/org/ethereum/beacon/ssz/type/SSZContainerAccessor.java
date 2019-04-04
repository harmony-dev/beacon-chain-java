package org.ethereum.beacon.ssz.type;

import java.util.List;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

public interface SSZContainerAccessor {

  interface InstanceBuilder {

    void setChild(SSZField childDescriptor, Object childValue);

    Object build();
  }

  interface Accessor {

    List<SSZField> getChildDescriptors();

    Object getChildValue(Object containerInstance, int childIndex);
  }

  boolean isSupported(SSZField field);

  Accessor createAccessor(SSZField containerDescriptor);

  InstanceBuilder createInstanceBuilder(SSZField containerDescriptor);

}
