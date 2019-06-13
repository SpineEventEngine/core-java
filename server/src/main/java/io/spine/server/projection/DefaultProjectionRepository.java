package io.spine.server.projection;

import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.server.DefaultRepository;
import io.spine.server.DefaultRepositoryString;
import io.spine.server.projection.model.ProjectionClass;

import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;

/**
 * Default implementation of {@code ProjectionRepository}.
 *
 * @see io.spine.server.DefaultRepository
 */
@Internal
public class DefaultProjectionRepository<I, P extends Projection<I, S, ?>, S extends Message>
        extends ProjectionRepository<I, P, S>
        implements DefaultRepository {

    private final ProjectionClass<P> modelClass;

    /**
     * Creates a new repository managing projections of the passed class.
     */
    public DefaultProjectionRepository(Class<P> cls) {
        super();
        this.modelClass = asProjectionClass(cls);
    }

    @Override
    public ProjectionClass<P> entityModelClass() {
        return modelClass;
    }

    @Override
    public String toString() {
        return logName();
    }
}
