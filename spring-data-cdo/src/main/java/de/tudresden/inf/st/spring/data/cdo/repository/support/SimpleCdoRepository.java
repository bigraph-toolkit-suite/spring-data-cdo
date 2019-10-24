package de.tudresden.inf.st.spring.data.cdo.repository.support;

import de.tudresden.inf.st.spring.data.cdo.CdoOperations;
import de.tudresden.inf.st.spring.data.cdo.core.CdoDeleteResult;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoEntityInformation;
import de.tudresden.inf.st.spring.data.cdo.repository.CdoRepository;
import org.eclipse.emf.cdo.util.InvalidURIException;
import org.eclipse.emf.ecore.EPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

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

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> iterable) {
        return null; //TODO
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
                if (reason instanceof InvalidURIException && LOG.isDebugEnabled()) {
                    LOG.debug("Couldn't delete: " + reason.getLocalizedMessage());
                    return;
                }
            }
            throw new OptimisticLockingFailureException("Delete all exception: maybe resource path doesn't exist: " + entityInformation.getPathValue());
        }
    }
}
