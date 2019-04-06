package org.ethereum.beacon.ssz;

import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.type.SSZContainerType;
import org.ethereum.beacon.ssz.type.SSZType;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZSimpleHasher;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;
import tech.pegasys.artemis.ethereum.core.Hash32;
import tech.pegasys.artemis.util.bytes.BytesValue;

/**
 * Implements Tree Hash algorithm.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ
 *     Tree Hash</a> in the spec
 */
public class SSZHasher implements BytesHasher {

  private static final int BYTES_PER_CHUNK = 32;

  private final SSZVisitorHost visitorHost;
  private final SSZSimpleHasher hasherVisitor;
  private final SSZSerializer serializer;
  private TypeResolver typeResolver;

  /**
   * SSZ hasher with following helpers
   *
   *     org.ethereum.beacon.ssz.access.SSZCodec} function
   */
  public SSZHasher(Function<BytesValue, Hash32> hashFunction, TypeResolver typeResolver) {

    this.serializer = new SSZSerializer(null, null, null, typeResolver);
    this.visitorHost = new SSZVisitorHost();
    this.typeResolver = typeResolver;
    hasherVisitor = new SSZSimpleHasher(serializer, hashFunction, BYTES_PER_CHUNK);
  }

  /** Calculates hash of the input object */
  @Override
  public byte[] hash(@Nullable Object input, Class clazz) {
    return visitorHost
        .handleAny(typeResolver.resolveSSZType(new SSZField(clazz)), input, hasherVisitor)
        .getFinalRoot()
        .extractArray();
  }

  @Override
  public <C> byte[] hashTruncate(@Nullable C input, Class<? extends C> clazz, String field) {
    return visitorHost
        .handleAny(
            new TruncatedContainerType(typeResolver.resolveSSZType(new SSZField(clazz))),
            input,
            hasherVisitor)
        .getFinalRoot()
        .extractArray();
  }

  private static class TruncatedContainerType extends SSZContainerType {
    private final SSZContainerType delegate;

    public TruncatedContainerType(SSZType delegate) {
      this.delegate = (SSZContainerType) delegate;
    }

    @Override
    public int getSize() {
      int size = delegate.getSize();
      List<SSZType> childTypes = delegate.getChildTypes();
      return size == -1 ? -1 : size - delegate.getSize() - childTypes.get(childTypes.size() - 1).getSize();
    }

    @Override
    public List<SSZType> getChildTypes() {
      List<SSZType> childTypes = delegate.getChildTypes();
      return childTypes.subList(0, childTypes.size() - 1);
    }

    @Override
    public List<String> getChildNames() {
      List<String> childNames = delegate.getChildNames();
      return childNames.subList(0, childNames.size() - 1);
    }

    @Override
    public SSZContainerAccessor getAccessor() {
      return delegate.getAccessor();
    }

    @Override
    public int getChildrenCount(Object value) {
      return delegate.getChildrenCount(value) - 1;
    }

    @Override
    public Object getChild(Object value, int idx) {
      if (idx >= getChildrenCount(value)) {
        throw new IndexOutOfBoundsException(idx + " >= " + getChildrenCount(value) + " for " + getTypeDescriptor());
      }
      return delegate.getChild(value, idx);
    }

    @Override
    public SSZField getTypeDescriptor() {
      return delegate.getTypeDescriptor();
    }
  }
}
