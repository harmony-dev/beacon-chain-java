package org.ethereum.beacon.ssz.access.list;

import org.ethereum.beacon.ssz.SSZDeserializeException;
import org.ethereum.beacon.ssz.access.SSZField;
import org.ethereum.beacon.ssz.type.SSZBitListType;
import org.ethereum.beacon.ssz.type.SSZType;
import tech.pegasys.artemis.util.bytes.MutableBytesValue;
import tech.pegasys.artemis.util.collections.Bitlist;

import java.util.List;

/** {@link Bitlist} accessor */
public class BitlistAccessor extends AbstractListAccessor {

  @Override
  public int getChildrenCount(Object value) {
    return ((Bitlist) value).byteSize();
  }

  @Override
  public Object getChildValue(Object value, int idx) {
    Bitlist bitlist = ((Bitlist) value);
    if ((idx + 1) == bitlist.byteSize()) {
      byte withoutSize = idx < ((bitlist.size() + 7) / 8) ? bitlist.getArrayUnsafe()[idx] : 0;
      int bitNumber = bitlist.size() % 8;
      return withoutSize | (1 << bitNumber); // add size bit
    } else {
      return bitlist.getArrayUnsafe()[idx];
    }
  }

  @Override
  public SSZField getListElementType(SSZField listTypeDescriptor) {
    return new SSZField(byte.class);
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

        try {
          return Bitlist.of(blank, ((SSZBitListType) type).getMaxBitSize());
        } catch (IllegalArgumentException ex) {
          throw new SSZDeserializeException(
              "Failed to create Bitlist instance from input data", ex);
        }
      }
    };
  }

  @Override
  public boolean isSupported(SSZField field) {
    return Bitlist.class.isAssignableFrom(field.getRawClass());
  }
}
