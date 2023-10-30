package org.bigraphs.spring.data.cdo.connections;

import org.bigraphs.spring.data.cdo.CdoClient;
import org.bigraphs.spring.data.cdo.CdoClientSession;
import org.bigraphs.spring.data.cdo.CdoClients;
import org.bigraphs.spring.data.cdo.config.CdoClientSessionOptions;
import org.eclipse.emf.cdo.session.CDORepositoryInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CdoClientConnectionTests {

    @Test
    public void cdoClient_test_01() throws InterruptedException {
        CdoClientSessionOptions options = CdoClientSessionOptions.builder()
                .setRepository("repo1")
                .build();
        CdoClient cdoClient = CdoClients.create("localhost", 2036);
        assert !cdoClient.isConnected();
        CdoClientSession cdoClientSession = cdoClient.startSession(options);
        assert cdoClient.isConnected();
        CDORepositoryInfo repositoryInfo = cdoClientSession.getCdoSession().getRepositoryInfo();
        System.out.println(repositoryInfo.getName());
        cdoClient.close();
        assert !cdoClient.isConnected();
    }
}
