package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.core.CdoDeleteResult;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Interface that specified a basic set of CDO operations, implemented by {@link CdoTemplate}.
 * Not often used but a useful option for extensibility and testability (as it can be easily mocked or stubbed).
 *
 * @author Dominik Grzelak
 */
public interface CdoOperations extends DisposableBean {

    /**
     * @return mapping context in use.
     */
    MappingContext<? extends CdoPersistentEntity<?>, CdoPersistentProperty> getMappingContext();

    /**
     * Get resource path of an annotated CDO entity.
     *
     * @param entityClass the class of the entity
     * @return the resource path, or the simple name of the given class.
     * An exception is thrown if {@literal entityClass} is {@code null}.
     */
    default String getResourcePathFrom(@Nullable Class<?> entityClass) {
        if (Objects.isNull(entityClass)) {
            throw new InvalidDataAccessApiUsageException(
                    "No class parameter provided, entity path can't be determined!");
        }
        return Optional.ofNullable(getMappingContext().getRequiredPersistentEntity(entityClass).getResourcePath())
                .orElse(entityClass.getSimpleName());
    }

    CdoOperations withSession(CdoClientSession session);

    <T> T insert(T objectToSave);

    <T> T insert(T objectToSave, String pathName);

    <T> T save(T entity);

    <T> T save(T entity, String pathName);

    //TODO add method to retrieve all revisions

    //TODO provide another method with query options to retrieve e.g. specific revision..

    /**
     * always the latest revision
     *
     * @param entityID
     * @param resourcePath
     * @param <ID>
     */
    @Nullable
    <T, ID> T find(ID entityID, Class<T> javaClassType, final String resourcePath);

    <T> List<T> findAll(Class<T> javaClassType, final String pathValue);

    <T> CdoDeleteResult remove(T entity, final String resourcePath);

    /**
     * The resource path name is automatically retrieved from the annotated property. If not available
     * a fallback is used.
     *
     * @param entity the entity to remove
     * @param <T>    the type of the entity
     * @return the result of the remove operation
     */
    <T> CdoDeleteResult remove(T entity);

    <T> CdoDeleteResult removeAll(final Class<T> javaType, final String resourcePath);

}
