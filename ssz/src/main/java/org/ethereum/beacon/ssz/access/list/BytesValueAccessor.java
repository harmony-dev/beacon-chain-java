package org.ethereum.beacon.ssz.access.list;

import java.util.List;
import org.ethereum.beacon.ssz.access.SSZField;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class BytesValueAccessor extends AbstractListAccessor {

  @Override
  public int getChildrenCount(Object value) {
    return ((BytesValue) value).size();
  }

  @Override
  public Object getChildValue(Object value, int idx) {
    return ((BytesValue) value).get(idx);
  }

  @Override
  public SSZField getListElementType(SSZField field) {
    return new SSZField(byte.class);
  }

  @Override
  public ListInstanceBuilder createInstanceBuilder(SSZField listType) {
    return new SimpleInstanceBuilder() {
      @Override
      protected Object buildImpl(List<Object> children) {
        byte[] vals = new byte[children.size()];
        for (int i = 0; i < children.size(); i++) {
          vals[i] = (Byte) children.get(i);
        }
        return BytesValue.wrap(vals);
      }
    };
  }

  @Override
  public boolean isSupported(SSZField field) {
    return BytesValue.class.isAssignableFrom(field.getRawClass());
  }
}
