package tech.pegasys.artemis.util.bytes;

public class MutableArrayWrappingBytes96 extends MutableArrayWrappingBytesValue
    implements MutableBytes96 {

  MutableArrayWrappingBytes96(byte[] bytes) {
    this(bytes, 0);
  }

  MutableArrayWrappingBytes96(byte[] bytes, int offset) {
    super(bytes, offset, SIZE);
  }

  @Override
  public Bytes96 copy() {
    // We *must* override this method because ArrayWrappingBytes96 assumes that it is the case.
    return new ArrayWrappingBytes96(arrayCopy());
  }

  @Override
  public MutableBytes96 mutableCopy() {
    return new MutableArrayWrappingBytes96(arrayCopy());
  }
}
