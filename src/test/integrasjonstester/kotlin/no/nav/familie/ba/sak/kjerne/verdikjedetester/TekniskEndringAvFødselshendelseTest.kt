package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.TilbakekrevingDto
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.BeslutningPåVedtakDto
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.ScenarioPersonDto
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
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
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {
    @Test
    fun `Skal teknisk opphøre fødselshendelse`() {
        System.setProperty(FeatureToggle.TEKNISK_ENDRING.navn, "true")

        val scenario =
            ScenarioDto(
                søker = ScenarioPersonDto(fødselsdato = "1998-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        ScenarioPersonDto(
                            fødselsdato = LocalDate.now().minusDays(2).toString(),
                            fornavn = "Barn",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }
        val behandling =
            behandleFødselshendelse(
                nyBehandlingHendelse =
                    NyBehandlingHendelse(
                        morsIdent = scenario.søker.ident,
                        barnasIdenter = listOf(scenario.barna.first().ident),
                    ),
                behandleFødselshendelseTask = behandleFødselshendelseTask,
                fagsakService = fagsakService,
                behandlingHentOgPersisterService = behandlingHentOgPersisterService,
                personidentService = personidentService,
                vedtakService = vedtakService,
                stegService = stegService,
                brevmalService = brevmalService,
            )!!

        val utvidetBehandlingDto =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = scenario.søker.ident,
                behandlingType = BehandlingType.TEKNISK_ENDRING,
                behandlingÅrsak = BehandlingÅrsak.TEKNISK_ENDRING,
                fagsakId = behandling.fagsak.id,
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDto,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING,
        )

        val minimalFagsak = familieBaSakKlient().hentMinimalFagsakPåPerson(personIdent = scenario.søker.ident)
        assertEquals(2, minimalFagsak.data?.behandlinger?.size)

        // Setter alle vilkår til ikke-oppfylt på løpende førstegangsbehandling
        utvidetBehandlingDto.data!!.personResultater.forEach { personResultatDto ->
            personResultatDto.vilkårResultater.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = utvidetBehandlingDto.data!!.behandlingId,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = personResultatDto.personIdent,
                            vilkårResultater =
                                listOf(
                                    it.copy(
                                        resultat = Resultat.IKKE_OPPFYLT,
                                    ),
                                ),
                        ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = utvidetBehandlingDto.data!!.behandlingId,
        )

        val utvidetBehandlingEtterBehandlingsresultatDto =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = utvidetBehandlingDto.data!!.behandlingId,
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingEtterBehandlingsresultatDto,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING,
            behandlingsresultat = Behandlingsresultat.OPPHØRT,
        )

        val utvidetBehandlingDtoEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                utvidetBehandlingEtterBehandlingsresultatDto.data!!.behandlingId,
                TilbakekrevingDto(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )
        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER,
            behandlingsresultat = Behandlingsresultat.OPPHØRT,
        )

        val utvidetBehandlingDtoEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = utvidetBehandlingDtoEtterVurderTilbakekreving.data?.behandlingId!!)

        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterSendTilBeslutter,
            behandlingStatus = BehandlingStatus.FATTER_VEDTAK,
            behandlingStegType = StegType.BESLUTTE_VEDTAK,
        )

        val utvidetBehandlingDtoEtterIverksetting =
            familieBaSakKlient().iverksettVedtak(
                behandlingId = utvidetBehandlingDtoEtterSendTilBeslutter.data!!.behandlingId,
                beslutningPåVedtakDto =
                    BeslutningPåVedtakDto(
                        Beslutning.GODKJENT,
                    ),
                beslutterHeaders =
                    HttpHeaders().apply {
                        setBearerAuth(
                            token(
                                mapOf(
                                    "groups" to listOf("SAKSBEHANDLER", "BESLUTTER"),
                                    "azp" to "azp-test",
                                    "name" to "Mock McMockface Beslutter",
                                    "NAVident" to "Z0000",
                                ),
                            ),
                        )
                    },
            )

        generellAssertUtvidetBehandlingDto(
            utvidetBehandlingDto = utvidetBehandlingDtoEtterIverksetting,
            behandlingStatus = BehandlingStatus.IVERKSETTER_VEDTAK,
            behandlingStegType = StegType.IVERKSETT_MOT_OPPDRAG,
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.hent(behandlingId = utvidetBehandlingDtoEtterIverksetting.data?.behandlingId!!),
            søkerFnr = scenario.søker.ident,
            fagsakStatusEtterIverksetting = FagsakStatus.AVSLUTTET,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )
    }
}
