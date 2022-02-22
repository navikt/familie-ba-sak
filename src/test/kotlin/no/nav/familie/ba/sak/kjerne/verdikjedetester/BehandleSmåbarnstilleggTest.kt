package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.EfSakRestClientMock
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.VedtakOmOvergangsstønadService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.RestHenleggBehandlingInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteSmåbarnstilleggSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.sisteUtvidetSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.SatsService.tilleggOrdinærSatsTilTester
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate
import java.time.YearMonth

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
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val vedtakOmOvergangsstønadService: VedtakOmOvergangsstønadService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val opprettTaskService: OpprettTaskService
) : AbstractVerdikjedetest() {

    private val barnFødselsdato = LocalDate.now().minusYears(2)
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

    private fun settOppefSakMockForDeFørste2Testene(søkersIdent: String) {

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
    }

    @Test
    @Order(1)
    fun `Skal behandle utvidet nasjonal sak med småbarnstillegg`() {
        every { featureToggleService.isEnabled(any()) } returns true
        val søkersIdent = scenario.søker.ident!!
        settOppefSakMockForDeFørste2Testene(søkersIdent)

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
        val småbarnstilleggAndel = andelerTilkjentYtelse.single { it.type == YtelseType.SMÅBARNSTILLEGG }

        assertEquals(
            barnFødselsdato.plusMonths(1).toYearMonth(),
            utvidedeAndeler.minByOrNull { it.stønadFom }?.stønadFom
        )
        assertEquals(
            periodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndel.stønadFom
        )
        assertEquals(
            barnFødselsdato.plusYears(3).toYearMonth(),
            småbarnstilleggAndel.stønadTom
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
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.sortedBy { it.fom }
                .first()
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
                                "NAVident" to "Z0000"
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
        settOppefSakMockForDeFørste2Testene(søkersIdent)

        val søkersAktør = personidentService.hentAktør(søkersIdent)
        vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(aktør = søkersAktør)
        val fagsak = fagsakService.hentFagsakPåPerson(aktør = søkersAktør)
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak!!.id)!!

        assertEquals(BehandlingStatus.AVSLUTTET, aktivBehandling.status)
        assertNotEquals(BehandlingÅrsak.SMÅBARNSTILLEGG, aktivBehandling.opprettetÅrsak)
    }

    @Test
    @Order(3)
    fun `Skal stoppe automatisk behandling som må fortsette manuelt pga tilbakekreving`() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)

        val søkersAktør = personidentService.hentAktør(scenario.søker.aktørId!!)

        val periodeOvergangsstønadTom = LocalDate.now().minusMonths(3)
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersAktør.aktivFødselsnummer(),
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = periodeOvergangsstønadTom,
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                ),
            )
        )
        vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(aktør = søkersAktør)

        val fagsak = fagsakService.hentFagsakPåPerson(aktør = søkersAktør)
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak!!.id)!!

        // Vedtaksperioder skal være slettet etter at den er blitt omgjort til manuell behandling
        assertEquals(
            0,
            vedtaksperiodeService.hentPersisterteVedtaksperioder(
                vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = aktivBehandling.id)
            ).size
        )

        verify(exactly = 1) {
            opprettTaskService.opprettOppgaveTask(
                behandlingId = aktivBehandling.id,
                oppgavetype = Oppgavetype.VurderLivshendelse,
                beskrivelse = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt"
            )
        }

        assertEquals(StegType.BEHANDLINGSRESULTAT, aktivBehandling.steg)
        assertEquals(BehandlingStatus.UTREDES, aktivBehandling.status)

        val behandlingEtterHenleggelse = stegService.håndterHenleggBehandling(
            behandling = aktivBehandling,
            henleggBehandlingInfo = RestHenleggBehandlingInfo(
                årsak = HenleggÅrsak.FEILAKTIG_OPPRETTET,
                begrunnelse = ""
            )
        )
        assertEquals(false, behandlingEtterHenleggelse.aktiv)
    }

    @Test
    @Order(4)
    fun `Skal automatisk endre småbarnstilleggperioder`() {
        EfSakRestClientMock.clearEfSakRestMocks(efSakRestClient)

        val søkersIdent = scenario.søker.ident!!
        val søkersAktør = personidentService.hentAktør(søkersIdent)

        val periodeOvergangsstønadTom = LocalDate.now()
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = søkersIdent,
                    fomDato = periodeMedFullOvergangsstønadFom,
                    tomDato = periodeOvergangsstønadTom,
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                ),
            )
        )
        vedtakOmOvergangsstønadService.håndterVedtakOmOvergangsstønad(aktør = søkersAktør)

        val fagsak = fagsakService.hentFagsakPåPerson(aktør = søkersAktør)
        val aktivBehandling = behandlingService.hentAktivForFagsak(fagsakId = fagsak!!.id)!!

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = aktivBehandling.id
            )
        val småbarnstilleggAndel = andelerTilkjentYtelse.single { it.type == YtelseType.SMÅBARNSTILLEGG }
        assertEquals(
            periodeMedFullOvergangsstønadFom.toYearMonth(),
            småbarnstilleggAndel.stønadFom
        )
        assertEquals(
            periodeOvergangsstønadTom.toYearMonth(),
            småbarnstilleggAndel.stønadTom
        )

        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(
            vedtak = vedtakService.hentAktivForBehandlingThrows(behandlingId = aktivBehandling.id)
        )

        val aktuellVedtaksperiode =
            vedtaksperioderMedBegrunnelser.find { it.fom?.toYearMonth() == YearMonth.now().nesteMåned() }
        assertNotNull(aktuellVedtaksperiode)
        assertTrue(aktuellVedtaksperiode?.begrunnelser?.any { it.vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD } == true)

        håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = søkersIdent,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
