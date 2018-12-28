package net.consensys.cava.ssz.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Identifies class that is SSZ serializable</p>
 */
@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface SSZSerializable {
  /**
   * <p>If set, uses following method to get encoded class data</p>
   *
   * <p>When restoring the instance uses constructor with only "encode" method
   * return type input parameter.</p>
   * @return method for encoding
   */
  String encode() default "";
}
