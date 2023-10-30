package org.bigraphs.spring.data.cdo.repositories;

import org.bigraphs.spring.data.cdo.repository.CdoRepository;
import org.eclipse.emf.cdo.common.id.CDOID;
import org.springframework.stereotype.Repository;

@Repository
public interface BookNotAnnotatedRepository extends CdoRepository<BookNotAnnotatedRepositoryIntegrationTest.BookNotAnnotated, CDOID> {

}
