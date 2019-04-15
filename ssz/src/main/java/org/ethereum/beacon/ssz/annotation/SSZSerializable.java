package org.ethereum.beacon.ssz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.ethereum.beacon.ssz.access.SSZCodec;
import org.ethereum.beacon.ssz.access.SSZContainerAccessor;
import org.ethereum.beacon.ssz.access.SSZListAccessor;

/**
 * Identifies class that is SSZ serializable
 *
 * <p>Required to mark SSZ compatible class
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface SSZSerializable {

  interface Void {}
  interface VoidListAccessor extends Void, SSZListAccessor {}
  interface VoidContainerAccessor extends Void, SSZContainerAccessor {}
  interface VoidBasicCodec extends Void, SSZCodec {}

  /**
   * Tells the Serializer that this class should be serialized as <code>serializeAs</code> class
   *
   * - This class should be an ancestor of the <code>serializeAs</code> class
   * - This class should have a public constructor that takes single <code>serializeAs</code>
   *   class instance as an argument
   */
  Class<?> serializeAs() default void.class;

  /**
   * Call this method to get target serializable instance.
   * This is handy for wrapper classes which delegate all calls to a wrapped instance which
   * is serializable
   */
  String instanceGetter() default "";

  Class<? extends SSZCodec> basicAccessor() default VoidBasicCodec.class;

  Class<? extends SSZListAccessor> listAccessor() default VoidListAccessor.class;

  Class<? extends SSZContainerAccessor> containerAccessor() default VoidContainerAccessor.class;
}
