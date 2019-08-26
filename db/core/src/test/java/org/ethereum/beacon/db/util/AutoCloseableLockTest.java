package org.ethereum.beacon.db.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

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

    @ParameterizedTest
    @MethodSource("creationArgumentsProvider")
    void testLockCloseCreation(Lock lock) {
        //TODO: how to test?
        final AutoCloseableLock result = new AutoCloseableLock(lock);
        result.lock();
        result.close();
    }
}
