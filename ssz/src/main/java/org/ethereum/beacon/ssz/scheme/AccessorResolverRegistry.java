package org.ethereum.beacon.ssz.scheme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ethereum.beacon.ssz.SSZAnnotationSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeBuilder.SSZScheme.SSZField;
import org.ethereum.beacon.ssz.type.BasicContainerAccessor;
import org.ethereum.beacon.ssz.type.BooleanPrimitive;
import org.ethereum.beacon.ssz.type.BytesPrimitive;
import org.ethereum.beacon.ssz.type.SSZCodec;
import org.ethereum.beacon.ssz.type.SSZContainerAccessor;
import org.ethereum.beacon.ssz.type.SSZListAccessor;
import org.ethereum.beacon.ssz.type.StringPrimitive;
import org.ethereum.beacon.ssz.type.SubclassCodec;
import org.ethereum.beacon.ssz.type.UIntPrimitive;
import org.ethereum.beacon.ssz.type.list.ArrayAccessor;
import org.ethereum.beacon.ssz.type.list.ListAccessor;

public class AccessorResolverRegistry implements AccessorResolver {

  private Map<Class, List<CodecEntry>> registeredClassHandlers = new HashMap<>();

  List<SSZListAccessor> listAccessors = Arrays.asList(
      new ArrayAccessor(),
      new ListAccessor()
  );

  List<SSZContainerAccessor> containerAccessors =
      Arrays.asList(new BasicContainerAccessor(new SSZAnnotationSchemeBuilder(true)));

  {
    registerCodec(new UIntPrimitive());
    registerCodec(new BytesPrimitive());
    registerCodec(new BooleanPrimitive());
    registerCodec(new StringPrimitive());

  }

  @Override
  public Optional<SSZListAccessor> resolveListAccessor(SSZField field) {
    return listAccessors.stream().filter(a -> a.isSupported(field)).findFirst();
  }

  @Override
  public Optional<SSZContainerAccessor> resolveContainerAccessor(SSZField field) {
    return containerAccessors.stream().filter(a -> a.isSupported(field)).findFirst();
  }

  @Override
  public SSZCodec resolveBasicTypeCodec(SSZField field) {
    Class<?> type = field.fieldType;
    boolean subclassCodec = false;
    if (!SubclassCodec.getSerializableClass(type).equals(type)) {
      type = SubclassCodec.getSerializableClass(type);
      subclassCodec = true;
    }

    SSZCodec codec = null;
    if (registeredClassHandlers.containsKey(type)) {
      List<CodecEntry> codecs = registeredClassHandlers.get(type);
      if (field.extraType == null || field.extraType.isEmpty()) {
        codec = codecs.get(0).codec;
      } else {
        for (CodecEntry codecEntry : codecs) {
          if (codecEntry.types.contains(field.extraType)) {
            codec = codecEntry.codec;
            break;
          }
        }
      }
    }

    if (codec != null && subclassCodec) {
      codec = new SubclassCodec(codec);
    }

    return codec;
  }

  /**
   * Registers codecs to be used for
   *
   * @param codec Codec able to encode/decode of specific class/types
   */
  public void registerCodec(SSZCodec codec) {
    for (Class clazz : codec.getSupportedClasses()) {
      if (registeredClassHandlers.get(clazz) != null) {
        registeredClassHandlers.get(clazz).add(new CodecEntry(codec, codec.getSupportedSSZTypes()));
      } else {
        registeredClassHandlers.put(
            clazz, new ArrayList<>(Collections.singletonList(new CodecEntry(codec, codec.getSupportedSSZTypes()))));
      }
    }
  }

  class CodecEntry {
    SSZCodec codec;
    Set<String> types;

    public CodecEntry(SSZCodec codec, Set<String> types) {
      this.codec = codec;
      this.types = types;
    }
  }
}
