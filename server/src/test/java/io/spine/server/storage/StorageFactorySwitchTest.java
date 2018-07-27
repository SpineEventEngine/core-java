/*
 * Copyright 2018, TeamDev. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.storage;

import io.spine.base.Environment;
import io.spine.core.BoundedContextName;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static io.spine.core.BoundedContextNames.newName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("StorageFactorySwitch should")
class StorageFactorySwitchTest {

    /**
     * Cached {@code Environment value} remembered {@linkplain #storeEnvironment() before all tests}
     * and restored {@linkplain #restoreEnvironment() after all tests}.
     */
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    // OK as we use the field after the initialization in storeEnvironment().
    private static Environment storedEnvironment;

    private boolean multitenant;
    private final BoundedContextName boundedContextName = newName(getClass().getSimpleName());

    private final Supplier<StorageFactory> testsSupplier = new Supplier<StorageFactory>() {
        @Override
        public StorageFactory get() {
            return InMemoryStorageFactory.newInstance(boundedContextName, multitenant);
        }
    };
    private final Supplier<StorageFactory> productionSupplier = new Supplier<StorageFactory>() {
        @Override
        public StorageFactory get() {
            return InMemoryStorageFactory.newInstance(boundedContextName, multitenant);
        }
    };

    private StorageFactorySwitch storageFactorySwitch;

    protected StorageFactorySwitchTest() {
        setMultitenant(false);
    }

    protected final void setMultitenant(boolean multitenant) {
        this.multitenant = multitenant;
    }

    @BeforeAll
    static void storeEnvironment() {
        storedEnvironment = Environment.getInstance()
                                       .createCopy();
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    // OK as we invoke after the initialization in storeEnvironment().
    @AfterAll
    static void restoreEnvironment() {
        Environment.getInstance()
                   .restoreFrom(storedEnvironment);
    }

    @BeforeEach
    void setUp() {
        storageFactorySwitch = StorageFactorySwitch.newInstance(boundedContextName, multitenant);
    }

    @AfterEach
    void cleanUp() {
        clearSwitch();
        Environment.getInstance()
                   .reset();
    }

    /**
     * Clears the `storageFactorySwitch` instance by nullifying fields.
     */
    private void clearSwitch() {
        storageFactorySwitch.reset();
    }

    @Test
    @DisplayName("return InMemoryStorageFactory in tests if test supplier was not set")
    void returnInMemoryByDefault() {
        StorageFactory storageFactory = storageFactorySwitch.get();

        assertNotNull(storageFactory);
        assertTrue(storageFactory instanceof InMemoryStorageFactory);
    }

    @Test
    @DisplayName("return custom test StorageFactory if supplier for tests was set")
    void returnCustomIfSet() {
        // This call avoids the racing conditions anomaly when running
        // the Gradle build from the console.
        // Despite the fact that we reset the switch state in `cleanUp()`, we still
        // get the cached value of the StorageFactory remembered by the switch in a previous test.
        // Having this call avoids the problem.
        storageFactorySwitch.reset();

        StorageFactory custom = mock(StorageFactory.class);

        storageFactorySwitch.init(testsSupplier, () -> custom);

        // These calls ensure that we're under the testing mode and we get the supplier for tests.
        assertTrue(Environment.getInstance()
                              .isTests());
        assertTrue(storageFactorySwitch.testsSupplier()
                                       .isPresent());

        // Get the StorageFactory from the switch.
        StorageFactory obtained = storageFactorySwitch.get();

        assertEquals(custom, obtained);
    }

    @SuppressWarnings("AccessOfSystemProperties") // OK for this test.
    @Test
    @DisplayName("throw ISE if production supplier is not present when in non-test mode")
    void throwOnNoProductionSupplier() {
        // Clear cached value for tests mode that may be left from the previous tests.
        Environment.getInstance()
                   .reset();
        // Pretend that we are not under tests for the `Environment`.
        Environment.getInstance()
                   .setToProduction();

        assertFalse(storageFactorySwitch.productionSupplier()
                                        .isPresent());

        assertThrows(IllegalStateException.class, () -> storageFactorySwitch.get());
    }

    @SuppressWarnings("CheckReturnValue") // ignore value of get() since we tests caching
    @Test
    @DisplayName("cache instance of StorageFactory in tests")
    void cacheInTest() {
        Supplier<StorageFactory> testingSupplier = spy(testsSupplier);

        storageFactorySwitch.init(productionSupplier, testingSupplier);

        Environment.getInstance()
                   .setToTests();

        storageFactorySwitch.get();
        storageFactorySwitch.get();

        verify(testingSupplier, times(1)).get();
    }

    @SuppressWarnings("CheckReturnValue") // ignore value of get() since we tests caching
    @Test
    @DisplayName("cache instance of StorageFactory in production")
    void cacheInProduction() {
        Supplier<StorageFactory> prodSupplier = spy(productionSupplier);

        storageFactorySwitch.init(prodSupplier, testsSupplier);

        Environment.getInstance()
                   .setToProduction();

        storageFactorySwitch.get();
        storageFactorySwitch.get();

        verify(prodSupplier, times(1)).get();
    }

    @Test
    @DisplayName("return itself on init")
    void returnItselfOnInit() {
        StorageFactorySwitch result = storageFactorySwitch.init(testsSupplier, testsSupplier);
        assertSame(storageFactorySwitch, result);
    }
}
