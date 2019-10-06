package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.config.AbstractCdoClientConfiguration;
import org.springframework.context.annotation.Configuration;

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

    @Override
    protected String getRepositoryName() {
        return "repo1";
    }
}
