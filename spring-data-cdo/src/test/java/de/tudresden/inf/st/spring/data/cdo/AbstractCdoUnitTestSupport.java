package de.tudresden.inf.st.spring.data.cdo;

import de.tudresden.inf.st.spring.data.cdo.server.CDOStandaloneServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Dominik Grzelak
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations = "classpath:cdo-server.properties")
public abstract class AbstractCdoUnitTestSupport {

    static CDOStandaloneServer cdoStandaloneServer = null;
    //    static Thread cdoServerThread;
    private static boolean spawnServer;
    @Value("${spawnCDOStandalone}")
    private boolean spawnServerVal;
    private static String repoName;
    @Value("${repoName}")
    private String repoNameVal;

    @BeforeClass
    public static void setUpAll() {
    }

    protected static void spawnServer() throws Exception {
        if (spawnServer && cdoStandaloneServer == null) {
            cdoStandaloneServer = new CDOStandaloneServer(repoName);
            CDOStandaloneServer.start(cdoStandaloneServer);
        }
    }

    protected static void closeServer() {
        if (spawnServer && cdoStandaloneServer != null) {
            cdoStandaloneServer.stop();
        }
    }

    @Value("${spawnCDOStandalone}")
    public void setSpawnServerVal(boolean spawnServerVal) {
        AbstractCdoUnitTestSupport.spawnServer = spawnServerVal;
    }

    @Value("${repoName}")
    public void setRepoNameVal(String repoNameVal) {
        AbstractCdoUnitTestSupport.repoName = repoNameVal;
    }

    @AfterClass
    public static void finishAll() {
        closeServer();
    }
}
