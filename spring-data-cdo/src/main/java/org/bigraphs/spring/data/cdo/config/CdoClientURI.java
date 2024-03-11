package org.bigraphs.spring.data.cdo.config;

import org.bigraphs.spring.data.cdo.CdoServerConnectionString;

import java.util.List;

/**
 * @author Dominik Grzelak
 */
public class CdoClientURI {

    private final CdoServerConnectionString proxied;
    private final CdoClientOptions.Builder builder;

    public CdoClientURI(String uri) {
        this(uri, CdoClientOptions.builder());
    }

    public CdoClientURI(String uri, CdoClientOptions.Builder builder) {
        this.builder = builder;
        this.proxied = new CdoServerConnectionString(uri);
    }

    public CdoServerConnectionString getProxied() {
        return proxied;
    }

    public String getUsername() {
        return this.proxied.getUsername();
    }

    public char[] getPassword() {
        return this.proxied.getPassword();
    }

    public List<String> getHosts() {
        return this.proxied.getHosts();
    }

    public String getDatabase() {
        return this.proxied.getRepoName();
    }

    public String getURI() {
        return this.proxied.getConnectionString();
    }

    public CdoCredentials getCredentials() {
        return this.proxied.getCredential();
    }
}
