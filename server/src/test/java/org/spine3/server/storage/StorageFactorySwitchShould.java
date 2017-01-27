/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.server.storage;

import com.google.common.base.Supplier;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.spine3.server.storage.memory.InMemoryStorageFactory;
import org.spine3.util.Environment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.spine3.test.Tests.hasPrivateParameterlessCtor;

/**
 * @author Alexander Yevsyukov
 */
public class StorageFactorySwitchShould {

    // Environment protection START
    @SuppressWarnings("StaticVariableMayNotBeInitialized")
    private static Environment storedEnvironment;

    @BeforeClass
    public static void storeEnvironment() {
        storedEnvironment = Environment.getInstance().createCopy();
    }

    @SuppressWarnings("StaticVariableUsedBeforeInitialization")
    @AfterClass
    public static void restoreEnvironment() {
        Environment.getInstance().restoreFrom(storedEnvironment);
    }
    // Environment protection END

    private StorageFactorySwitch storageFactorySwitch;

    private final Supplier<StorageFactory> inMemorySupplier = new Supplier<StorageFactory>() {
        @Override
        public StorageFactory get() {
            return InMemoryStorageFactory.getInstance();
        }
    };

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException {
        storageFactorySwitch = StorageFactorySwitch.getInstance();
    }

    @After
    public void cleanUp() throws NoSuchFieldException, IllegalAccessException {
        cleanSwitch();
        Environment.getInstance().reset();
    }

    /**
     * Cleans the `storageFactorySwitch` instance by nullifying fields.
     */
    private void cleanSwitch() throws NoSuchFieldException, IllegalAccessException {
        storageFactorySwitch.reset();
    }

    @Test
    public void have_private_parameterless_constructor() {
        assertTrue(hasPrivateParameterlessCtor(StorageFactorySwitch.class));
    }

    @Test
    public void return_InMemoryStorageFactory_in_tests_if_tests_supplier_was_not_set() {
        final StorageFactory storageFactory = storageFactorySwitch.get();

        assertNotNull(storageFactory);
        assertTrue(storageFactory instanceof InMemoryStorageFactory);
    }

    @Test
    public void return_custom_test_StorageFactory_if_supplier_for_tests_was_set() {
        // This calls avoids the racing conditions anomaly when running
        // the Gradle build from the console.
        // Despite the fact that we reset the switch state in `cleanUp()`, we still
        // get the cached value of the StorageFactory remembered by the switch in a previous test.
        // Having this call avoids the problem.
        storageFactorySwitch.reset();

        final StorageFactory custom = mock(StorageFactory.class);

        storageFactorySwitch.init(inMemorySupplier, new Supplier<StorageFactory>() {
            @Override
            public StorageFactory get() {
                return custom;
            }
        });

        // These calls ensure that we're under the testing mode and we get the supplier for tests.
        assertTrue(Environment.getInstance().isTests());
        assertTrue(storageFactorySwitch.testsSupplier().isPresent());

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

        assertFalse(storageFactorySwitch.productionSupplier().isPresent());
        storageFactorySwitch.get();
    }

    @Test
    public void cache_instance_of_StorageFactory_in_testing() {
        Supplier<StorageFactory> testingSupplier = spy(inMemorySupplier);

        storageFactorySwitch.init(inMemorySupplier, testingSupplier);

        Environment.getInstance().setToTests();

        storageFactorySwitch.get();
        storageFactorySwitch.get();

        verify(testingSupplier, times(1)).get();
    }

    @Test
    public void cache_instance_of_StorageFactory_in_production() {
        Supplier<StorageFactory> productionSupplier = spy(inMemorySupplier);

        storageFactorySwitch.init(productionSupplier, inMemorySupplier);

        Environment.getInstance().setToProduction();

        storageFactorySwitch.get();
        storageFactorySwitch.get();

        verify(productionSupplier, times(1)).get();
    }
}
