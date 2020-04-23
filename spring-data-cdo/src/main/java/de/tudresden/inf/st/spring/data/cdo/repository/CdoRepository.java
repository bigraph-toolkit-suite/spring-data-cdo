package de.tudresden.inf.st.spring.data.cdo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Generic interface of a CDO repository for generic CRUD operations.
 *
 * @author Dominik Grzelak
 */
@NoRepositoryBean
public interface CdoRepository<T, ID> extends CrudRepository<T, ID> {

    /**
     * Inserts or updates and entity.
     * If the entity is <i>new</i> it will be inserted. Otherwise, the entity is updated.
     * <p>
     * The returned instance shall be used for all further operations as the save operation might have changed the
     * entity instance completely.
     *
     * @param s   the entity to save; must not be {@code null}
     * @param <S> the type of the entity
     * @return the saved entity
     */
    @Override
    <S extends T> S save(S s);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#saveAll(java.lang.Iterable)
     */
    @Override
    <S extends T> Iterable<S> saveAll(Iterable<S> iterable);

    @Override
    Optional<T> findById(ID id);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#existsById()
     */
    @Override
    boolean existsById(ID id);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    @Override
    Iterable<T> findAll();

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAllById(java.lang.Iterable)
     */
    @Override
    Iterable<T> findAllById(Iterable<ID> iterable);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#count()
     */
    @Override
    long count();

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteById()
     */
    @Override
    void deleteById(ID id);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#delete()
     */
    @Override
    void delete(T t);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteAll(java.lang.Iterable)
     */
    @Override
    void deleteAll(Iterable<? extends T> iterable);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteAll()
     */
    @Override
    void deleteAll();
}
