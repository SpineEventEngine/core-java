/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.tuple;

import com.google.common.base.Optional;
import com.google.common.testing.EqualsTester;
import com.google.common.testing.NullPointerTester;
import com.google.protobuf.Message;
import io.spine.test.tuple.quintet.InstrumentNumber;
import io.spine.test.tuple.quintet.Viola;
import io.spine.test.tuple.quintet.Violin;
import io.spine.test.tuple.quintet.ViolinCello;
import io.spine.validate.Validate;
import org.junit.Test;

import static com.google.common.testing.SerializableTester.reserializeAndAssert;

/**
 * Tests {@link Quintet} tuple.
 *
 * <p>This test suite uses <a href="https://en.wikipedia.org/wiki/String_quintet">String quintets</a>
 * types just to have test data other than default Protobuf types, and for some fun.
 *
 * <p>In a real app a {@link Quintet} should have only event messages, and not value objects.
 *
 * @author Alexander Yevsyukov
 */
public class QuintetShould {

    private final Quintet celloQuintet = QuintetFactory.newCelloQuintet();
    private final Quintet violaQuintet = QuintetFactory.newViolaQuintet();

    @Test
    public void pass_null_tolerance_check() {
        new NullPointerTester().setDefault(Message.class,
                                           InstrumentFactory.newViola())
                               .setDefault(Optional.class,
                                           Optional.of(InstrumentFactory.newViolinCello()))
                               .testAllPublicStaticMethods(Quintet.class);
    }

    @Test
    public void serialize() {
        reserializeAndAssert(celloQuintet);
        reserializeAndAssert(violaQuintet);
    }

    @Test
    public void support_equality() {
        new EqualsTester().addEqualityGroup(QuintetFactory.newCelloQuintet(),
                                            QuintetFactory.newCelloQuintet())
                          .addEqualityGroup(QuintetFactory.newViolaQuintet())
                          .testEquals();
    }

    /*
     * Test environment
     *********************/

    /**
     * Creates typical <a href="https://en.wikipedia.org/wiki/String_quintet">String quintets</a>.
     */
    private static class QuintetFactory {

        private static final InstrumentNumber NUM_1 = InstrumentNumber.newBuilder()
                                                                      .setValue(1)
                                                                      .build();

        private static final InstrumentNumber NUM_2 = InstrumentNumber.newBuilder()
                                                                      .setValue(2)
                                                                      .build();

        /** Prevents instantiation of this utility class. */
        private QuintetFactory() {
        }

        static Quintet<Violin, Violin, Viola, ViolinCello, ViolinCello> newCelloQuintet() {
            return Quintet.of(InstrumentFactory.newViolin(NUM_1),
                              InstrumentFactory.newViolin(NUM_2),
                              InstrumentFactory.newViola(),
                              InstrumentFactory.newViolinCello(NUM_1),
                              InstrumentFactory.newViolinCello(NUM_2));
        }

        static Quintet<Violin, Violin, Viola, Viola, ViolinCello> newViolaQuintet() {
            return Quintet.of(InstrumentFactory.newViolin(NUM_1),
                              InstrumentFactory.newViolin(NUM_2),
                              InstrumentFactory.newViola(NUM_1),
                              InstrumentFactory.newViola(NUM_2),
                              InstrumentFactory.newViolinCello());
        }
    }

    /**
     * Creates instruments.
     */
    private static class InstrumentFactory {

        /** Prevents instantiation of this utility class. */
        private InstrumentFactory() {
        }

        private static Violin newViolin(InstrumentNumber number) {
            final Violin result = Violin.newBuilder()
                                        .setNumber(number)
                                        .build();
            Validate.checkValid(result);
            return result;
        }

        private static Viola newViola() {
            final Viola result = Viola.newBuilder()
                                      .setSingle(true)
                                      .build();
            Validate.checkValid(result);
            return result;
        }

        private static Viola newViola(InstrumentNumber number) {
            final Viola result = Viola.newBuilder()
                                      .setNumber(number)
                                      .build();
            Validate.checkValid(result);
            return result;
        }

        private static ViolinCello newViolinCello(InstrumentNumber number) {
            final ViolinCello result = ViolinCello.newBuilder()
                                                  .setNumber(number)
                                                  .build();
            Validate.checkValid(result);
            return result;
        }

        private static ViolinCello newViolinCello() {
            final ViolinCello result = ViolinCello.newBuilder()
                                                  .setSingle(true)
                                                  .build();
            Validate.checkValid(result);
            return result;
        }
    }
}
