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

import com.google.common.base.Supplier;
import io.spine.base.Environment;
import io.spine.core.BoundedContextName;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.spine.server.BoundedContext.newName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Yevsyukov
 */
public class StorageFactorySwitchShould {

    // Environment protection START
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
        // OK as we use the field after the initialization in storeEnvironment().
    private static Environment storedEnvironment;

    @BeforeClass
    public static void storeEnvironment() {
        storedEnvironment = Environment.getInstance().createCopy();
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
        // OK as we invoke after the initialization in storeEnvironment().
    @AfterClass
    public static void restoreEnvironment() {
        Environment.getInstance()
                   .restoreFrom(storedEnvironment);
    }
    // Environment protection END

    private StorageFactorySwitch storageFactorySwitch;

    private final boolean multitenant;

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

    protected StorageFactorySwitchShould(boolean multitenant) {
        this.multitenant = multitenant;
    }

    public StorageFactorySwitchShould() {
        this(false);
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        storageFactorySwitch = StorageFactorySwitch.newInstance(boundedContextName, multitenant);
    }

    @After
    public void cleanUp() throws NoSuchFieldException, IllegalAccessException {
        clearSwitch();
        Environment.getInstance().reset();
    }

    /**
     * Clears the `storageFactorySwitch` instance by nullifying fields.
     */
    private void clearSwitch() {
        storageFactorySwitch.reset();
    }

    @Test
    public void return_InMemoryStorageFactory_in_tests_if_tests_supplier_was_not_set() {
        final StorageFactory storageFactory = storageFactorySwitch.get();

        assertNotNull(storageFactory);
        assertTrue(storageFactory instanceof InMemoryStorageFactory);
    }

    @Test
    public void return_custom_test_StorageFactory_if_supplier_for_tests_was_set() {
        // This call avoids the racing conditions anomaly when running
        // the Gradle build from the console.
        // Despite the fact that we reset the switch state in `cleanUp()`, we still
        // get the cached value of the StorageFactory remembered by the switch in a previous test.
        // Having this call avoids the problem.
        storageFactorySwitch.reset();

        final StorageFactory custom = mock(StorageFactory.class);

        storageFactorySwitch.init(testsSupplier, new Supplier<StorageFactory>() {
            @Override
            public StorageFactory get() {
                return custom;
            }
        });

        // These calls ensure that we're under the testing mode and we get the supplier for tests.
        assertTrue(Environment.getInstance().isTests());
        assertTrue(storageFactorySwitch.testsSupplier()
                                       .isPresent());

        // Get the StorageFactory from the switch.
        final StorageFactory obtained = storageFactorySwitch.get();

        assertEquals(custom, obtained);
    }

    @SuppressWarnings("AccessOfSystemProperties") // OK for this test.
    @Test(expected = IllegalStateException.class)
    public void throw_IllegalStateException_if_production_supplier_is_non_tests_mode()
            throws NoSuchFieldException, IllegalAccessException {
        // Clear cached value for tests mode that may be left from the previous tests.
        Environment.getInstance().reset();
        // Pretend that we are not under tests for the `Environment`.
        Environment.getInstance().setToProduction();

        assertFalse(storageFactorySwitch.productionSupplier()
                                        .isPresent());
        storageFactorySwitch.get();
    }

    @Test
    public void cache_instance_of_StorageFactory_in_testing() {
        final Supplier<StorageFactory> testingSupplier = spy(testsSupplier);

        storageFactorySwitch.init(productionSupplier, testingSupplier);

        Environment.getInstance()
                   .setToTests();

        storageFactorySwitch.get();
        storageFactorySwitch.get();

        verify(testingSupplier, times(1)).get();
    }

    @Test
    public void cache_instance_of_StorageFactory_in_production() {
        final Supplier<StorageFactory> prodSupplier = spy(productionSupplier);

        storageFactorySwitch.init(prodSupplier, testsSupplier);

        Environment.getInstance()
                   .setToProduction();

        storageFactorySwitch.get();
        storageFactorySwitch.get();

        verify(prodSupplier, times(1)).get();
    }

    @Test
    public void return_itself_on_init() {
        final StorageFactorySwitch result = storageFactorySwitch.init(testsSupplier, testsSupplier);
        assertSame(storageFactorySwitch, result);
    }
}
