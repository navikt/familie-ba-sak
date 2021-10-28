package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class TekniskOpphørAvFødselshendelseTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal teknisk opphøre fødselshendelse`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1998-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    )
                )
            )
        )
        behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        val restFagsakMedBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = scenario.søker.ident,
            behandlingType = BehandlingType.TEKNISK_OPPHØR,
            behandlingÅrsak = BehandlingÅrsak.TEKNISK_OPPHØR
        )
        generellAssertFagsak(
            restFagsak = restFagsakMedBehandling,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.VILKÅRSVURDERING
        )
        assertEquals(2, restFagsakMedBehandling.data?.behandlinger?.size)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakMedBehandling.data!!)

        // Setter alle vilkår til ikke-oppfylt på løpende førstegangsbehandling
        aktivBehandling.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = aktivBehandling.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.IKKE_OPPFYLT
                            )
                        )
                    )
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = aktivBehandling.behandlingId
        )
        val restFagsakEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = aktivBehandling.behandlingId
            )
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingsresultat,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
            behandlingResultat = BehandlingResultat.OPPHØRT
        )

        val restFagsakEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(fagsakId = restFagsakEtterBehandlingsresultat.data!!.id)
        generellAssertFagsak(
            restFagsak = restFagsakEtterSendTilBeslutter,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BESLUTTE_VEDTAK
        )

        val restFagsakEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                fagsakId = restFagsakEtterSendTilBeslutter.data!!.id,
                restBeslutningPåVedtak = RestBeslutningPåVedtak(
                    Beslutning.GODKJENT
                ),
                beslutterHeaders = HttpHeaders().apply {
                    setBearerAuth(
                        token(
                            mapOf(
                                "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                "azp" to "e2e-test",
                                "name" to "Mock McMockface Beslutter",
                                "preferred_username" to "mock.mcmockface.beslutter@nav.no"
                            )
                        )
                    )
                }
            )

        generellAssertFagsak(
            restFagsak = restFagsakEtterIverksetting,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = restFagsakEtterSendTilBeslutter.data!!.id)!!,
            søkerFnr = scenario.søker.ident,
            fagsakStatusEtterIverksetting = FagsakStatus.AVSLUTTET,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
