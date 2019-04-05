package org.ethereum.beacon.ssz;

import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.TypeResolver;
import org.ethereum.beacon.ssz.visitor.SSZSimpleHasher;
import org.ethereum.beacon.ssz.visitor.SSZVisitorHost;
import org.javatuples.Pair;
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

  private final SSZVisitorHost visitorHall;
  private final SSZSimpleHasher hasherVisitor;
  private final SSZSerializer serializer;
  private TypeResolver typeResolver;

  /**
   * SSZ hasher with following helpers
   *
   * @param schemeBuilder SSZ model scheme building of type {@link SSZSchemeBuilder.SSZScheme}
   * @param codecResolver Resolves field encoder/decoder {@link
   *     org.ethereum.beacon.ssz.access.SSZCodec} function
   */
  public SSZHasher(SSZSchemeBuilder schemeBuilder, SSZCodecResolver codecResolver,
      Function<BytesValue, Hash32> hashFunction) {

    this.serializer = new SSZSerializer(schemeBuilder, codecResolver, null, null);
    this.visitorHall = new SSZVisitorHost();
    hasherVisitor = new SSZSimpleHasher(serializer, hashFunction, BYTES_PER_CHUNK);
  }

  /** Calculates hash of the input object */
  @Override
  public byte[] hash(@Nullable Object input, Class clazz) {
    return visitorHall
        .handleAny(typeResolver.resolveSSZType(new SSZField(clazz)), input, hasherVisitor)
        .getFinalRoot()
        .extractArray();
  }

  @Override
  public <C> byte[] hashTruncate(@Nullable C input, Class<? extends C> clazz, String field) {
    SSZVisitorHost hall = new SSZVisitorHost();
    hall.setContainerMembersFilter(new TruncateFilter(clazz, field));
    return visitorHall
        .handleAny(typeResolver.resolveSSZType(new SSZField(clazz)), input, hasherVisitor)
        .getFinalRoot()
        .extractArray();
  }

  private static class TruncateFilter implements Predicate<Pair<Class<?>, SSZField>> {
    private final Class<?> truncateClass;
    private final String startFieldName;
    private boolean fieldHit;

    public TruncateFilter(Class<?> truncateClass, String startFieldName) {
      this.truncateClass = truncateClass;
      this.startFieldName = startFieldName;
    }

    @Override
    public boolean test(Pair<Class<?>, SSZField> field) {
      if (field.getValue0().equals(truncateClass)) {
        if (startFieldName.equals(field.getValue1().name)) {
          fieldHit = true;
        }
        return fieldHit;
      } else {
        return false;
      }
    }
  }
}
