package org.ethereum.beacon.db.flush;

import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseFlusherTest {

    private DatabaseFlusher databaseFlusher;

    private boolean flushed;
    private boolean committed;

    @BeforeEach
    void setUp() {
        flushed = false;
        committed = false;

        databaseFlusher = new DatabaseFlusher() {
            @Override
            public void flush() {
                flushed = true;
            }

            @Override
            public void commit() {
                committed = true;
            }
        };
    }

    @Test
    void flush() {
        assertThat(flushed).isFalse();
        databaseFlusher.flush();
        assertThat(flushed).isTrue();
    }

    @Test
    void commit() {
        assertThat(committed).isFalse();
        databaseFlusher.commit();
        assertThat(committed).isTrue();
    }
}
