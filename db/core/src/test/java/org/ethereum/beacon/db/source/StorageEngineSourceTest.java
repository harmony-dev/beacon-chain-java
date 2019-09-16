package org.ethereum.beacon.db.source;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.pegasys.artemis.util.bytes.BytesValue;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class StorageEngineSourceTest {

    private StorageEngineSource<String> storageEngineSource;
    private boolean opened;

    @BeforeEach
    void setUp() {
        final Map<BytesValue, String> store = new ConcurrentHashMap<>();
        storageEngineSource = new StorageEngineSource<String>() {
            @Override
            public void open() {
                opened = true;
            }

            @Override
            public void close() {
                opened = false;
            }

            @Override
            public void batchUpdate(Map<BytesValue, String> updates) {
                assertTrue(opened);
                store.putAll(updates);
            }

            @Override
            public Optional<String> get(@Nonnull BytesValue key) {
                assertTrue(opened);
                return Optional.ofNullable(store.get(key));
            }

            @Override
            public void put(@Nonnull BytesValue key, @Nonnull String value) {
                assertTrue(opened);
                store.put(key, value);
            }

            @Override
            public void remove(@Nonnull BytesValue key) {
                assertTrue(opened);
                store.remove(key);
            }

            @Override
            public void flush() {
                assertTrue(opened);
                store.put(wrap("test_flush"), "test_flush");
            }
        };

        assertThat(storageEngineSource).isNotNull();
    }

    @Test
    void testBasicOperations(){
        storageEngineSource.open();
        storageEngineSource.put(wrap("key_0"), "value_0");
        assertThat(storageEngineSource.get(wrap("key_0")).get()).isEqualTo("value_0");

        Map<BytesValue, String> batch = new HashMap<>();
        batch.put(wrap("key_2"), "value_2");
        batch.put(wrap("key_3"), "value_3");
        batch.put(wrap("key_4"), "value_4");

        storageEngineSource.batchUpdate(batch);

        assertThat("value_2").isEqualTo(storageEngineSource.get(wrap("key_2")).get());
        assertThat("value_3").isEqualTo(storageEngineSource.get(wrap("key_3")).get());
        assertThat("value_4").isEqualTo(storageEngineSource.get(wrap("key_4")).get());

        batch.put(wrap("key_1"), null);
        assertThatThrownBy(() -> storageEngineSource.batchUpdate(batch)).isInstanceOf(NullPointerException.class);
        assertThat(storageEngineSource.get(wrap("key_1"))).isNotPresent();

        storageEngineSource.remove(wrap("key_3"));
        assertThat(storageEngineSource.get(wrap("key_3"))).isNotPresent();

        storageEngineSource.close();
        storageEngineSource.open();

        assertThat(storageEngineSource.get(wrap("key_1"))).isNotPresent();
        assertThat("value_2").isEqualTo(storageEngineSource.get(wrap("key_2")).get());
        assertThat(storageEngineSource.get(wrap("key_3"))).isNotPresent();
        assertThat("value_4").isEqualTo(storageEngineSource.get(wrap("key_4")).get());
        assertThat(storageEngineSource.get(wrap("key_5"))).isNotPresent();

        storageEngineSource.close();
    }

    private BytesValue wrap(String value) {
        return BytesValue.wrap(value.getBytes());
    }
}