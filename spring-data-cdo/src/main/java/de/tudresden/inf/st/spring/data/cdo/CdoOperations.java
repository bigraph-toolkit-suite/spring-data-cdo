package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.core.CdoDeleteResult;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentEntity;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoPersistentProperty;
import org.eclipse.emf.ecore.EPackage;
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

    default String getPackageNameFrom(@Nullable Class<?> entityClass) {
        if (Objects.isNull(entityClass)) {
            throw new InvalidDataAccessApiUsageException(
                    "No class parameter provided, entity path can't be determined!");
        }
        return Optional.ofNullable(getMappingContext().getRequiredPersistentEntity(entityClass).getPackageName())
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

    /**
     * The resource path name is automatically retrieved from the annotated property. If not available
     * a fallback is used.
     *
     * @param entity the entity to remove
     * @param <T>    the type of the entity
     * @return the result of the remove operation
     */
    <T> CdoDeleteResult remove(T entity);

    <T> CdoDeleteResult remove(T entity, final String resourcePath);

    /**
     * Deletes the given resource path from the configured repository.
     * Only the final node or folder will be removed keeping the parent folders intact.
     *
     * @param resourcePath the resource path to remove. Must not be {@code null}.
     */
    void removeResourcePath(final String resourcePath);

    /**
     * Deletes the whole resource path from the configured repository.
     *
     * @param resourcePath the resource path to remove. Must not be {@code null}.
     * @param recursive    flag if all parent folders in the path shall be deleted recursively also.
     */
    void removeResourcePath(final String resourcePath, boolean recursive);


    /**
     * Will create the given resource path if it doesn't already exists.
     *
     * @param resourcePath the resource path to create. Must not be {@code null}.
     * @throws CreateResourceFailedException This exception is thrown when the resource path couldn't be created.
     *                                       For example, when some part of the path represents a folder which is
     *                                       however already a resource node in CDO with the same name.
     */
    void createResourcePath(final String resourcePath);

    /**
     * Drop the resource path with the path value indicated by the entity class.
     *
     * @param javaType class that determines the resource path to delete. Must not be {@code null}.
     * @param <T>      the type of the class
     * @return
     */
    <T> CdoDeleteResult removeAll(final Class<T> javaType);

    /**
     * Remove all objects under the given resource path.
     *
     * @param resourcePath name of the resource path to remove
     * @return
     */
    CdoDeleteResult removeAll(final String resourcePath);

    <T> CdoDeleteResult removeAll(final Class<T> javaType, final String resourcePath);


    /**
     * Returns the default {@link CdoConverter}.
     *
     * @return the default converter
     */
    CdoConverter getConverter();

    /**
     * The context of the current class type is automatically inferred by using the provided
     * {@link EPackage} of the entity as context.
     *
     * @param <T>          type of the class
     * @param javaType     the class of the entity
     * @param context      the context of the query which is the EPackage of the entity
     * @param resourcePath the resource path
     * @return
     */
    <T> long countAll(final Class<T> javaType, final EPackage context, final String resourcePath);
}
