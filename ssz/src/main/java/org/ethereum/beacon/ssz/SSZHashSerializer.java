package org.ethereum.beacon.ssz;

import org.javatuples.Triplet;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends {@link SSZSerializer} to match Tree Hash algorithm.
 *
 * @see <a
 *     href="https://github.com/ethereum/eth2.0-specs/blob/master/specs/simple-serialize.md#tree-hash">SSZ
 *     Tree Hash</a> in the spec
 */
public class SSZHashSerializer extends SSZSerializer {

  private static final int HASH_LENGTH = 32;

  public SSZHashSerializer(
      SSZSchemeBuilder schemeBuilder,
      SSZCodecResolver codecResolver,
      SSZModelFactory sszModelFactory) {
    super(schemeBuilder, codecResolver, sszModelFactory);
  }

  /**
   * Shortcut to {@link #encode(Object, Class)}. Resolves class using input object. Not suitable for
   * null values.
   *
   * @param input
   */
  @Override
  public byte[] encode(@Nullable Object input, Class clazz) {
    byte[] preBakedHash;
    if (input instanceof List) {
      preBakedHash = encodeList((List) input);
    } else {
      preBakedHash = super.encode(input, clazz);
    }
    // For the final output only (ie. not intermediate outputs), if the output is less than 32
    // bytes, right-zero-pad it to 32 bytes.
    byte[] res;
    if (preBakedHash.length < HASH_LENGTH) {
      res = new byte[HASH_LENGTH];
      System.arraycopy(preBakedHash, 0, res, 0, preBakedHash.length);
    } else {
      res = preBakedHash;
    }

    return res;
  }

  private byte[] encodeList(List input) {
    if (input.isEmpty()) {
      return new byte[0];
    }
    Class internalClass = input.get(0).getClass();
    checkSSZSerializableAnnotation(internalClass);

    // Cook field for such List
    SSZSchemeBuilder.SSZScheme.SSZField field = new SSZSchemeBuilder.SSZScheme.SSZField();
    field.type = internalClass;
    field.multipleType = SSZSchemeBuilder.SSZScheme.MultipleType.LIST;
    field.notAContainer = false;

    ByteArrayOutputStream res = new ByteArrayOutputStream();
    codecResolver.resolveEncodeFunction(field).accept(new Triplet<>(input, res, this));

    return res.toByteArray();
  }
}
