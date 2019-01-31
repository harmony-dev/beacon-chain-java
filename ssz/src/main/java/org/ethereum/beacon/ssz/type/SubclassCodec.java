package org.ethereum.beacon.ssz.type;

import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.ConstructorObjCreator;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;

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
        field.type, new Class[] {serializableField.type}, new Object[] {serializableTypeObject}).getValue1();
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
                    field.type,
                    new Class[] {serializableField.type},
                    new Object[] {serializableTypeObject}).getValue1())
        .collect(Collectors.toList());
  }

  private static SSZField getSerializableField(SSZField field) {
    SSZField ret = new SSZField();
    ret.type = field.getSerializableClass();
    ret.name = field.name;
    ret.multipleType = field.multipleType;
    ret.extraType = field.extraType;
    ret.extraSize = field.extraSize;
    ret.getter = field.getter;
    ret.notAContainer = field.notAContainer;
    return ret;
  }
}
