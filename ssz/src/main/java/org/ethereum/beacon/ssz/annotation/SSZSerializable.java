package org.ethereum.beacon.ssz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
  /**
   * If set, uses following method to get encoded class data
   *
   * <p>When restoring the instance uses constructor with only "encode" method return type input
   * parameter.
   *
   * @return method for encoding
   */
  String encode() default "";
}
