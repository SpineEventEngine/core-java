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

    @DisplayName("clear enrichments from event")
    @Test
    void shouldClearEnrichments() {
        Enrichment enrichment = GivenEnrichment.withOneAttribute();
        Event event = GivenEvent.arbitrary();
        EventContext contextWithEnrichment = event
                .getContext()
                .toVBuilder()
                .setEnrichment(enrichment)
                .build();
        Event eventWithEnrichment = event
                .toVBuilder()
                .setContext(contextWithEnrichment)
                .build();
        EEntity entity = EEntity.create(eventWithEnrichment);
        Event state = entity.state();
        assertThat(state).isEqualTo(event);
    }
}
