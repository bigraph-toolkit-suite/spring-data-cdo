package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.config.CdoClientOptions;

/**
 * Factory for {@link CdoClient}.
 *
 * @author Dominik Grzelak
 */
public class CdoClients {

    public static CdoClient create(String connectionString) {
        return create(new CdoServerConnectionString(connectionString));
    }

    public static CdoClient create(CdoServerConnectionString cdoServerConnectionString) {
        return new CdoClient(CdoClientOptions.builder().applyConnectionString(cdoServerConnectionString).build());
    }

    public static CdoClient create(String host, int port) {
        return new CdoClient(host, port);
    }
}
