package no.nav.familie.ba.sak.config;

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Configuration
@Profile("mock-oauth")
public class OAuth2AccessTokenTestConfig {

    @Bean
    @Primary
    public OAuth2AccessTokenService oAuth2AccessTokenServiceMock() {
        OAuth2AccessTokenService tokenMockService = Mockito.mock(OAuth2AccessTokenService.class);
        when(tokenMockService.getAccessToken(any())).thenReturn(new OAuth2AccessTokenResponse("Mock-token-response", 60, 60, null));
        return tokenMockService;
    }
}
