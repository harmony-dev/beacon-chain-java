package org.ethereum.beacon.ssz.type;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.creator.ConstructorObjCreator;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

/**
 * The SSZCodec which implements logic of {@link SSZSerializable#serializeAs()} attribute
 * It delegates calls to wrapped Codec corresponding to <code>serializeAs</code> class
 * but substitutes <code>field.type</code> with the <code>serializeAs</code> class
 * and decodes result to the original <code>field.type</code>.
 */
public class SubclassCodec implements SSZCodec {

  private final SSZCodec superclassCodec;

  public SubclassCodec(SSZCodec superclassCodec) {
    this.superclassCodec = superclassCodec;
  }

  @Override
  public Set<String> getSupportedSSZTypes() {
    return superclassCodec.getSupportedSSZTypes();
  }

  @Override
  public Set<Class> getSupportedClasses() {
    return superclassCodec.getSupportedClasses();
  }

  @Override
  public int getSize(SSZField field) {
    return superclassCodec.getSize(field);
  }

  @Override
  public void encode(Object value, SSZField field,
      OutputStream result) {
    superclassCodec.encode(value, getSerializableField(field), result);
  }

  @Override
  public void encodeList(List<Object> value,
      SSZField field, OutputStream result) {
    superclassCodec.encodeList(value, getSerializableField(field), result);
  }

  @Override
  public Object decode(SSZField field,
      BytesSSZReaderProxy reader) {
    SSZField serializableField = getSerializableField(field);
    Object serializableTypeObject = superclassCodec.decode(serializableField, reader);
    return ConstructorObjCreator.createInstanceWithConstructor(
        field.fieldType, new Class[] {serializableField.fieldType}, new Object[] {serializableTypeObject});
  }

  @Override
  public List decodeList(SSZField field,
      BytesSSZReaderProxy reader) {
    SSZField serializableField = getSerializableField(field);
    List<?> serializableTypeList = superclassCodec.decodeList(serializableField, reader);
    return serializableTypeList.stream()
        .map(
            serializableTypeObject ->
                ConstructorObjCreator.createInstanceWithConstructor(
                    field.fieldType,
                    new Class[] {serializableField.fieldType},
                    new Object[] {serializableTypeObject}))
        .collect(Collectors.toList());
  }

  private static SSZField getSerializableField(SSZField field) {
    SSZField ret = new SSZField();
    ret.fieldType = getSerializableClass(field.fieldType);
    ret.name = field.name;
    ret.extraType = field.extraType;
    ret.extraSize = field.extraSize;
    ret.getter = field.getter;
    return ret;
  }

  /**
   *  If the field class specifies {@link SSZSerializable#serializeAs()} attribute
   *  returns the specified class.
   *  Else returns type value.
   */
  public static Class<?> getSerializableClass(Class<?> type) {
    SSZSerializable fieldClassAnnotation = type.getAnnotation(SSZSerializable.class);
    if (fieldClassAnnotation != null && fieldClassAnnotation.serializeAs() != void.class) {
      // the class of the field wants to be serialized as another class
      return fieldClassAnnotation.serializeAs();
    } else {
      return type;
    }
  }


}
