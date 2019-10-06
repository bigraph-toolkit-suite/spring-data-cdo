package de.tudresden.inf.st.spring.data.cdo.connections;

import de.tudresden.inf.st.spring.data.cdo.CdoServerConnectionString;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;


/**
 * Parsing tests for the {@link CdoServerConnectionString} class.
 *
 * @author Dominik Grzelak
 */
public class ConnectionStringTests {

    @Test
    void parse_connection_string_tests_without_credentials() {
        String connection = "cdo://localhost:2036/repo1/sample/repo/path";
        CdoServerConnectionString cdoConnectionStr = new CdoServerConnectionString(connection);

        Assertions.assertEquals(1, cdoConnectionStr.getHosts().size());
        Assertions.assertEquals("localhost", cdoConnectionStr.getServer());
        Assertions.assertEquals(2036, cdoConnectionStr.getPort());
    }

    @Test
    void parse_connection_string_tests_with_credentials() {
        String connection = "cdo://user:pass@localhost:2036/repo1";
        CdoServerConnectionString cdoConnectionStr = new CdoServerConnectionString(connection);

        Assertions.assertEquals(1, cdoConnectionStr.getHosts().size());
        Assertions.assertEquals("localhost", cdoConnectionStr.getServer());
        Assertions.assertEquals(2036, cdoConnectionStr.getPort());
        Assertions.assertEquals("user", cdoConnectionStr.getUsername());
        Assertions.assertTrue("pass".contentEquals(new String(cdoConnectionStr.getPassword())));
    }

    @Test
    void parse_connection_string_tests_multiple_hosts() {
        String connection = "cdo://user:pass@localhost:2036,localhost:2037,127.0.0.1:2038/repo1";
        CdoServerConnectionString cdoConnectionStr = new CdoServerConnectionString(connection);

        Assertions.assertEquals(3, cdoConnectionStr.getHosts().size());
        Assertions.assertEquals("127.0.0.1", cdoConnectionStr.getServer());
        Assertions.assertEquals(2038, cdoConnectionStr.getPort());
        Assertions.assertEquals("user", cdoConnectionStr.getUsername());
        Assertions.assertTrue("pass".contentEquals(new String(cdoConnectionStr.getPassword())));
    }

    @Test
    void parse_malformed_connection_string_tests() {
        String connection = "cdo:/user:pass@localhost:2036,localhost:2037,127.0.0.1:2038/repo1";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CdoServerConnectionString(connection));
        String connection2 = "//user:pass@localhost:2036,localhost:2037,127.0.0.1:2038/repo1";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CdoServerConnectionString(connection2));
        String connection3 = "user:pass@localhost:2036,localhost:2037,127.0.0.1:2038/repo1";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CdoServerConnectionString(connection3));

        String connection4 = "cdo://localhost:2036//////";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CdoServerConnectionString(connection4));
        String connection5 = "cdo://localhost:2036,localhost:2032";
        Assertions.assertThrows(IllegalArgumentException.class, () -> new CdoServerConnectionString(connection5));
    }
}
