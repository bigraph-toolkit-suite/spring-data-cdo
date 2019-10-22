package de.tudresden.inf.st.spring.data.cdo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * Generic interface of a CDO repository.
 *
 * @author Dominik Grzelak
 */
@NoRepositoryBean
public interface CdoRepository<T, ID> extends CrudRepository<T, ID> {

    /**
     * Inserts or updates and entity.
     * If the entity is <i>new</i> it will be inserted. Otherwise, the entity is updated.
     *
     * @param s   the entity to save
     * @param <S> the type of the entity
     * @return the saved entity
     */
    @Override
    <S extends T> S save(S s);

    @Override
    <S extends T> Iterable<S> saveAll(Iterable<S> iterable);//TODO

    @Override
    Optional<T> findById(ID id);

    @Override
    boolean existsById(ID id);

    @Override
    Iterable<T> findAll();

    @Override
    Iterable<T> findAllById(Iterable<ID> iterable);

    @Override
    long count();

    @Override
    void deleteById(ID id);

    @Override
    void delete(T t);

    @Override
    void deleteAll(Iterable<? extends T> iterable);

    @Override
    void deleteAll();
}
