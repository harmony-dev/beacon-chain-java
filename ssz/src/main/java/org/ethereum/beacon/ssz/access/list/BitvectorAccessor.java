package org.ethereum.beacon.ssz.access.list;

import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SSZBitListType;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.collections.Bitvector;

import java.util.List;

/**
 * {@link Bitvector} accessor
 */
public class BitvectorAccessor extends BitlistAccessor {

  @Override
  public Object getChildValue(Object value, int idx) {
    Bitvector bitvector = ((Bitvector) value);
    return bitvector.getArrayUnsafe()[idx];
  }

  @Override
  public ListInstanceBuilder createInstanceBuilder(SSZType type) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        MutableBytesValue blank = MutableBytesValue.create(children.size());
        for (int i = 0; i < children.size(); i++) {
          blank.set(i, ((Integer) children.get(i)).byteValue());
        }

        return Bitvector.of(((SSZBitListType) type).getBitSize(), blank.copy());
      }
    };
  }

  @Override
  public boolean isSupported(SSZField field) {
    return Bitvector.class.isAssignableFrom(field.getRawClass());
  }
}
