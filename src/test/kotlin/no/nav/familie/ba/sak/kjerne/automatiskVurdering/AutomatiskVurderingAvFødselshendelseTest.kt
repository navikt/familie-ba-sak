package no.nav.familie.ba.sak.kjerne.automatiskVurdering

import no.nav.familie.ba.sak.WebSpringAuthTestRunner
import no.nav.familie.ba.sak.common.nyOrdinærBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

//@ActiveProfiles("dev", "mock-pdl", "mock-arbeidsfordeling", "mock-infotrygd-barnetrygd")

@ActiveProfiles(
        "postgres",
        "mock-pdl-verdikjede-førstegangssøknad-nasjonal",
        "mock-oauth",
        "mock-arbeidsfordeling",
        "mock-tilbakekreving-klient",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
)
class AutomatiskVurderingAvFødselshendelseTestWebSpringAuthTestRunner : WebSpringAuthTestRunner() {

    private val barnFnr = "21111777001"
    private val søkerFnr = "04086226621"
    private val søker = mockSøkerAutomatiskBehandling
    private val nyBehandling = nyOrdinærBehandling(søkerFnr, BehandlingÅrsak.FØDSELSHENDELSE)

    //Husk å endre familiebasakklient tilbake!
    fun fødselshendelseKlient() = FødselshendelseKlient(
            baSakUrl = hentUrl(""),
            restOperations = restOperations,
            headers = hentHeaders()
    )

    @Test
    fun `Starter en ny behandling`() {
        fødselshendelseKlient().sendTilSak(nyBehandling)
    }
}