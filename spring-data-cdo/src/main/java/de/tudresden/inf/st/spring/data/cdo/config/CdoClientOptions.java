package de.tudresden.inf.st.spring.data.cdo.config;

import de.tudresden.inf.st.spring.data.cdo.CdoServerAddress;
import de.tudresden.inf.st.spring.data.cdo.CdoServerConnectionString;

/**
 * @author Dominik Grzelak
 */
public class CdoClientOptions {

    private final String description;

    private CdoServerAddress addr;

    private CdoCredentials cdoCredentials;

    public CdoClientOptions() {
        this("0.0.0.0");
    }

    public CdoClientOptions(String description) {
        this.description = description;
    }

    public static CdoClientOptions.Builder builder() {
        return new CdoClientOptions.Builder();
    }

    public String getDescription() {
        return description;
    }

    public CdoServerAddress getAddr() {
        return addr;
    }

    public CdoCredentials getCdoCredentials() {
        return cdoCredentials;
    }

    public static class Builder {
        private String description = "tcp";

        public Builder() {
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder applyConnectionString(CdoServerConnectionString connectionString) {
            //TODO ....
            return this;
        }

        public CdoClientOptions build() {
            return new CdoClientOptions(description);
        }
    }

//    public static MongoClientOptions.Builder builder(MongoClientOptions options) {
//        return new MongoClientOptions.Builder(options);
//    }
}
