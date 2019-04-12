package org.ethereum.beacon.ssz.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.access.basic.SubclassCodec;
import org.ethereum.beacon.ssz.access.list.SubclassListAccessor;
import org.ethereum.beacon.ssz.annotation.SSZSerializable;

public class AccessorResolverRegistry implements AccessorResolver {

  private Map<Class, List<CodecEntry>> registeredClassHandlers = new HashMap<>();
  List<SSZListAccessor> listAccessors;
  List<SSZContainerAccessor> containerAccessors;

  public AccessorResolverRegistry withContainerAccessors(List<SSZContainerAccessor> containerAccessors) {
    this.containerAccessors = containerAccessors;
    return this;
  }

  public AccessorResolverRegistry withListAccessors(List<SSZListAccessor> listAccessors) {
    this.listAccessors = listAccessors;
    return this;
  }

  public AccessorResolverRegistry withBasicCodecs(List<SSZBasicAccessor> codecs) {
    for (SSZBasicAccessor codec : codecs) {
      registerCodec(codec);
    }
    return this;
  }

  @Override
  public Optional<SSZListAccessor> resolveListAccessor(SSZField field) {
    if (!SubclassListAccessor.getSerializableClass(field.getRawClass()).equals(field.getRawClass())) {
      return resolveListAccessor(SubclassListAccessor.getSerializableField(field))
          .map(SubclassListAccessor::new);
    }

    Object accessor = getAccessorFromAnnotation(field);
    if (accessor instanceof SSZListAccessor) {
      return Optional.of((SSZListAccessor) accessor);
    }
    return listAccessors.stream().filter(a -> a.isSupported(field)).findFirst();
  }

  @Override
  public Optional<SSZContainerAccessor> resolveContainerAccessor(SSZField field) {
    Object accessor = getAccessorFromAnnotation(field);
    if (accessor instanceof SSZContainerAccessor) {
      return Optional.of((SSZContainerAccessor) accessor);
    }

    return containerAccessors.stream().filter(a -> a.isSupported(field)).findFirst();
  }

  private Object getAccessorFromAnnotation(SSZField field) {
    SSZSerializable annotation = field.getRawClass().getAnnotation(SSZSerializable.class);
    if (annotation == null || annotation.accessor() == void.class) {
      return null;
    }

    try {
      return annotation.accessor().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new SSZSchemeException("Couldn't create codec instance " + annotation.accessor() + " for field " + field);
    }
  }

  @Override
  public Optional<SSZBasicAccessor> resolveBasicAccessor(SSZField field) {
    Object accessor = getAccessorFromAnnotation(field);
    if (accessor instanceof SSZBasicAccessor) {
      return Optional.of((SSZBasicAccessor) accessor);
    }

    Class<?> type = field.getRawClass();
    boolean subclassCodec = false;
    if (!SubclassCodec.getSerializableClass(type).equals(type)) {
      type = SubclassCodec.getSerializableClass(type);
      subclassCodec = true;
    }

    SSZBasicAccessor codec = null;
    if (registeredClassHandlers.containsKey(type)) {
      List<CodecEntry> codecs = registeredClassHandlers.get(type);
      if (field.getExtraType() == null || field.getExtraType().isEmpty()) {
        codec = codecs.get(0).codec;
      } else {
        for (CodecEntry codecEntry : codecs) {
          if (codecEntry.types.contains(field.getExtraType())) {
            codec = codecEntry.codec;
            break;
          }
        }
      }
    }

    if (codec != null && subclassCodec) {
      codec = new SubclassCodec(codec);
    }

    return Optional.ofNullable(codec);
  }

  /**
   * Registers codecs to be used for
   *
   * @param codec Codec able to encode/decode of specific class/types
   */
  public void registerCodec(SSZBasicAccessor codec) {
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
    SSZBasicAccessor codec;
    Set<String> types;

    public CodecEntry(SSZBasicAccessor codec, Set<String> types) {
      this.codec = codec;
      this.types = types;
    }
  }
}
