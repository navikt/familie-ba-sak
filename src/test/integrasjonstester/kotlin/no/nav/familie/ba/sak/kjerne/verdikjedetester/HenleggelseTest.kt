package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.brev.domene.ManueltBrevRequest
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.logg.LoggType
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class HenleggelseTest(
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
) : AbstractVerdikjedetest() {
    val restScenario =
        RestScenario(
            søker =
                RestScenarioPerson(
                    _ident = randomFnr(LocalDate.of(1990, 4, 20)),
                    fødselsdato = "1990-04-20",
                    fornavn = "Mor",
                    etternavn = "Søker",
                ),
            barna =
                listOf(
                    RestScenarioPerson(
                        _ident = randomFnr(LocalDate.now().minusMonths(2)),
                        fødselsdato = LocalDate.now().minusMonths(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
        )

    @BeforeEach
    fun init() {
        stubScenario(scenario = restScenario)
    }

    @Test
    fun `Opprett behandling, henlegg behandling feilaktig opprettet og opprett behandling på nytt`() {
        val førsteBehandling = opprettBehandlingOgRegistrerSøknad(restScenario)

        val responseHenlagtSøknad =
            familieBaSakKlient().henleggSøknad(
                førsteBehandling.behandlingId,
                RestHenleggBehandlingInfo(
                    årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                    begrunnelse = "feilaktig opprettet",
                ),
            )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = responseHenlagtSøknad,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
        )

        val ferdigstiltBehandling =
            behandlingHentOgPersisterService.hent(behandlingId = responseHenlagtSøknad.data!!.behandlingId)

        assertThat(!ferdigstiltBehandling.aktiv)
        assertThat(ferdigstiltBehandling.resultat == Behandlingsresultat.HENLAGT_FEILAKTIG_OPPRETTET)

        val behandlingslogg = familieBaSakKlient().hentBehandlingslogg(responseHenlagtSøknad.data!!.behandlingId)
        assertEquals(Ressurs.Status.SUKSESS, behandlingslogg.status)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.HENLEGG_BEHANDLING }?.size == 1)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.DISTRIBUERE_BREV }?.size == 0)

        val andreBehandling = opprettBehandlingOgRegistrerSøknad(restScenario)
        assertEquals(andreBehandling.status, BehandlingStatus.UTREDES)
    }

    @Test
    fun `Opprett behandling, hent forhåndsvising av brev, henlegg behandling søknad trukket`() {
        val førsteBehandling = opprettBehandlingOgRegistrerSøknad(restScenario)

        /**
         * Denne forhåndsvisningen går ikke til sanity for øyeblikket, men det er en mulighet å legge til
         * familie-brev som docker-container og mocke ut pdf-generering for å teste mapping mot sanity.
         */
        val responseForhandsvis =
            familieBaSakKlient().forhaandsvisHenleggelseBrev(
                behandlingId = førsteBehandling.behandlingId,
                manueltBrevRequest =
                    ManueltBrevRequest(
                        brevmal = Brevmal.HENLEGGE_TRUKKET_SØKNAD,
                    ),
            )
        assertThat(responseForhandsvis.status == Ressurs.Status.SUKSESS)

        val responseHenlagtSøknad =
            familieBaSakKlient().henleggSøknad(
                førsteBehandling.behandlingId,
                RestHenleggBehandlingInfo(
                    årsak = HenleggÅrsak.SØKNAD_TRUKKET,
                    begrunnelse = "Søknad trukket",
                ),
            )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = responseHenlagtSøknad,
            behandlingStatus = BehandlingStatus.AVSLUTTET,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET,
        )

        val ferdigstiltBehandling =
            behandlingHentOgPersisterService.hent(behandlingId = responseHenlagtSøknad.data!!.behandlingId)

        assertThat(!ferdigstiltBehandling.aktiv)
        assertThat(ferdigstiltBehandling.resultat == Behandlingsresultat.HENLAGT_SØKNAD_TRUKKET)

        val behandlingslogg = familieBaSakKlient().hentBehandlingslogg(responseHenlagtSøknad.data!!.behandlingId)
        assertEquals(Ressurs.Status.SUKSESS, behandlingslogg.status)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.HENLEGG_BEHANDLING }?.size == 1)
        assertThat(behandlingslogg.data?.filter { it.type == LoggType.DISTRIBUERE_BREV }?.size == 1)
    }

    private fun opprettBehandlingOgRegistrerSøknad(scenario: RestScenario): RestUtvidetBehandling {
        val søkersIdent = scenario.søker.ident
        val barn1 = scenario.barna[0].ident
        val fagsak = familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val restFagsakMedBehandling =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = søkersIdent,
                fagsakId = fagsak.data!!.id,
            )

        val behandling = behandlingHentOgPersisterService.hent(restFagsakMedBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = søkersIdent,
                        barnasIdenter = listOf(barn1),
                    ),
                bekreftEndringerViaFrontend = false,
            )
        return familieBaSakKlient()
            .registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad,
            ).data!!
    }
}
