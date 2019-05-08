package org.ethereum.beacon.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class Utils {

  public static <A, B> Function<Optional<A>, Stream<B>> optionalFlatMap(Function<A, B> func) {
    return opt -> opt.map(a -> Stream.of(func.apply(a))).orElseGet(Stream::empty);
  }

  public static <A, B> Function<A, Stream<B>> nullableFlatMap(Function<A, B> func) {
    return n -> n != null ? Stream.of(func.apply(n)) : Stream.empty();
  }
}
