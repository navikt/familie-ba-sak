package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.PersonResultatDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutobrevOmregningPgaAlderService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.ba.sak.task.dto.AutobrevPgaAlderDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate
import java.time.YearMonth

class TriggingAvAutobrevOmregningPgaAlderTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val autobrevOmregningPgaAlderService: AutobrevOmregningPgaAlderService,
    @Autowired private val brevmalService: BrevmalService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
) : AbstractVerdikjedetest() {
    @Test
    fun `Omregning og autobrev skal ikke kjøre hvis behandling er begrunnet for 18 åringer`() {
        val behandlinger = kjørFørstegangsbehandlingOgTriggAutobrev(Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR)
        assertThat(behandlinger).hasSize(1)
        assertThat(behandlinger.filter { it.opprettetÅrsak == BehandlingÅrsak.SØKNAD }).hasSize(1)
    }

    @Test
    fun `Omregning og autobrev skal kjøre hvis behandling IKKE er begrunnet for 18 åringer`() {
        val behandlinger = kjørFørstegangsbehandlingOgTriggAutobrev(null)
        assertThat(behandlinger.size).isEqualTo(2)
        assertThat(behandlinger.filter { it.opprettetÅrsak == BehandlingÅrsak.SØKNAD }).hasSize(1)
        assertThat(behandlinger.filter { it.opprettetÅrsak == BehandlingÅrsak.OMREGNING_18ÅR }).hasSize(1)
    }

    fun kjørFørstegangsbehandlingOgTriggAutobrev(
        årMedReduksjonsbegrunnelse: Standardbegrunnelse?,
    ): List<Behandling> {
        val scenario =
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-11-12", fornavn = "Mor", etternavn = "Søker"),
                barna =
                    listOf(
                        RestScenarioPerson(
                            fødselsdato = LocalDate.now().minusYears(2).toString(),
                            fornavn = "Toåringen",
                            etternavn = "Barnesen",
                        ),
                        RestScenarioPerson(
                            fødselsdato = LocalDate.now().minusYears(6).toString(),
                            fornavn = "Seksåringen",
                            etternavn = "Barnesen",
                        ),
                        RestScenarioPerson(
                            fødselsdato = LocalDate.now().minusYears(18).toString(),
                            fornavn = "Attenåringen",
                            etternavn = "Barnesen",
                        ),
                    ),
            ).also { stubScenario(it) }

        val fagsakId = familieBaSakKlient().opprettFagsak(søkersIdent = scenario.søker.ident).data?.id!!
        familieBaSakKlient().opprettBehandling(søkersIdent = scenario.søker.ident, fagsakId = fagsakId)

        val restFagsakEtterOpprettelse = familieBaSakKlient().hentFagsak(fagsakId = fagsakId)

        val aktivBehandling = hentAktivBehandling(restFagsak = restFagsakEtterOpprettelse.data!!)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = scenario.søker.ident,
                        barnasIdenter = scenario.barna.map { it.ident },
                    ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = aktivBehandling.behandlingId,
                restRegistrerSøknad = restRegistrerSøknad,
            )

        // Godkjenner alle vilkår på førstegangsbehandling.
        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    personResultatDto =
                        PersonResultatDto(
                            personIdent = restPersonResultat.personIdent,
                            vilkårResultater =
                                listOf(
                                    it.copy(
                                        resultat = Resultat.OPPFYLT,
                                        periodeFom = LocalDate.now().minusMonths(2),
                                    ),
                                ),
                        ),
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId,
        )

        val restUtvidetBehandlingEtterBehandlingsresultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId,
            )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsresultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse"),
            )

        val vedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentRestUtvidetVedtaksperiodeMedBegrunnelser(
                restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId,
            )

        val førsteVedtaksperiode = vedtaksperioderMedBegrunnelser.sortedBy { it.fom }.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = førsteVedtaksperiode.id,
            restPutVedtaksperiodeMedStandardbegrunnelser =
                RestPutVedtaksperiodeMedStandardbegrunnelser(
                    standardbegrunnelser =
                        listOf(
                            Standardbegrunnelse.INNVILGET_BOR_HOS_SØKER.enumnavnTilString(),
                        ),
                ),
        )
        val reduksjonVedtaksperiodeId =
            vedtaksperioderMedBegrunnelser.single {
                it.fom!!.isEqual(
                    LocalDate.now().førsteDagIInneværendeMåned(),
                ) &&
                    it.type == Vedtaksperiodetype.UTBETALING
            }

        if (årMedReduksjonsbegrunnelse != null) {
            familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
                vedtaksperiodeId = reduksjonVedtaksperiodeId.id,
                restPutVedtaksperiodeMedStandardbegrunnelser =
                    RestPutVedtaksperiodeMedStandardbegrunnelser(
                        standardbegrunnelser = listOf(årMedReduksjonsbegrunnelse.enumnavnTilString()),
                    ),
            )
        }

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

        familieBaSakKlient().iverksettVedtak(
            behandlingId = restUtvidetBehandlingEtterSendTilBeslutter.data!!.behandlingId,
            restBeslutningPåVedtak =
                RestBeslutningPåVedtak(
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

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId = fagsakId)!!,
            søkerFnr = scenario.søker.ident,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            brevmalService = brevmalService,
        )

        autobrevOmregningPgaAlderService.opprettOmregningsoppgaveForBarnIBrytingsalder(
            autobrevPgaAlderDTO =
                AutobrevPgaAlderDTO(
                    fagsakId = fagsakId,
                    alder = 18,
                    årMåned = YearMonth.now(),
                ),
        )

        return behandlingHentOgPersisterService.hentBehandlinger(fagsakId)
    }
}
