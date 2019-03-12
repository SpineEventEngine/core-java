package io.spine.server.procman;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.server.procman.model.ProcessManagerClass;

import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;

/**
 * Default implementation of {@code ProcessManagerRepository}.
 */
@Internal
public final class DefaultProcessManagerRepository<I,
                                                   P extends ProcessManager<I, S, ?>,
                                                   S extends Message>
    extends ProcessManagerRepository<I, P, S> {

    private final ProcessManagerClass<P> modelClass;

    /**
     * Creates a new repository managing process managers of the passed class.
     */
    public DefaultProcessManagerRepository(Class<P> cls) {
        super();
        this.modelClass = asProcessManagerClass(cls);
    }

    @Override
    protected ProcessManagerClass<P> entityModelClass() {
        return modelClass;
    }
}
