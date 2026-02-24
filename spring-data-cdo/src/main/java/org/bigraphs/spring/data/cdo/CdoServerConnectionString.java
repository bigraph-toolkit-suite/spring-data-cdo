package org.bigraphs.spring.data.cdo;

import org.bigraphs.spring.data.cdo.config.CdoCredentials;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Source code is mostly adapted from {@code com.mongodb.ConnectionString} of the
 * <a href="https://mongodb.github.io/mongo-java-driver/3.8/javadoc/com/mongodb/ConnectionString.html">mongo-java-driver library</a>.
 *
 * @author Dominik Grzelak
 * @see <a href="https://mongodb.github.io/mongo-java-driver/3.8/javadoc/com/mongodb/ConnectionString.html">https://mongodb.github.io/mongo-java-driver/3.8/javadoc/com/mongodb/ConnectionString.html</a>
 */
public class CdoServerConnectionString {
    private static final String CDO_PROTOCOL_PREFIX = "cdo://";
    private static final String UTF_8 = "UTF-8";

    private String repoName;

    private int port;

    private final CdoCredentials credential;

    private String host;

    private final List<String> hosts;

    private String connectionString;

    public CdoServerConnectionString(final String connectionString) {
        this.connectionString = connectionString;
        boolean isCDOProtocol = connectionString.startsWith(CDO_PROTOCOL_PREFIX);
        if (!isCDOProtocol) {
            throw new IllegalArgumentException(format("The connection string is invalid. "
                    + "Connection strings must start with either '%s'", CDO_PROTOCOL_PREFIX));
        }

        String unprocessedConnectionString;
        unprocessedConnectionString = connectionString.substring(CDO_PROTOCOL_PREFIX.length());

        // Split out the user and host information
        String userAndHostInformation;
        int idx = unprocessedConnectionString.indexOf("/");
        if (idx == -1) {
            userAndHostInformation = unprocessedConnectionString;
            unprocessedConnectionString = "";
        } else {
            userAndHostInformation = unprocessedConnectionString.substring(0, idx);
            unprocessedConnectionString = unprocessedConnectionString.substring(idx + 1);
        }

        // Split the user and host information
        String userInfo;
        String hostIdentifier;
        String userName = null;
        char[] password = null;
        idx = userAndHostInformation.lastIndexOf("@");
        if (idx > 0) {
            userInfo = userAndHostInformation.substring(0, idx).replace("+", "%2B");
            hostIdentifier = userAndHostInformation.substring(idx + 1);
            int colonCount = countOccurrences(userInfo, ":");
            if (userInfo.contains("@") || colonCount > 1) {
                throw new IllegalArgumentException("The connection string contains invalid user information. "
                        + "If the username or password contains a colon (:) or an at-sign (@) then it must be urlencoded");
            }
            if (colonCount == 0) {
                userName = urldecode(userInfo);
            } else {
                idx = userInfo.indexOf(":");
                userName = urldecode(userInfo.substring(0, idx));
                password = urldecode(userInfo.substring(idx + 1), true).toCharArray();
            }
        } else {
            hostIdentifier = userAndHostInformation;
        }

        this.hosts = unmodifiableList(parseHosts(asList(hostIdentifier.split(","))));
        if (!this.hosts.isEmpty()) {
            String[] splitted = this.hosts.get(0).split(":");
            this.host = splitted[0];
            this.port = Integer.parseInt(splitted[1]);
        }

        idx = unprocessedConnectionString.indexOf("/");
        if (unprocessedConnectionString.isEmpty()) {
            throw new IllegalArgumentException("The connection string contains no repository details.");
        }

        String repoResourcePathString;
        if (idx == -1) {
            this.repoName = unprocessedConnectionString;
        } else {
            this.repoName = unprocessedConnectionString.substring(0, idx);
            if (this.repoName.replaceAll("/", "").isEmpty()) {
                throw new IllegalArgumentException("The connection string contains no repository details.");
            }
            repoResourcePathString = unprocessedConnectionString.substring(idx, unprocessedConnectionString.length());
        }

        if (userName == null) {
            credential = CdoCredentials.empty();
        } else {
            credential = new CdoCredentials(userName, password, "");
        }
    }

    private int countOccurrences(final String haystack, final String needle) {
        return haystack.length() - haystack.replace(needle, "").length();
    }

    private String urldecode(final String input) {
        return urldecode(input, false);
    }

    private String urldecode(final String input, final boolean password) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8);
    }

    private List<String> parseHosts(final List<String> rawHosts) {
        if (rawHosts.isEmpty()) {
            throw new IllegalArgumentException("The connection string must contain at least one host");
        }
        List<String> hosts = new ArrayList<>();
        for (String host : rawHosts) {
            if (host.isEmpty()) {
                throw new IllegalArgumentException(format("The connection string contains an empty host '%s'. ", rawHosts));
            } else if (host.endsWith(".sock")) {
                host = urldecode(host);
            } else if (host.startsWith("[")) {
                if (!host.contains("]")) {
                    throw new IllegalArgumentException(format("The connection string contains an invalid host '%s'. "
                            + "IPv6 address literals must be enclosed in '[' and ']' according to RFC 2732", host));
                }
                int idx = host.indexOf("]:");
                if (idx != -1) {
                    validatePort(host, host.substring(idx + 2));
                }
            } else {
                int colonCount = countOccurrences(host, ":");
                if (colonCount > 1) {
                    throw new IllegalArgumentException(format("The connection string contains an invalid host '%s'. "
                            + "Reserved characters such as ':' must be escaped according RFC 2396. "
                            + "Any IPv6 address literal must be enclosed in '[' and ']' according to RFC 2732.", host));
                } else if (colonCount == 1) {
                    validatePort(host, host.substring(host.indexOf(":") + 1));
                }
            }
            hosts.add(host);
        }
        Collections.sort(hosts);
        return hosts;
    }

    private void validatePort(final String host, final String port) {
        boolean invalidPort = false;
        try {
            int portInt = Integer.parseInt(port);
            if (portInt <= 0 || portInt > 65535) {
                invalidPort = true;
            }
        } catch (NumberFormatException e) {
            invalidPort = true;
        }
        if (invalidPort) {
            throw new IllegalArgumentException(format("The connection string contains an invalid host '%s'. "
                    + "The port '%s' is not a valid, it must be an integer between 0 and 65535", host, port));
        }
    }

    public List<String> getHosts() {
        return hosts;
    }

    public CdoCredentials getCredential() {
        return credential;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getUsername() {
        return credential.getUserName();
    }

    public char[] getPassword() {
        return credential.getPassword();
    }

    public String getServer() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getConnectionString() {
        return connectionString;
    }
}
