package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
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
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class TekniskEndringAvFødselshendelseTest(
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

        val restUtvidetBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = scenario.søker.ident,
            behandlingType = BehandlingType.TEKNISK_ENDRING,
            behandlingÅrsak = BehandlingÅrsak.TEKNISK_ENDRING
        )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING
        )

        val minimalFagsak = familieBaSakKlient().hentMinimalFagsakPåPerson(personIdent = scenario.søker.ident)
        assertEquals(2, minimalFagsak.data?.behandlinger?.size)

        // Setter alle vilkår til ikke-oppfylt på løpende førstegangsbehandling
        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
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
            behandlingId = restUtvidetBehandling.data!!.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsresultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
            behandlingResultat = BehandlingResultat.OPPHØRT
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
            behandlingResultat = BehandlingResultat.OPPHØRT
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data?.behandlingId!!)

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK
        )

        val restUtvidetBehandlingEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
                restBeslutningPåVedtak = RestBeslutningPåVedtak(
                    Beslutning.GODKJENT
                ),
                beslutterHeaders = HttpHeaders().apply {
                    setBearerAuth(
                        token(
                            mapOf(
                                "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                "azp" to "azp-test",
                                "name" to "Mock McMockface Beslutter",
                                "preferred_username" to "mock.mcmockface.beslutter@nav.no"
                            )
                        )
                    )
                }
            )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hent(behandlingId = restUtvidetBehandlingEtterIverksetting.data?.behandlingId!!),
            søkerFnr = scenario.søker.ident,
            fagsakStatusEtterIverksetting = FagsakStatus.AVSLUTTET,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
