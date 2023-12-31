package org.bigraphs.spring.data.cdo.repository;

import org.bigraphs.spring.data.cdo.CdoClientSession;

/**
 * Operations that only relates to specific repository tasks such as creating a view
 * and do not incorporates any models. Needs still a session of course
 *
 * @author Dominik Grzelak
 */
public interface CdoDatabase {
    String getName();

    void createResource(CdoClientSession session, String resourcePath);

}
