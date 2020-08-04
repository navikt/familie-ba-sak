package no.nav.familie.ba.sak.config;

import no.nav.familie.http.sts.StsRestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
public class StsTestConfig {
    @Bean
    @Profile("mock-sts")
    public StsRestClient stsRestClientMock() {
        StsRestClient client = mock(StsRestClient.class);

        when(client.getSystemOIDCToken()).thenReturn("MOCKED-OIDC-TOKEN");
        return client;
    }
}