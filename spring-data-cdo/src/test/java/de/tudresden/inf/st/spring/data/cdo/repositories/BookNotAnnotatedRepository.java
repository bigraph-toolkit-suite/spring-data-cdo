package de.tudresden.inf.st.spring.data.cdo.repositories;

import de.tudresden.inf.st.spring.data.cdo.repository.CdoRepository;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.springframework.stereotype.Repository;

@Repository
public interface BookNotAnnotatedRepository extends CdoRepository<BookNotAnnotatedRepositoryIntegrationTest.BookNotAnnotated, CDOID> {

}