package org.bigraphs.spring.data.cdo;

/**
 * @author Dominik Grzelak
 */
public class CdoServerAddress {

    public final String productGroup;
    public final String transportType;
    public final String description;
    public final int port;

    public static CdoServerAddressBuilder builder() {
        return new CdoServerAddressBuilder();
    }

    public CdoServerAddress() {
        this(new CdoServerAddressBuilder());
    }

    public String getProductGroup() {
        return productGroup;
    }

    public String getTransportType() {
        return transportType;
    }

    /**
     * server
     * @return
     */
    public String getDescription() {
        return description;
    }

    public int getPort() {
        return port;
    }

    private CdoServerAddress(String productGroup, String transportType, String description, int port) {
        this.productGroup = productGroup;
        this.transportType = transportType;
        this.description = description;
        this.port = port;
    }

    public CdoServerAddress(CdoServerAddressBuilder builder) {
        this.productGroup = builder.productGroup;
        this.transportType = builder.transportType;
        this.description = builder.description;
        this.port = builder.port;
    }

    /**
     * @return description and port
     */
    public String getFullConnectorDescription() {
        return description + ":" + port;
    }

    public static class CdoServerAddressBuilder {
        private String productGroup = "org.eclipse.net4j.connectors";
        private String transportType = "tcp";
        private String description = "localhost";
        private int port = 2036;

        public CdoServerAddressBuilder setProductGroup(String productGroup) {
            this.productGroup = productGroup;
            return this;
        }

        public CdoServerAddressBuilder setTransportType(String transportType) {
            this.transportType = transportType;
            return this;
        }

        public CdoServerAddressBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public CdoServerAddressBuilder setPort(int port) {
            this.port = port;
            return this;
        }

        public CdoServerAddress createCdoServerAddress() {
            return new CdoServerAddress(productGroup, transportType, description, port);
        }
    }
}
