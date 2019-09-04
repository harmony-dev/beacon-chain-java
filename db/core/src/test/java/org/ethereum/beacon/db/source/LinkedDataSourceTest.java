package org.ethereum.beacon.db.source;

import org.ethereum.beacon.db.source.impl.HashMapDataSource;
import org.junit.jupiter.api.*;

import javax.annotation.Nonnull;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class LinkedDataSourceTest {

    private LinkedDataSource<String, String, String, String> dataSource;

    @BeforeEach
    void setUp() {
        dataSource = new LinkedDataSource<String, String, String, String>() {
            public Optional<String> get(@Nonnull String key) {
                return Optional.empty();
            }

            public void put(@Nonnull String key, @Nonnull String value) {

            }

            public void remove(@Nonnull String key) {

            }

            public void flush() {

            }

            @Nonnull
            public DataSource<String, String> getUpstream() {
                return null;
            }
        };

        assertThat(dataSource).isNotNull();
    }

    @Test
    void testSetUpstream() {
        assertThatThrownBy(() -> dataSource.setUpstream(new HashMapDataSource<>())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testGetNullUpstream() {
        assertThat(dataSource.getUpstream()).isNull();
    }

    @Test
    void testGetUpstream() {
        dataSource = new LinkedDataSource<String, String, String, String>() {
            public Optional<String> get(@Nonnull String key) {
                return Optional.empty();
            }

            public void put(@Nonnull String key, @Nonnull String value) {

            }

            public void remove(@Nonnull String key) {

            }

            public void flush() {

            }

            @Nonnull
            public DataSource<String, String> getUpstream() {
                return new HashMapDataSource<>();
            }
        };
        assertThat(dataSource).isNotNull();
        assertThat(dataSource.getUpstream()).isNotNull();
    }
}
