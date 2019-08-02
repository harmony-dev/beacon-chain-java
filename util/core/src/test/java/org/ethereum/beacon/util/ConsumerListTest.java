package org.ethereum.beacon.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConsumerListTest {
  @Test
  public void test() {
    final List<Integer> list = new ArrayList<>();
    Consumer<Integer> consumer = list::add;
    ConsumerList<Integer> consumerList = ConsumerList.create(5, consumer);
    assertEquals(0, consumerList.size());
    consumerList.add(1);
    consumerList.add(2);
    consumerList.add(3);
    consumerList.add(4);
    consumerList.add(5);
    consumerList.add(6);
    assertEquals(1, list.size());
    assertEquals(Integer.valueOf(1), list.get(0));
    assertEquals(5, consumerList.size());
    assertEquals(5, consumerList.maxSize);
    List<Integer> three = new ArrayList<>();
    three.add(7);
    three.add(8);
    three.add(9);
    consumerList.addAll(three);
    assertEquals(4, list.size());
    assertEquals(Integer.valueOf(2), list.get(1));
    assertEquals(Integer.valueOf(3), list.get(2));
    assertEquals(Integer.valueOf(4), list.get(3));
    assertTrue(consumerList.contains(5));
    assertTrue(consumerList.contains(6));
    assertTrue(consumerList.contains(7));
    assertTrue(consumerList.contains(8));
    assertTrue(consumerList.contains(9));
  }
}
