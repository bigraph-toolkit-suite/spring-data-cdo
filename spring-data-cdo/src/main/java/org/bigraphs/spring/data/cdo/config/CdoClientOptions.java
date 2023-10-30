package org.bigraphs.spring.data.cdo.config;

import org.bigraphs.spring.data.cdo.CdoServerAddress;
import org.bigraphs.spring.data.cdo.CdoServerConnectionString;

/**
 * @author Dominik Grzelak
 */
public class CdoClientOptions {

    private CdoServerAddress addr;

    private CdoCredentials cdoCredentials;

    public CdoClientOptions() {
        this("0.0.0.0");
    }

    public CdoClientOptions(String server) {
        addr = new CdoServerAddress(CdoServerAddress.builder().setDescription(server));
    }

    public static CdoClientOptions.Builder builder() {
        return new CdoClientOptions.Builder();
    }

    public CdoServerAddress getAddr() {
        return addr;
    }

    public CdoCredentials getCdoCredentials() {
        return cdoCredentials;
    }

    public static class Builder {
        private String description = "localhost";
        private CdoServerAddress addr;
        private CdoCredentials cdoCredentials;

        public Builder() {
        }

        public Builder setServer(String description) {
            this.description = description;
            return this;
        }

        public Builder applyConnectionString(CdoServerConnectionString connectionString) {
            this.description = connectionString.getServer();
            this.addr = new CdoServerAddress(
                    CdoServerAddress.builder()
                            .setDescription(connectionString.getServer())
                            .setPort(connectionString.getPort())
            );
            this.cdoCredentials = connectionString.getCredential();
            return this;
        }

        public CdoClientOptions build() {
            CdoClientOptions cdoClientOptions = new CdoClientOptions(description);
            cdoClientOptions.addr = this.addr;
            cdoClientOptions.cdoCredentials = this.cdoCredentials;
            return cdoClientOptions;
        }
    }
}
