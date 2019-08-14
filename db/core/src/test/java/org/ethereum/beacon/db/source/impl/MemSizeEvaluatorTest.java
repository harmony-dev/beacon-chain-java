package org.ethereum.beacon.db.source.impl;

import org.junit.Assert;
import org.junit.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

public class MemSizeEvaluatorTest {

  @Test
  public void bytesValueEvaluator() {
    long evaluatedSize =
        MemSizeEvaluators.BytesValueEvaluator.apply(BytesValue.wrap(new byte[1000]));
    System.out.println("Evaluated size: " + evaluatedSize);
    Assert.assertTrue(evaluatedSize > 1000);
  }
}
