package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.dokument.domene.BrevType
import no.nav.familie.ba.sak.kjerne.dokument.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class HenleggelseTest(
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val stegService: StegService
) : AbstractVerdikjedetest() {

    val restScenario = RestScenario(
        søker = RestScenarioPerson(fødselsdato = "1990-04-20", fornavn = "Mor", etternavn = "Søker"),
        barna = listOf(
            RestScenarioPerson(
                fødselsdato = LocalDate.now().minusMonths(2).toString(),
                fornavn = "Barn",
                etternavn = "Barnesen"
            )
        )
    )

    @Test
    fun `Opprett behandling, henlegg behandling feilaktig opprettet og opprett behandling på nytt`() {
        val scenario = mockServerKlient().lagScenario(restScenario)

        val førsteBehandling = opprettBehandlingOgRegistrerSøknad(scenario)

        val responseHenlagtSøknad = familieBaSakKlient().henleggSøknad(
            førsteBehandling.behandlingId,
            RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                begrunnelse = "feilaktig opprettet"
            )
        )

        generellAssertFagsak(
            restFagsak = responseHenlagtSøknad,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.FERDIGSTILLE_BEHANDLING
        )

        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(
            behandling = behandlingService.hentAktivForFagsak(
                fagsakId = responseHenlagtSøknad.data!!.id
            )!!
        )

        assertThat(!ferdigstiltBehandling.aktiv)
        assertThat(ferdigstiltBehandling.resultat == BehandlingResultat.HENLAGT_FEILAKTIG_OPPRETTET)

        val behandlingslogg = familieBaSakKlient().hentBehandlingslogg(responseHenlagtSøknad.data!!.id)
        assertEquals(Ressurs.Status.SUKSESS, behandlingslogg.status)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.HENLEGG_BEHANDLING }?.size == 1)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.DISTRIBUERE_BREV }?.size == 0)

        val andreBehandling = opprettBehandlingOgRegistrerSøknad(scenario)
        assertThat(andreBehandling.aktiv).isTrue
    }

    @Test
    fun `Opprett behandling, hent forhåndsvising av brev, henlegg behandling søknad trukket`() {
        val scenario = mockServerKlient().lagScenario(restScenario)
        val førsteBehandling = opprettBehandlingOgRegistrerSøknad(scenario)

        /**
         * Denne forhåndsvisningen går ikke til sanity for øyeblikket, men det er en mulighet å legge til
         * familie-brev som docker-container og mocke ut pdf-generering for å teste mapping mot sanity.
         */
        val responseForhandsvis = familieBaSakKlient().forhaandsvisHenleggelseBrev(
            behandlingId = førsteBehandling.behandlingId,
            manueltBrevRequest = ManueltBrevRequest(
                mottakerIdent = scenario.søker.ident!!,
                brevmal = BrevType.HENLEGGE_TRUKKET_SØKNAD
            )
        )
        assertThat(responseForhandsvis?.status == Ressurs.Status.SUKSESS)

        val responseHenlagtSøknad = familieBaSakKlient().henleggSøknad(
            førsteBehandling.behandlingId,
            RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.SØKNAD_TRUKKET,
                begrunnelse = "Søknad trukket"
            )
        )

        generellAssertFagsak(
            restFagsak = responseHenlagtSøknad,
            fagsakStatus = FagsakStatus.OPPRETTET,
            behandlingStegType = StegType.FERDIGSTILLE_BEHANDLING
        )

        val ferdigstiltBehandling = stegService.håndterFerdigstillBehandling(
            behandling = behandlingService.hentAktivForFagsak(
                fagsakId = responseHenlagtSøknad.data!!.id
            )!!
        )

        assertThat(!ferdigstiltBehandling.aktiv)
        assertThat(ferdigstiltBehandling.resultat == BehandlingResultat.HENLAGT_SØKNAD_TRUKKET)

        val behandlingslogg = familieBaSakKlient().hentBehandlingslogg(responseHenlagtSøknad.data!!.id)
        assertEquals(Ressurs.Status.SUKSESS, behandlingslogg.status)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.HENLEGG_BEHANDLING }?.size == 1)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.DISTRIBUERE_BREV }?.size == 1)
    }

    private fun opprettBehandlingOgRegistrerSøknad(scenario: RestScenario): RestUtvidetBehandling {
        val søkersIdent = scenario.søker.ident!!
        val barn1 = scenario.barna[0].ident!!
        familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val restFagsakMedBehandling = familieBaSakKlient().opprettBehandling(søkersIdent = søkersIdent)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakMedBehandling.data!!)
        val restRegistrerSøknad = RestRegistrerSøknad(
            søknad = lagSøknadDTO(
                søkerIdent = søkersIdent,
                barnasIdenter = listOf(barn1)
            ),
            bekreftEndringerViaFrontend = false
        )
        val fagsakEtterRegistrerSøknad = familieBaSakKlient().registrererSøknad(
            behandlingId = aktivBehandling!!.behandlingId,
            restRegistrerSøknad = restRegistrerSøknad
        )
        return hentAktivBehandling(restFagsak = fagsakEtterRegistrerSøknad.data!!)!!
    }
}
