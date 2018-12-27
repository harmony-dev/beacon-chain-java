package tech.pegasys.artemis.util.bytes;

public class DelegatingBytes48 extends BaseDelegatingBytesValue<Bytes48> implements Bytes48 {

  protected DelegatingBytes48(Bytes48 wrapped) {
    super(wrapped);
  }

  @Override
  public Bytes48 copy() {
    return wrapped.copy();
  }

  @Override
  public MutableBytes48 mutableCopy() {
    return wrapped.mutableCopy();
  }
}
