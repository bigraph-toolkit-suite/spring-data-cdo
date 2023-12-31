package org.bigraphs.spring.data.cdo.repository.support;

import org.bigraphs.spring.data.cdo.CdoOperations;
import org.bigraphs.spring.data.cdo.CDORevisionHolder;
import org.bigraphs.spring.data.cdo.core.CdoDeleteResult;
import org.bigraphs.spring.data.cdo.repository.CdoEntityInformation;
import org.bigraphs.spring.data.cdo.repository.CdoRepository;
import org.eclipse.emf.cdo.util.InvalidURIException;
import org.eclipse.emf.ecore.EPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.util.Streamable;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * See: https://github.com/spring-projects/spring-data-commons/wiki/developer-guide#create-a-simplerepository-implementing-crudrepository-or-pagingandsortingrepository
 * Two kind of stores
 *
 * @author Dominik Grzelak
 */
public class SimpleCdoRepository<T, ID> implements CdoRepository<T, ID> {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleCdoRepository.class);
    private final CdoOperations cdoOperations;
    private final CdoEntityInformation<T, ID> entityInformation;

    public SimpleCdoRepository(CdoEntityInformation<T, ID> metadata, CdoOperations cdoOperations) {

        Assert.notNull(metadata, "CdoEntityInformation must not be null!");
        Assert.notNull(cdoOperations, "CdoOperations must not be null!");

        this.entityInformation = metadata;
        this.cdoOperations = cdoOperations;
    }

    @Override
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null!");

        if (entityInformation.isNew(entity)) { // create new
            return cdoOperations.insert(entity, entityInformation.getPathValue());
        }
        // UPDATE otherwise
        return cdoOperations.save(entity, entityInformation.getPathValue());
    }

    /*
     * (non-Javadoc)
     * @see org.bigraphs.spring.data.cdo.repository.cdo.CdoRepository#saveAll(java.lang.Iterable)
     */
    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> iterable) {
        Assert.notNull(iterable, "The given Iterable of entities must not be null!");

        Streamable<S> source = Streamable.of(iterable);
        boolean allNew = source.stream().allMatch(it -> entityInformation.isNew(it));

        if (allNew) {
            List<S> result = source.stream().collect(Collectors.toList());
            return new ArrayList<>(cdoOperations.insertAll(result, entityInformation.getPathValue()));
        }
        return source.stream().map(this::save).collect(Collectors.toList());
    }

    @Override
    public CDORevisionHolder<T> getRevisionById(@NonNull ID id) {
        Assert.notNull(id, "The given ID must not be null!");
        return cdoOperations.getRevisionById(id, entityInformation.getPathValue());
    }

    @Override
    public CDORevisionHolder<T> getRevision(@NonNull T entity) {
        Assert.notNull(entity, "The given entity must not be null!");
        return cdoOperations.getRevision(entity, entityInformation.getPathValue());
    }

    @Override
    public Optional<T> findById(ID id) {
        Class<T> javaType = entityInformation.getJavaType();
        T objectFound = cdoOperations.find(id, javaType, entityInformation.getPathValue());
        return Optional.ofNullable(objectFound);
    }

    @Override
    public boolean existsById(ID id) {
        //TODO maybe there is a faster way than returning the whole object?
        return findById(id).isPresent();
    }

    @Override
    public Iterable<T> findAll() {
        Class<T> javaType = entityInformation.getJavaType();
        List<T> all = cdoOperations.findAll(javaType, entityInformation.getPathValue());
        return all;
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> iterable) {
        // TODO
        return null;
    }

    @Override
    public long count() {
        Class<T> javaType = entityInformation.getJavaType();
        EPackage context = entityInformation.getContext();
        String resourcePath = entityInformation.getPathValue();
        return cdoOperations.countAll(javaType, context, resourcePath);
    }

    @Override
    public void deleteById(ID id) {

    }

    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "Entity must not be null!");

        CdoDeleteResult deleteResult = cdoOperations.remove(entity, entityInformation.getPathValue());

        if (entityInformation.isVersioned() && deleteResult.wasAcknowledged() && deleteResult.getDeletedCount() == 0) {
            throw new OptimisticLockingFailureException(String.format(
                    "The entity with id %s in %s cannot be deleted! Was it modified or deleted in the meantime?",
                    entityInformation.getId(entity),
                    entityInformation.getPathValue()));
        }
    }

    @Override
    public void deleteAll(Iterable<? extends T> iterable) {

    }

    @Override
    public void deleteAll() {
        CdoDeleteResult cdoDeleteResult = cdoOperations.removeAll(entityInformation.getJavaType(), entityInformation.getPathValue());
        if (!cdoDeleteResult.wasAcknowledged()) {
            if (cdoDeleteResult instanceof CdoDeleteResult.UnacknowledgedCdoDeleteResult) {
                Throwable reason = ((CdoDeleteResult.UnacknowledgedCdoDeleteResult) cdoDeleteResult).getReason();
                if (reason instanceof InvalidURIException) {
                    if (LOG.isDebugEnabled())
                        LOG.debug("Couldn't delete: " + reason.getLocalizedMessage() + ". Maybe resource is already removed.");
                    return;
                }
            }
            throw new OptimisticLockingFailureException("Delete all exception: maybe resource path doesn't exist: " + entityInformation.getPathValue());
        }
    }
}
