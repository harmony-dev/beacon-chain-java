package org.ethereum.beacon.core.types;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.Test;

public class IteratorTest {

  @Test
  public void test1() {
    SlotNumber s1 = SlotNumber.of(10);
    SlotNumber s2 = SlotNumber.of(11);

    Iterator<SlotNumber> iterator = s1.iterateTo(s2).iterator();
    Assert.assertTrue(iterator.hasNext());
    Assert.assertEquals(SlotNumber.of(10), iterator.next());
    Assert.assertFalse(iterator.hasNext());
    try {
      iterator.next();
      Assert.fail();
    } catch (NoSuchElementException e) {
      // expected
    }
  }

  @Test
  public void test2() {
    SlotNumber s1 = SlotNumber.of(10);
    SlotNumber s2 = SlotNumber.of(10);

    Iterator<SlotNumber> iterator = s1.iterateTo(s2).iterator();
    Assert.assertFalse(iterator.hasNext());
    try {
      iterator.next();
      Assert.fail();
    } catch (NoSuchElementException e) {
      // expected
    }
  }

  @Test
  public void test3() {
    SlotNumber s1 = SlotNumber.of(11);
    SlotNumber s2 = SlotNumber.of(10);

    Iterator<SlotNumber> iterator = s1.iterateTo(s2).iterator();
    Assert.assertFalse(iterator.hasNext());
    try {
      iterator.next();
      Assert.fail();
    } catch (NoSuchElementException e) {
      // expected
    }
  }
}

