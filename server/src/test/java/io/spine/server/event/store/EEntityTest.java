package io.spine.server.event.store;

import io.spine.core.Enrichment;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.server.type.given.GivenEvent;
import io.spine.testing.core.given.GivenEnrichment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;

@DisplayName("EEntity should")
final class EEntityTest {

    @DisplayName("clear enrichments from an event")
    @Test
    void clearEnrichments() {
        Enrichment enrichment = GivenEnrichment.withOneAttribute();
        Event event = GivenEvent.arbitrary();
        EventContext contextWithEnrichment = event
                .getContext()
                .toBuilder()
                .setEnrichment(enrichment)
                .build();
        Event eventWithEnrichment = event
                .toBuilder()
                .setContext(contextWithEnrichment)
                .build();
        EEntity entity = EEntity.create(eventWithEnrichment);

        assertThat(entity.state()).isEqualTo(event);
    }
}
