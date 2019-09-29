package de.tudresden.inf.st.spring.data.cdo.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Optional;

/**
 * @author Dominik Grzelak
 */
@NoRepositoryBean
public interface CdoRepository<T, ID> extends CrudRepository<T, ID> {

    @Override
    <S extends T> S save(S s);

    @Override
    <S extends T> Iterable<S> saveAll(Iterable<S> iterable);

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
