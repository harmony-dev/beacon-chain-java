package org.ethereum.beacon.db.util;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class AutoCloseableLockTest {

    @Tag("FIX")
    @Test
    void testInvalidCreation() {
        assertThatThrownBy(() -> new AutoCloseableLock(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AutoCloseableLock.wrap(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("creationArgumentsProvider")
    void testValidCreation(Lock lock) {
        final AutoCloseable result = new AutoCloseableLock(lock);
        assertThat(result).isNotNull();
    }

    private static Stream<Arguments> creationArgumentsProvider() {
        return Stream.of(
                Arguments.of(new ReentrantLock()),
                Arguments.of(new ReentrantReadWriteLock().writeLock()),
                Arguments.of(new ReentrantReadWriteLock().readLock())
        );
    }

    @ParameterizedTest
    @MethodSource("creationArgumentsProvider")
    void wrap(Lock lock) {
        final AutoCloseable result = AutoCloseableLock.wrap(lock);
        assertThat(result).isNotNull();
    }

    private boolean locked;
    private boolean unlocked;

    @Test
    void testLockUnlockCloseCreation() {
        locked = false;
        unlocked = false;

        final Lock lock = new Lock() {
            @Override
            public void lock() {
                locked = true;
            }

            @Override
            public void lockInterruptibly() {

            }

            @Override
            public boolean tryLock() {
                return false;
            }

            @Override
            public boolean tryLock(long time, @NotNull TimeUnit unit) {
                return false;
            }

            @Override
            public void unlock() {
                unlocked = true;
            }

            @NotNull
            @Override
            public Condition newCondition() {
                return null;
            }
        };

        final AutoCloseableLock result = new AutoCloseableLock(lock);
        assertThat(locked).isFalse();
        result.lock();
        assertThat(locked).isTrue();

        assertThat(unlocked).isFalse();
        result.close();
        assertThat(unlocked).isTrue();

        unlocked = false;
        assertThat(unlocked).isFalse();
        result.unlock();
        assertThat(unlocked).isTrue();
    }
}
