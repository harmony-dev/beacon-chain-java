package org.ethereum.beacon.db.flush;

import org.ethereum.beacon.db.source.WriteBuffer;
import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class InstantFlusherTest {

    private boolean doFlush;
    private boolean datasourceFlush;
    private boolean instantFlushed;

    @BeforeEach
    void setUp() {
        doFlush = false;
        datasourceFlush = false;
        instantFlushed = false;
    }

    @Test
    @Tag("FIX")
    void testInvalidInstanceCreation() {
        assertThatThrownBy(() -> new InstantFlusher(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void flush(boolean upstreamFlush) {
        final InstantFlusher instantFlusher = new InstantFlusher(new WriteBuffer(new HashMapDataSource<String, String>() {
            @Override
            public void flush() {
                datasourceFlush = true;
            }
        }, upstreamFlush) {
            @Override
            public void doFlush() {
                doFlush = true;
            }
        }) {
            @Override
            public void flush() {
                instantFlushed = true;
            }
        };

        assertThat(instantFlusher).isNotNull();

        assertThat(doFlush).isFalse();
        assertThat(datasourceFlush).isFalse();
        assertThat(instantFlushed).isFalse();
        instantFlusher.flush();
        assertThat(doFlush).isFalse();
        assertThat(datasourceFlush).isFalse();
        assertThat(instantFlushed).isTrue();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void commit(boolean upstreamFlush) {
        final InstantFlusher instantFlusher = new InstantFlusher(new WriteBuffer(new HashMapDataSource<String, String>() {
            @Override
            public void flush() {
                datasourceFlush = true;
            }
        }, upstreamFlush) {
            @Override
            public void doFlush() {
                doFlush = true;
            }
        }) {
            @Override
            public void flush() {
                instantFlushed = true;
            }
        };

        assertThat(instantFlusher).isNotNull();

        assertThat(doFlush).isFalse();
        assertThat(datasourceFlush).isFalse();
        assertThat(instantFlushed).isFalse();
        instantFlusher.commit();
        assertThat(doFlush).isTrue();
        assertThat(datasourceFlush).isEqualTo(upstreamFlush);
        assertThat(instantFlushed).isFalse();
    }
}
