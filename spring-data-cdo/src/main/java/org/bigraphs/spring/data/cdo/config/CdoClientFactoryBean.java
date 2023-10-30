package org.bigraphs.spring.data.cdo.config;

import org.bigraphs.spring.data.cdo.CdoServerAddress;
import org.bigraphs.spring.data.cdo.CdoClient;
import org.bigraphs.spring.data.cdo.core.CdoExceptionTranslator;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.net.UnknownHostException;
import java.util.Objects;

/**
 * @author Dominik Grzelak
 */
public class CdoClientFactoryBean extends AbstractFactoryBean<CdoClient> implements PersistenceExceptionTranslator {

    private static final PersistenceExceptionTranslator DEFAULT_EXCEPTION_TRANSLATOR = new CdoExceptionTranslator();

    @Nullable
    private CdoClientOptions cdoClientOptions;
    @Nullable
    private String host;
    @Nullable
    private Integer port;
    @Nullable
    private CdoCredentials credential;

    private PersistenceExceptionTranslator exceptionTranslator = DEFAULT_EXCEPTION_TRANSLATOR;

    public CdoClientFactoryBean() {
    }

    @Override
    public Class<? extends CdoClient> getObjectType() {
        return CdoClient.class;
    }

    @Override
    protected CdoClient createInstance() throws Exception {
        if (cdoClientOptions == null) {
            cdoClientOptions = CdoClientOptions.builder().build();
        }
        return createCdoClient();
    }

    @Override
    protected void destroyInstance(@Nullable CdoClient instance) throws Exception {
        if (instance != null) {
            instance.close();
        }
    }

    private CdoClient createCdoClient() throws UnknownHostException {
        return new CdoClient(createConfiguredOrDefaultServerAddress(), credential, cdoClientOptions);
    }

    private CdoServerAddress createConfiguredOrDefaultServerAddress() throws UnknownHostException {
        CdoServerAddress.CdoServerAddressBuilder builder = CdoServerAddress.builder();
        if (StringUtils.hasText(host))
            builder.setDescription(host);
        if (Objects.nonNull(port)) {
            builder.setPort(port);
        }
        return builder.createCdoServerAddress();
    }

    /**
     * Configures the {@link PersistenceExceptionTranslator} to use.
     *
     * @param exceptionTranslator
     */
    public void setExceptionTranslator(@Nullable PersistenceExceptionTranslator exceptionTranslator) {
        this.exceptionTranslator = exceptionTranslator == null ? DEFAULT_EXCEPTION_TRANSLATOR : exceptionTranslator;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.dao.support.PersistenceExceptionTranslator#translateExceptionIfPossible(java.lang.RuntimeException)
     */
    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException e) {
        return exceptionTranslator.translateExceptionIfPossible(e);
    }

    public void setHost(@Nullable String host) {
        this.host = host;
    }

    public void setPort(@Nullable Integer port) {
        this.port = port;
    }

    public void setCredential(@Nullable CdoCredentials credential) {
        this.credential = credential;
    }
}
