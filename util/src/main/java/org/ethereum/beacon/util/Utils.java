package org.ethereum.beacon.util;

import java.math.BigInteger;
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

  public static <C> void futureForward(
      CompletableFuture<C> result, CompletableFuture<C> forwardToFuture) {
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
    return Collections.newSetFromMap(
        new LinkedHashMap<C, Boolean>() {
          protected boolean removeEldestEntry(Map.Entry<C, Boolean> eldest) {
            return size() > size;
          }
        });
  }

  /**
   * @return byte array representation of BigInteger for unsigned numeric
   *     <p>{@link BigInteger#toByteArray()} adds a bit for the sign. If you work with unsigned
   *     numerics it's always a 0. But if an integer uses exactly 8-some bits, sign bit will add an
   *     extra 0 byte to the result, which could broke some things. This method removes this
   *     redundant prefix byte when extracting byte array from BigInteger
   */
  public static byte[] extractBytesFromUnsignedBigInt(BigInteger bigInteger) {
    byte[] bigIntBytes = bigInteger.toByteArray();
    byte[] res;
    if (bigIntBytes[0] == 0) {
      res = new byte[bigIntBytes.length - 1];
      System.arraycopy(bigIntBytes, 1, res, 0, res.length);
    } else {
      res = bigIntBytes;
    }

    return res;
  }
}
