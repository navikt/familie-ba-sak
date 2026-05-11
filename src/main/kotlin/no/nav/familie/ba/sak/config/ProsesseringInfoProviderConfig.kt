package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.sikkerhet.Rolle.PROSESSERING
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.harRolle
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerEpost
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProsesseringInfoProviderConfig {
    @Bean
    fun prosesseringInfoProvider() =
        object : ProsesseringInfoProvider {
            override fun hentBrukernavn(): String = hentSaksbehandlerEpost()

            override fun harTilgang(): Boolean = harRolle(PROSESSERING)
        }
}
