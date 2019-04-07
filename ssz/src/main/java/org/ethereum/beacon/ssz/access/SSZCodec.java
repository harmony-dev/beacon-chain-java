package org.ethereum.beacon.ssz.access;

import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.SSZSerializer;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Used for encoding and decoding of SSZ data (restoring instance of class)
 *
 * <p>For more information check {@link SSZSerializer}
 */
public interface SSZCodec {

  /**
   * Set of compatible SSZ types represented as strings. If type could be extended with numeric
   * size, only text part is added in type part.
   *
   * @return text types
   */
  Set<String> getSupportedSSZTypes();

  /**
   * Set of compatible classes.
   *
   * <p>Field with class other than included in this list will be never routed to be encoded/decoded
   * by this codec.
   *
   * @return compatible classes
   */
  Set<Class> getSupportedClasses();

  int getSize(SSZField field);

  /**
   * Encodes field as SSZ type and writes it to output stream
   *
   * @param value Field value
   * @param field Field type
   * @param result Output stream
   */
  void encode(Object value, SSZField field, OutputStream result);

  /**
   * Encodes list field as SSZ type and writes it to output stream
   *
   * @param value Field value
   * @param field Field type
   * @param result Output stream
   */
  void encodeList(
      List<Object> value, SSZField field, OutputStream result);

  /**
   * Encodes array field as SSZ type and writes it to output stream
   *
   * @param value Field value
   * @param field Field type
   * @param result Output stream
   */
  default void encodeArray(
      Object[] value, SSZField field, OutputStream result) {
    encodeList(Arrays.asList(value), field, result);
  }

  /**
   * Decodes SSZ encoded data and returns result
   *
   * @param field Type of field to read at this point
   * @param reader Reader which holds SSZ encoded data at the appropriate point. Pointer will be
   *     moved to the end of this field/beginning of next one after reading is performed.
   * @return field value
   */
  Object decode(SSZField field, BytesSSZReaderProxy reader);

  /**
   * Decodes SSZ encoded data and returns result
   *
   * @param field Type of field to read at this point, list
   * @param reader Reader which holds SSZ encoded data at the appropriate point. Pointer will be
   *     moved to the end of this field/beginning of next one after reading is performed.
   * @return field list value
   */
  List decodeList(SSZField field, BytesSSZReaderProxy reader);

  /**
   * Decodes SSZ encoded data and returns result
   *
   * @param field Type of field to read at this point, array
   * @param reader Reader which holds SSZ encoded data at the appropriate point. Pointer will be
   *     moved to the end of this field/beginning of next one after reading is performed.
   * @return field array value
   */
  default Object[] decodeArray(
      SSZField field, BytesSSZReaderProxy reader) {
    List list = decodeList(field, reader);
    return list.toArray();
  }

  /**
   * Helper designed to throw usual error
   *
   * @param field Handled field
   * @return technically nothing, exception is thrown before
   * @throws RuntimeException {@link SSZSchemeException} that current codec cannot handle input
   *     field type
   */
  default Object throwUnsupportedType(SSZField field)
      throws RuntimeException {
    throw new SSZSchemeException(String.format("Type [%s] is not supported", field.fieldType));
  }

  /**
   * Helper designed to throw usual error for list
   *
   * @param field Handled field, list
   * @return technically nothing, exception is thrown before
   * @throws RuntimeException {@link SSZSchemeException} that current code cannot handle input field
   *     type
   */
  default List<Object> throwUnsupportedListType(SSZField field)
      throws RuntimeException {
    throw new SSZSchemeException(String.format("List of types [%s] is not supported", field.fieldType));
  }
}
