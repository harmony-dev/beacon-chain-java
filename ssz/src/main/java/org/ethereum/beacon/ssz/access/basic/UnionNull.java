package org.ethereum.beacon.ssz.access.basic;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import org.ethereum.beacon.ssz.access.SSZBasicAccessor;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.visitor.SSZReader;
import tech.pegasys.artemis.util.collections.ReadUnion;

/** special dummy accessor for dedicated ReadUnion.Null class */
public class UnionNull implements SSZBasicAccessor {

  @Override
  public Set<String> getSupportedSSZTypes() {
    return Collections.emptySet();
  }

  @Override
  public Set<Class> getSupportedClasses() {
    return Collections.singleton(ReadUnion.Null.class);
  }



  @Override
  public int getSize(SSZField field) {
    return 0;
  }

  @Override
  public void encode(Object value, SSZField field, OutputStream result) {
  }

  @Override
  public Object decode(SSZField field, SSZReader reader) {
    return null;
  }
}
