package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;


class BufferSizeObserverTest {

    private final String TEST_KEY = "TEST_KEY";
    private final String TEST_VALUE = "TEST_VALUE";
    private WriteBuffer<String, String> buffer;
    private WriteBuffer<String, String> commitTrack;
    private BufferSizeObserver observer;


    @BeforeEach
    void setUp() {
        buffer = new WriteBuffer<>(new HashMapDataSource<>(), true);
        commitTrack = new WriteBuffer<>(new HashMapDataSource<>(), true);
        observer = new BufferSizeObserver(buffer, commitTrack, Long.MIN_VALUE);
        assertThat(observer).isNotNull();
        observer = new BufferSizeObserver(buffer, commitTrack, Long.MAX_VALUE);
        assertThat(observer).isNotNull();
    }

    @Test
    void testBufferSizeObserverCreation() {
        assertThat(new BufferSizeObserver(null, null, Long.MIN_VALUE)).isNotNull();
        assertThat(new BufferSizeObserver(null, new WriteBuffer(new HashMapDataSource(), true), Long.MIN_VALUE)).isNotNull();
        assertThat(new BufferSizeObserver(new WriteBuffer(new HashMapDataSource(), true), null, Long.MIN_VALUE)).isNotNull();

        assertThat(new BufferSizeObserver(null, null, Long.MAX_VALUE)).isNotNull();
        assertThat(new BufferSizeObserver(null, new WriteBuffer(new HashMapDataSource(), true), Long.MAX_VALUE)).isNotNull();
        assertThat(new BufferSizeObserver(new WriteBuffer(new HashMapDataSource(), true), null, Long.MAX_VALUE)).isNotNull();
    }


    @Test
    void testCreate() {
        String TEST_KEY_0 = "TEST_KEY_0";
        String TEST_VALUE_0 = "TEST_VALUE_0";
        String TEST_KEY_1 = "TEST_KEY_1";
        String TEST_VALUE_1 = "TEST_VALUE_1";
        String TEST_KEY_2 = "TEST_KEY_2";
        String TEST_VALUE_2 = "TEST_VALUE_2";
        String TEST_KEY_3 = "TEST_KEY_3";
        String TEST_VALUE_3 = "TEST_VALUE_3";
        String TEST_KEY_4 = "TEST_KEY_4";
        String TEST_VALUE_4 = "TEST_VALUE_4";
        String TEST_KEY_5 = "TEST_KEY_5";
        String TEST_VALUE_5 = "TEST_VALUE_5";

        buffer = new WriteBuffer<>(new HashMapDataSource<>(), false);
        observer = BufferSizeObserver.create(buffer, Long.MIN_VALUE);
        buffer.put(TEST_KEY_0, TEST_VALUE_0);
        buffer.put(TEST_KEY_1, TEST_VALUE_1);
        buffer.put(TEST_KEY_2, TEST_VALUE_2);
        buffer.put(TEST_KEY_3, TEST_VALUE_3);
        buffer.put(TEST_KEY_4, TEST_VALUE_4);

        assertThatThrownBy(() -> buffer.put(null, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> buffer.put("NOT_NULL", null)).isInstanceOf(NullPointerException.class);

        assertThat(buffer.get(TEST_KEY_0)).isPresent();
        assertThat(buffer.getCacheEntry(TEST_KEY_0)).isPresent();
        assertTrue(buffer.evaluateSize() >= Long.MIN_VALUE, "Needed db flush");

        observer.flush();
        assertThat(buffer.getCacheEntry(TEST_KEY_0)).isNotPresent();
        assertThat(buffer.getUpstream().get(TEST_KEY_0)).isPresent();

        buffer.put(TEST_KEY_5, TEST_VALUE_5);

        assertThat(buffer.getUpstream().get(TEST_KEY_5)).isNotPresent();
        assertThat(buffer.getCacheEntry(TEST_KEY_5)).isPresent();

        observer.commit();
        assertThat(buffer.getUpstream().get(TEST_KEY_5)).isPresent();
        assertThat(buffer.getUpstream().get(TEST_KEY_5)).hasValue(TEST_VALUE_5);

    }

    @Test
    void testFlush() {
        buffer.put(TEST_KEY, TEST_VALUE);
        assertThat(buffer.get(TEST_KEY)).isPresent();
        assertThat(buffer.get(TEST_KEY)).hasValue(TEST_VALUE);

        observer.flush();
        assertThat(buffer.getUpstream().get(TEST_KEY)).isPresent();
        assertThat(buffer.getUpstream().get(TEST_KEY)).hasValue(TEST_VALUE);

        buffer.getUpstream().remove(TEST_KEY);
        assertThat(buffer.get(TEST_KEY)).isNotPresent();
        assertThat(buffer.getUpstream().get(TEST_KEY)).isNotPresent();

    }

    @Test
    void testCommit() {
        commitTrack.put(TEST_KEY, TEST_VALUE);
        assertThat(commitTrack.get(TEST_KEY)).isPresent();
        assertThat(commitTrack.get(TEST_KEY)).hasValue(TEST_VALUE);
        assertThat(commitTrack.getUpstream().get(TEST_KEY)).isNotPresent();

        observer.commit();
        assertThat(commitTrack.get(TEST_KEY)).isPresent();
        assertThat(commitTrack.getUpstream().get(TEST_KEY)).isPresent();
        assertThat(commitTrack.getUpstream().get(TEST_KEY)).hasValue(TEST_VALUE);

        commitTrack.getUpstream().remove(TEST_KEY);
        assertThat(commitTrack.get(TEST_KEY)).isNotPresent();
        assertThat(commitTrack.getUpstream().get(TEST_KEY)).isNotPresent();
        assertThat(buffer.getUpstream().get(TEST_KEY)).isNotPresent();
    }
}
