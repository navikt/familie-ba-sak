package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.EfSakRestClientMock
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteSmåbarnstilleggSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.tilleggOrdinærSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.VedtakOmOvergangsstønadService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate

// Todo. Bruker every. Dette endrer funksjonalliteten for alle klasser.
@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BehandleSmåbarnstilleggTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val featureToggleService: FeatureToggleService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val vedtakOmOvergangsstønadService: VedtakOmOvergangsstønadService
) : AbstractVerdikjedetest(efSakRestClient = efSakRestClient) {

    private val barnFødselsdato = LocalDate.now().minusYears(3)
    private val periodeMedFullOvergangsstønadFom = barnFødselsdato.plusYears(1)

    lateinit var scenario: RestScenario

    @BeforeAll
    fun init() {
        scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                        bostedsadresser = emptyList()
                    )
                )
            )
        )
    }

    @Test
    @Order(1)
    fun `Skal behandle utvidet nasjonal sak med småbarnstillegg`() {
        every { featureToggleService.isEnabled(any()) } returns true

        val søkersIdent = scenario.søker.ident!!

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = barnFødselsdato.plusYears(18),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        val fagsak = familieBaSakKlient().opprettFagsak(søkersIdent = søkersIdent)
        val restBehandling = familieBaSakKlient().opprettBehandling(
            søkersIdent = søkersIdent,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET
        )

        val behandling = behandlingService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = søkersIdent,
                    barnasIdenter = scenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandling,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VILKÅRSVURDERING
        )

        restUtvidetBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.data!!.behandlingId,
                    vilkårId = it.id,
                    restPersonResultat =
                    RestPersonResultat(
                        personIdent = restPersonResultat.personIdent,
                        vilkårResultater = listOf(
                            it.copy(
                                resultat = Resultat.OPPFYLT,
                                periodeFom = barnFødselsdato
                            )
                        )
                    )
                )
            }
        }

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.data!!.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.data!!.behandlingId
            )

        assertEquals(
            tilleggOrdinærSatsTilTester.beløp + sisteUtvidetSatsTilTester.beløp + sisteSmåbarnstilleggSatsTilTester.beløp,
            hentNåværendeEllerNesteMånedsUtbetaling(
                behandling = restUtvidetBehandlingEtterBehandlingsResultat.data!!
            )
        )

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId
            )
        val utvidedeAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.UTVIDET_BARNETRYGD }
        val småbarnstilleggAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.SMÅBARNSTILLEGG }

        assertEquals(
            barnFødselsdato.plusMonths(1).toYearMonth(),
            utvidedeAndeler.minByOrNull { it.stønadFom }?.stønadFom
        )
        assertEquals(
            periodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndeler.minByOrNull { it.stønadFom }?.stønadFom
        )
        assertEquals(
            barnFødselsdato.plusYears(3).toYearMonth(),
            småbarnstilleggAndeler.maxByOrNull { it.stønadFom }?.stønadTom
        )

        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterBehandlingsResultat,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.VURDER_TILBAKEKREVING
        )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
            )
        generellAssertRestUtvidetBehandling(
            restUtvidetBehandling = restUtvidetBehandlingEtterVurderTilbakekreving,
            behandlingStatus = BehandlingStatus.UTREDES,
            behandlingStegType = StegType.SEND_TIL_BESLUTTER
        )

        val vedtaksperiodeId =
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.first()
        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = vedtaksperiodeId.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER
                )
            )
        )

        val restUtvidetBehandlingEtterSendTilBeslutter =
            familieBaSakKlient().sendTilBeslutter(behandlingId = restUtvidetBehandlingEtterVurderTilbakekreving.data!!.behandlingId)

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
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsak.data!!.id)!!,
            søkerFnr = søkersIdent,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }

    @Test
    @Order(2)
    fun `Skal ikke opprette behandling når det ikke finnes endringer på perioder med full overgangsstønad`() {
        val søkersIdent = scenario.søker.ident!!
        vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(personIdent = søkersIdent)
        val fagsak = fagsakService.hentFagsakPåPerson(identer = setOf(PersonIdent(søkersIdent)))
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak!!.id)!!

        assertEquals(BehandlingStatus.AVSLUTTET, aktivBehandling.status)
        assertNotEquals(BehandlingÅrsak.SMÅBARNSTILLEGG, aktivBehandling.opprettetÅrsak)
    }

    @Test
    @Order(3)
    fun `Skal automatisk endre småbarnstilleggperioder`() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)
        val søkersIdent = scenario.søker.ident!!

        val nyPeriodeMedFullOvergangsstønadFom = periodeMedFullOvergangsstønadFom.plusMonths(3)
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersIdent,
                    fomDato = nyPeriodeMedFullOvergangsstønadFom,
                    tomDato = barnFødselsdato.plusYears(18),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )
        vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(personIdent = søkersIdent)

        val fagsak = fagsakService.hentFagsakPåPerson(identer = setOf(PersonIdent(søkersIdent)))
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak!!.id)!!

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = aktivBehandling.id
            )
        val småbarnstilleggAndeler = andelerTilkjentYtelse.filter { it.type == YtelseType.SMÅBARNSTILLEGG }
        assertEquals(
            nyPeriodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndeler.minByOrNull { it.stønadFom }?.stønadFom
        )
        assertEquals(
            barnFødselsdato.plusYears(3).toYearMonth(),
            småbarnstilleggAndeler.maxByOrNull { it.stønadFom }?.stønadTom
        )

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = søkersIdent,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
