package org.ethereum.beacon.db.util;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        final AutoCloseableLock result = new AutoCloseableLock(lock);
        result
                .lock()
                .unlock();
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
        final AutoCloseableLock result = AutoCloseableLock.wrap(lock);
        result
                .lock()
                .unlock();
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
