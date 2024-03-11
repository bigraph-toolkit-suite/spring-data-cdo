package org.bigraphs.spring.data.cdo.config;

/**
 * @author Dominik Grzelak
 */
public class CdoClientSessionOptions {

    private String repository;
    private boolean generatedPackageEmulationEnabled;
    private long writeLockoutTimeout;

    private CdoClientSessionOptions(String repository) {
        this.repository = repository;
    }

    public static CdoClientSessionOptionsBuilder builder() {
        return new CdoClientSessionOptionsBuilder();
    }

    public String getRepository() {
        return repository;
    }

    public long getWriteLockoutTimeout() {
        return writeLockoutTimeout;
    }

    public boolean isGeneratedPackageEmulationEnabled() {
        return generatedPackageEmulationEnabled;
    }

    public static class CdoClientSessionOptionsBuilder {
        private String repository;
        private boolean generatedPackageEmulationEnabled = true;
        private long writeLockoutTimeout = 10000L;

        public CdoClientSessionOptionsBuilder setRepository(String repository) {
            this.repository = repository;
            return this;
        }

        public CdoClientSessionOptionsBuilder setGeneratedPackageEmulationEnabled(boolean generatedPackageEmulationEnabled) {
            this.generatedPackageEmulationEnabled = generatedPackageEmulationEnabled;
            return this;
        }

        public CdoClientSessionOptionsBuilder setWriteLockoutTimeout(long lockoutTimeout) {
            this.writeLockoutTimeout = lockoutTimeout;
            return this;
        }

        public CdoClientSessionOptions build() {
            CdoClientSessionOptions cdoClientSessionOptions = new CdoClientSessionOptions(repository);
            cdoClientSessionOptions.generatedPackageEmulationEnabled = this.generatedPackageEmulationEnabled;
            cdoClientSessionOptions.writeLockoutTimeout = this.writeLockoutTimeout;
            return cdoClientSessionOptions;
        }
    }
}
