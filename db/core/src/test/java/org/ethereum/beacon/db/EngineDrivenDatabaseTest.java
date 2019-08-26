package org.ethereum.beacon.db;

import org.ethereum.beacon.db.source.*;
import org.ethereum.beacon.db.source.impl.MemSizeEvaluators;
import org.junit.jupiter.api.*;
import tech.pegasys.artemis.util.bytes.*;
import tech.pegasys.artemis.util.uint.UInt64;

import javax.annotation.Nonnull;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class EngineDrivenDatabaseTest {

  @Test
  public void generalCasesAreCorrect() {
    TestStorageSource engineSource = new TestStorageSource();
    EngineDrivenDatabase db = EngineDrivenDatabase.createWithInstantFlusher(engineSource);

    DataSource<BytesValue, BytesValue> storage = db.createStorage("test");

    storage.put(wrap("ONE"), wrap("FIRST"));
    storage.put(wrap("TWO"), wrap("SECOND"));

    assertTrue(engineSource.source.isEmpty());
    assertThat(wrap("FIRST")).isEqualTo(storage.get(wrap("ONE")).get());
    assertThat(wrap("SECOND")).isEqualTo(storage.get(wrap("TWO")).get());
    assertThat(storage.get(wrap("THREE"))).isNotPresent();

    db.commit();

    assertThat(db.getWriteBuffer().getCacheEntry(wrap("ONE"))).isNotPresent();
    assertThat(db.getWriteBuffer().getCacheEntry(wrap("TWO"))).isNotPresent();
    assertThat(0L).isEqualTo(db.getWriteBuffer().evaluateSize());

    assertTrue(engineSource.source.containsValue(wrap("FIRST")));
    assertTrue(engineSource.source.containsValue(wrap("SECOND")));
    assertThat(wrap("FIRST")).isEqualTo(storage.get(wrap("ONE")).get());
    assertThat(wrap("SECOND")).isEqualTo(storage.get(wrap("TWO")).get());
    assertThat(storage.get(wrap("THREE"))).isNotPresent();

    storage.remove(wrap("SECOND"));
    storage.put(wrap("THREE"), wrap("THIRD"));
    storage.remove(wrap("TWO"));

    assertTrue(engineSource.source.containsValue(wrap("FIRST")));
    assertTrue(engineSource.source.containsValue(wrap("SECOND")));
    assertFalse(engineSource.source.containsValue(wrap("THIRD")));

    assertThat(wrap("FIRST")).isEqualTo(storage.get(wrap("ONE")).get());
    assertThat(storage.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("THIRD")).isEqualTo(storage.get(wrap("THREE")).get());

    db.commit();

    assertTrue(engineSource.source.containsValue(wrap("FIRST")));
    assertFalse(engineSource.source.containsValue(wrap("SECOND")));
    assertTrue(engineSource.source.containsValue(wrap("THIRD")));

    assertThat(wrap("FIRST")).isEqualTo(storage.get(wrap("ONE")).get());
    assertThat(storage.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("THIRD")).isEqualTo(storage.get(wrap("THREE")).get());
  }

  @Test
  public void multipleStorageCase() {
    TestStorageSource engineSource = new TestStorageSource();
    EngineDrivenDatabase db = EngineDrivenDatabase.createWithInstantFlusher(engineSource);

    DataSource<BytesValue, BytesValue> uno = db.createStorage("uno");
    DataSource<BytesValue, BytesValue> dos = db.createStorage("dos");

    uno.put(wrap("ONE"), wrap("FIRST"));
    uno.put(wrap("TWO"), wrap("SECOND"));
    dos.put(wrap("TWO"), wrap("SECOND"));
    uno.put(wrap("THREE"), wrap("UNO_THIRD"));
    dos.put(wrap("FOUR"), wrap("DOS_FOURTH"));

    db.commit();
    assertThat(wrap("FIRST")).isEqualTo(uno.get(wrap("ONE")).get());
    assertThat(wrap("SECOND")).isEqualTo(uno.get(wrap("TWO")).get());
    assertThat(wrap("SECOND")).isEqualTo(dos.get(wrap("TWO")).get());
    assertThat(wrap("UNO_THIRD")).isEqualTo(uno.get(wrap("THREE")).get());
    assertThat(wrap("DOS_FOURTH")).isEqualTo(dos.get(wrap("FOUR")).get());
    assertThat(uno.get(wrap("FOUR"))).isNotPresent();
    assertThat(dos.get(wrap("ONE"))).isNotPresent();
    assertThat(dos.get(wrap("THREE"))).isNotPresent();

    uno.remove(wrap("TWO"));
    dos.put(wrap("THREE"), wrap("DOS_THIRD"));

    assertThat(uno.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("DOS_THIRD")).isEqualTo(dos.get(wrap("THREE")).get());
    assertThat(wrap("UNO_THIRD")).isEqualTo(uno.get(wrap("THREE")).get());

    db.commit();
    assertThat(uno.get(wrap("TWO"))).isNotPresent();
    assertThat(wrap("DOS_THIRD")).isEqualTo(dos.get(wrap("THREE")).get());
    assertThat(wrap("UNO_THIRD")).isEqualTo(uno.get(wrap("THREE")).get());

    dos.remove(wrap("FOUR"));
    uno.put(wrap("FOUR"), wrap("UNO_FOURTH"));
    assertThat(wrap("UNO_FOURTH")).isEqualTo(uno.get(wrap("FOUR")).get());
    assertThat(dos.get(wrap("FOUR"))).isNotPresent();

    db.commit();
    assertThat(wrap("UNO_FOURTH")).isEqualTo(uno.get(wrap("FOUR")).get());
    assertThat(dos.get(wrap("FOUR"))).isNotPresent();
  }

  @Test
  public void checkBufferSizeFlusher() {
    TestStorageSource engineSource = new TestStorageSource();
    EngineDrivenDatabase db = EngineDrivenDatabase.create(engineSource, 512);

    Random rnd = new Random();

    DataSource<BytesValue, BytesValue> storage = db.createStorage("test");
    storage.put(wrap("ONE"), Bytes32.random(rnd));
    storage.put(wrap("TWO"), Bytes32.random(rnd));

    db.commit();

    // no flush is expected
    assertTrue(engineSource.source.isEmpty());

    storage.put(wrap("THREE"), Bytes32.random(rnd));
    storage.put(wrap("FOUR"), Bytes32.random(rnd));

    // should be flushed now
    db.commit();
    assertThat(4).isEqualTo(engineSource.source.size());
    assertThat(0L).isEqualTo(db.getWriteBuffer().evaluateSize());
    assertThat(db.getWriteBuffer().getCacheEntry(wrap("ONE"))).isNotPresent();
    assertThat(db.getWriteBuffer().getCacheEntry(wrap("TWO"))).isNotPresent();
    assertThat(db.getWriteBuffer().getCacheEntry(wrap("THREE"))).isNotPresent();
    assertThat(db.getWriteBuffer().getCacheEntry(wrap("FOUR"))).isNotPresent();

    storage.put(wrap("FIVE"), Bytes32.random(rnd));
    storage.put(wrap("SIX"), Bytes32.random(rnd));
    assertThat(
            4 * MemSizeEvaluators.BytesValueEvaluator.apply(Bytes32.random(rnd))).isEqualTo(
        db.getWriteBuffer().evaluateSize());

    storage.remove(wrap("FIVE"));

    assertThat(
            2 * MemSizeEvaluators.BytesValueEvaluator.apply(Bytes32.random(rnd))).isEqualTo(
        db.getWriteBuffer().evaluateSize());
  }

  @Test
  @Disabled
  public void checkWithConcurrentAccessTake1() throws InterruptedException {
    TestStorageSource engineSource = new TestStorageSource();
    EngineDrivenDatabase db = EngineDrivenDatabase.createWithInstantFlusher(engineSource);

    DataSource<BytesValue, BytesValue> one = db.createStorage("one");
    DataSource<BytesValue, BytesValue> two = db.createStorage("two");

    Map<BytesValue, BytesValue> writtenToOne = Collections.synchronizedMap(new HashMap<>());
    Map<BytesValue, BytesValue> writtenToTwo = Collections.synchronizedMap(new HashMap<>());

    Thread w1 = spawnWriterThread(1, one, writtenToOne);
    Thread w2 = spawnWriterThread(2, one, writtenToOne);
    Thread r1 = spawnReaderThread(3, one);
    Thread r2 = spawnReaderThread(4, one);

    Thread w3 = spawnWriterThread(5, two, writtenToTwo);
    Thread w4 = spawnWriterThread(6, two, writtenToTwo);
    Thread r3 = spawnReaderThread(7, two);
    Thread r4 = spawnReaderThread(8, two);

    List<Thread> threads = Arrays.asList(w1, w2, w3, w4, r1, r2, r3, r4);
    threads.forEach(Thread::start);

    Random rnd = new Random();
    for (int i = 0; i < 10; i++) {
      Thread.sleep(Math.abs(rnd.nextLong() % 1000));
      db.commit();
    }

    for (Thread t : threads) {
      t.interrupt();
      t.join();
    }

    db.commit();

    Set<BytesValue> sourceValues = new HashSet<>(engineSource.source.values());
    Set<BytesValue> expectedValues = new HashSet<>(writtenToOne.values());
    expectedValues.addAll(writtenToTwo.values());

    assertThat(expectedValues).isEqualTo(sourceValues);
  }

  @Test
  @Disabled
  public void checkWithConcurrentAccessTake2() throws InterruptedException {
    TestStorageSource engineSource = new TestStorageSource();
    EngineDrivenDatabase db = EngineDrivenDatabase.createWithInstantFlusher(engineSource);

    DataSource<BytesValue, BytesValue> one = db.createStorage("one");
    DataSource<BytesValue, BytesValue> two = db.createStorage("two");

    Map<BytesValue, BytesValue> writtenToOne = Collections.synchronizedMap(new HashMap<>());
    Map<BytesValue, BytesValue> writtenToTwo = Collections.synchronizedMap(new HashMap<>());

    Thread w1 = spawnWriterThread(1, one, writtenToOne);
    Thread w2 = spawnWriterThread(2, one, writtenToOne);
    Thread m1 = spawnModifierThread(3, one);
    Thread m2 = spawnModifierThread(4, one);

    Thread w3 = spawnWriterThread(5, two, writtenToTwo);
    Thread w4 = spawnWriterThread(6, two, writtenToTwo);
    Thread m3 = spawnModifierThread(7, two);
    Thread m4 = spawnModifierThread(8, two);

    List<Thread> threads = Arrays.asList(w1, w2, w3, w4, m1, m2, m3, m4);
    threads.forEach(Thread::start);

    Random rnd = new Random();
    for (int i = 0; i < 10; i++) {
      Thread.sleep(Math.abs(rnd.nextLong() % 1000));
      db.commit();
    }

    for (Thread t : threads) {
      t.interrupt();
      t.join();
    }

    db.commit();

    Set<BytesValue> sourceValues = new HashSet<>(engineSource.source.values());
    Set<BytesValue> expectedValues = new HashSet<>(writtenToOne.values());
    expectedValues.addAll(writtenToTwo.values());

    assertTrue(expectedValues.size() >= sourceValues.size());
  }

  private Thread spawnWriterThread(
      long id,
      DataSource<BytesValue, BytesValue> source,
      Map<BytesValue, BytesValue> writtenValues) {
    Random rnd = new Random(id);
    return new Thread(
        () -> {
          long writesTotal = 0;
          while (!Thread.currentThread().isInterrupted()) {
            long key = rnd.nextLong() % 1000;
            Bytes32 value = Bytes32.random(rnd);
            source.put(UInt64.valueOf(key).toBytes8(), value);
            writtenValues.put(UInt64.valueOf(key).toBytes8(), value);

            writesTotal += 1;

            try {
              Thread.sleep(Math.abs(rnd.nextLong() % 10));
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }

          System.out.println(String.format("Writer %d: writes %d", id, writesTotal));
        });
  }

  private Thread spawnReaderThread(long id, DataSource<BytesValue, BytesValue> source) {
    Random rnd = new Random(id);
    return new Thread(
        () -> {
          long readsTotal = 0;
          long readsSuccessful = 0;

          while (!Thread.currentThread().isInterrupted()) {
            long key = rnd.nextLong() % 1000;
            Optional<BytesValue> val = source.get(UInt64.valueOf(key).toBytes8());
            readsTotal += 1;
            if (val.isPresent()) {
              readsSuccessful += 1;
            }

            try {
              Thread.sleep(Math.abs(rnd.nextLong() % 10));
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }

          System.out.println(
              String.format("Reader %d: reads %d, non null %d", id, readsTotal, readsSuccessful));
        });
  }

  private Thread spawnModifierThread(long id, DataSource<BytesValue, BytesValue> source) {
    Random rnd = new Random(id);
    return new Thread(
        () -> {
          long readsTotal = 0;
          long readsSuccessful = 0;
          long removalsTotal = 0;

          while (!Thread.currentThread().isInterrupted()) {
            long key = rnd.nextLong() % 1000;
            Optional<BytesValue> val = source.get(UInt64.valueOf(key).toBytes8());
            readsTotal += 1;
            if (val.isPresent()) {
              readsSuccessful += 1;
              if (Math.abs(rnd.nextLong() % 100) < 20) {
                removalsTotal += 1;
                source.remove(UInt64.valueOf(key).toBytes8());
              }
            }

            try {
              Thread.sleep(Math.abs(rnd.nextLong() % 10));
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }

          System.out.println(
              String.format(
                  "Modifier %d: reads %d, non null %d, removals %d",
                  id, readsTotal, readsSuccessful, removalsTotal));
        });
  }

  private BytesValue wrap(String value) {
    return BytesValue.wrap(value.getBytes());
  }

  private static class TestStorageSource implements StorageEngineSource<BytesValue> {

    private final HashMap<BytesValue, BytesValue> source = new HashMap<>();

    @Override
    public void open() {}

    @Override
    public void close() {}

    @Override
    public void batchUpdate(Map<BytesValue, BytesValue> updates) {
      source.putAll(updates);
    }

    @Override
    public Optional<BytesValue> get(@Nonnull BytesValue key) {
      return Optional.ofNullable(source.get(key));
    }

    @Override
    public void put(@Nonnull BytesValue key, @Nonnull BytesValue value) {
      source.put(key, value);
    }

    @Override
    public void remove(@Nonnull BytesValue key) {
      source.remove(key);
    }

    @Override
    public void flush() {}
  }
}
