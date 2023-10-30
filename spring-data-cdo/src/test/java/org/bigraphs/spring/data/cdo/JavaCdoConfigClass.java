package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.config.AbstractCdoClientConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Sample configuration class.
 *
 * @author Dominik Grzelak
 */
@Configuration
public class JavaCdoConfigClass extends AbstractCdoClientConfiguration {
    @Override
    public CdoClient cdoClient() {
        return new CdoClient();
    }

    @NonNull
    @Override
    protected String getRepositoryName() {
        return "repo1";
    }
}
