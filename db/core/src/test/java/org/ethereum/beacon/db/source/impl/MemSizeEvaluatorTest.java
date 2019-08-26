package org.ethereum.beacon.db.source.impl;

import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import static org.assertj.core.api.Assertions.assertThat;

public class MemSizeEvaluatorTest {

  @Test
  public void bytesValueEvaluator() {
    long evaluatedSize =
        MemSizeEvaluators.BytesValueEvaluator.apply(BytesValue.wrap(new byte[1000]));
    System.out.println("Evaluated size: " + evaluatedSize);
    assertThat(evaluatedSize).isGreaterThan(1000);
  }
}
