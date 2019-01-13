package org.ethereum.beacon.ssz;

import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.javatuples.Pair;
import org.javatuples.Triplet;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Resolves {@link SSZCodec} function that should be used for
 * encoding/decoding of SSZ data using {@link org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField} info.
 */
public interface SSZCodecResolver {

  /**
   * Registers {@link SSZCodec} in resolver
   * Priorities in codec resolving etc depends on implementation
   * @param classes   Set of classes to be handled by this codec
   * @param types     SSZ types to be handled by this codec
   * @param codec     Codec instance
   */
  void registerCodec(Set<Class> classes, Set<String> types, SSZCodec codec);

  /**
   * SSZ Encode function matching current field
   * @param field   Field
   * @return encode function
   */
  Consumer<Triplet<Object, OutputStream, SSZSerializer>> resolveEncodeFunction(
      SSZSchemeBuilder.SSZScheme.SSZField field);

  /**
   * SSZ Decode function matching current field
   * @param field   Field
   * @return decode function
   */
  Function<Pair<BytesSSZReaderProxy, SSZSerializer>, Object> resolveDecodeFunction(
      SSZSchemeBuilder.SSZScheme.SSZField field);
}
