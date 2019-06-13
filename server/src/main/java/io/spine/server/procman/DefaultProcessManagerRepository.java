package io.spine.server.procman;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.server.DefaultRepository;
import io.spine.server.DefaultRepositoryString;
import io.spine.server.procman.model.ProcessManagerClass;

import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;

/**
 * Default implementation of {@code ProcessManagerRepository}.
 *
 * @see io.spine.server.DefaultRepository
 */
@Internal
public final class DefaultProcessManagerRepository<I,
                                                   P extends ProcessManager<I, S, ?>,
                                                   S extends Message>
        extends ProcessManagerRepository<I, P, S>
        implements DefaultRepository {

    private final ProcessManagerClass<P> modelClass;

    /**
     * Creates a new repository managing process managers of the passed class.
     */
    public DefaultProcessManagerRepository(Class<P> cls) {
        super();
        this.modelClass = asProcessManagerClass(cls);
    }

    @Override
    public ProcessManagerClass<P> entityModelClass() {
        return modelClass;
    }

    @Override
    public String toString() {
        return logName();
    }
}
