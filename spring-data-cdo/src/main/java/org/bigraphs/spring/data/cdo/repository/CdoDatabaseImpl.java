package org.bigraphs.spring.data.cdo.repository;

import org.bigraphs.spring.data.cdo.CdoClientSession;

/**
 * Specific repository commands
 *
 * @author Dominik Grzelak
 */
public class CdoDatabaseImpl implements CdoDatabase {

    private final String name;

    public CdoDatabaseImpl(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public void createResource(CdoClientSession var1, String var2) {

    }
}
