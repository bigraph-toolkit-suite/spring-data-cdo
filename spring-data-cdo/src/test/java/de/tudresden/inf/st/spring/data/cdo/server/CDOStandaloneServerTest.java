package de.tudresden.inf.st.spring.data.cdo.server;

import de.tudresden.inf.st.spring.data.cdo.CDOStandaloneServer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

@Disabled
public class CDOStandaloneServerTest {

    @Test
    void run_server_test_01() {
        CDOStandaloneServer server = new CDOStandaloneServer("repo1");
        try {
            CDOStandaloneServer.start(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void run_server_test_02() {
        CDOStandaloneServer server = new CDOStandaloneServer(new File("src/test/resources/config/cdo-server2.xml"));
        try {
            CDOStandaloneServer.start(server);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
