package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractLinkedDataSourceTest {

    private final String TEST_KEY = "test_key";
    private final String TEST_VALUE = "test_value";
    private final String TEST_FLUSH = "flush";
    private final String TEST_DO_FLUSH = "do_flush";

    private AbstractLinkedDataSource<String, String, String, String> dataSource;

    @BeforeEach
    void setUp() {
        final HashMapDataSource<String, String> upstream = new HashMapDataSource<>();
        dataSource = new AbstractLinkedDataSource<String, String, String, String>(upstream) {
            public Optional<String> get(@Nonnull String key) {
                return getUpstream().get(key);
            }

            public void put(@Nonnull String key, @Nonnull String value) {
                getUpstream().put(key, value);
            }

            public void remove(@Nonnull String key) {
                getUpstream().remove(key);
            }
        };

        assertThat(dataSource.getUpstream()).isEqualTo(upstream);
    }

    @Test
    void testInvalidSourceCreation() {
        assertThatThrownBy(() -> new AbstractLinkedDataSource(null) {
            public Optional get(@Nonnull Object key) {
                return Optional.empty();
            }

            public void put(@Nonnull Object key, @Nonnull Object value) {

            }

            public void remove(@Nonnull Object key) {

            }
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    void testGetPutRemoveUpstreamFlush() {
        final HashMapDataSource<String, String> upstream = new HashMapDataSource<String, String>() {
            public void flush() {
                getStore().put(TEST_FLUSH, TEST_FLUSH);
            }
        };
        dataSource = new AbstractLinkedDataSource<String, String, String, String>(upstream, true) {
            public Optional<String> get(@Nonnull String key) {
                return getUpstream().get(key);
            }

            public void put(@Nonnull String key, @Nonnull String value) {
                getUpstream().put(key, value);
            }

            public void remove(@Nonnull String key) {
                getUpstream().remove(key);
            }

            protected void doFlush() {
                getUpstream().put(TEST_DO_FLUSH, TEST_DO_FLUSH);
            }
        };

        assertThat(dataSource.getUpstream()).isEqualTo(upstream);

        dataSource.put(TEST_KEY, TEST_VALUE);
        assertThat(dataSource.get(TEST_KEY)).isPresent().hasValue(TEST_VALUE);

        dataSource.remove(TEST_KEY);
        assertThat(dataSource.get(TEST_KEY)).isNotPresent();

        dataSource.flush();
        assertThat(dataSource.get(TEST_FLUSH)).isPresent().hasValue(TEST_FLUSH);
        assertThat(dataSource.get(TEST_DO_FLUSH)).isPresent().hasValue(TEST_DO_FLUSH);
    }

    @Test
    void testFlushIsFalse() {
        dataSource = new AbstractLinkedDataSource<String, String, String, String>(new HashMapDataSource<String, String>() {
            public void flush() {
                getStore().put(TEST_FLUSH, TEST_FLUSH);
            }
        }) {
            public Optional<String> get(@Nonnull String key) {
                return getUpstream().get(key);
            }

            public void put(@Nonnull String key, @Nonnull String value) {
            }

            public void remove(@Nonnull String key) {
            }

            protected void doFlush() {
                getUpstream().put(TEST_DO_FLUSH, TEST_DO_FLUSH);
            }
        };

        dataSource.flush();
        assertThat(dataSource.get(TEST_FLUSH)).isNotPresent();
        assertThat(dataSource.get(TEST_DO_FLUSH)).isPresent().hasValue(TEST_DO_FLUSH);
    }
}
