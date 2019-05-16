package org.ethereum.beacon.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;

public class Utils {

  public static <A, B> Function<Optional<A>, Stream<B>> optionalFlatMap(Function<A, B> func) {
    return opt -> opt.map(a -> Stream.of(func.apply(a))).orElseGet(Stream::empty);
  }

  public static <A, B> Function<A, Stream<B>> nullableFlatMap(Function<A, B> func) {
    return n -> n != null ? Stream.of(func.apply(n)) : Stream.empty();
  }

  public static <C> void futureForward(CompletableFuture<C> result, CompletableFuture<C> forwardToFuture) {
    result.whenComplete(
        (res, t) -> {
          if (t != null) {
            forwardToFuture.completeExceptionally(t);
          } else {
            forwardToFuture.complete(res);
          }
        });
  }

  public static <C> Set<C> newLRUSet(int size) {
    return Collections.newSetFromMap(new LinkedHashMap<C, Boolean>() {
      protected boolean removeEldestEntry(Map.Entry<C, Boolean> eldest) {
        return size() > size;
      }
    });
  }
}
