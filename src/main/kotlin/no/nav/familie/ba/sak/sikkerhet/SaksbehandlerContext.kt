package no.nav.familie.ba.sak.sikkerhet

import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SaksbehandlerContext(
    @Value("\${rolle.kode6}") private val kode6GruppeId: String,
    private val integrasjonClient: IntegrasjonClient,
    private val unleashNextMedContextService: UnleashNextMedContextService,
) {
    private val logger = LoggerFactory.getLogger(SaksbehandlerContext::class.java)

    fun hentSaksbehandlerSignaturTilBrev(): String {
        val grupper = SikkerhetContext.hentGrupper()

        return if (grupper.contains(kode6GruppeId)) {
            ""
        } else {
            val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()

            return try {
                val saksbehandler = integrasjonClient.hentSaksbehandler(saksbehandlerIdent)

                "${saksbehandler.fornavn} ${saksbehandler.etternavn}".trim()
            } catch (exception: Exception) {
                logger.warn("Oppstod feil ved forsøk på å hente saksbehandler signatur til brev ($saksbehandlerIdent), bruker navn fra token.")
                SikkerhetContext.hentSaksbehandlerNavn()
            }
        }
    }
}
