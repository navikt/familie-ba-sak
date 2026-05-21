package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentGrupper
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.hentSaksbehandlerEpost
import no.nav.familie.prosessering.config.ProsesseringInfoProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ProsesseringInfoProviderConfig(
    @param:Value("\${prosessering.rolle}") private val prosesseringRolle: String,
) {
    @Bean
    fun prosesseringInfoProvider() =
        object : ProsesseringInfoProvider {
            override fun hentBrukernavn(): String = hentSaksbehandlerEpost()

            override fun harTilgang(): Boolean = hentGrupper().contains(prosesseringRolle)
        }
}
