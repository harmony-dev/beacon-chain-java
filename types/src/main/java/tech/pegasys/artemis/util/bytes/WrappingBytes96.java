package tech.pegasys.artemis.util.bytes;

import static com.google.common.base.Preconditions.checkArgument;

public class WrappingBytes96 extends AbstractBytesValue implements Bytes96 {

  private final BytesValue value;

  WrappingBytes96(BytesValue value) {
    checkArgument(
        value.size() == SIZE, "Expected value to be %s bytes, but is %s bytes", SIZE, value.size());
    this.value = value;
  }

  @Override
  public byte get(int i) {
    return value.get(i);
  }

  @Override
  public BytesValue slice(int index, int length) {
    return value.slice(index, length);
  }

  @Override
  public MutableBytes96 mutableCopy() {
    MutableBytes96 copy = MutableBytes96.create();
    value.copyTo(copy);
    return copy;
  }

  @Override
  public Bytes96 copy() {
    return mutableCopy();
  }

  @Override
  public byte[] getArrayUnsafe() {
    return value.getArrayUnsafe();
  }

  @Override
  public int size() {
    return value.size();
  }
}
