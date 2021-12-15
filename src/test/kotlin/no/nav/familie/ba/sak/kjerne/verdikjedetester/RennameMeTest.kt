package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.VedtakOmOvergangsstønadService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate

@DirtiesContext
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RennameMeTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val featureToggleService: FeatureToggleService,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val vedtakOmOvergangsstønadService: VedtakOmOvergangsstønadService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val økonomiKlient: ØkonomiKlient,
    @Autowired private val opprettTaskService: OpprettTaskService
) : AbstractVerdikjedetest(efSakRestClient = efSakRestClient, økonomiKlient = økonomiKlient) {

    @Test
    @Order(1)
    fun `TODO renname me hente alle gjeldene behandlinger med småbarnstillegg opphør denne måneden`() {

        // Opprett innslag av fagsaker og behandlinger i databasen

        val barnFødselsdato1: LocalDate = LocalDate.now().minusYears(2)
        val personScenario1 = lagScenario(barnFødselsdato1)
        val fagsak1: RestMinimalFagsak = lagFagsak(personScenario = personScenario1)
        val beh1 = lagFullførtBehandling(
            fagsak = fagsak1,
            opphørAvSmåbarnstilleggNå = false,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato1,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        )
        val beh2 = lagRevurderingBehandling(
            fagsak = fagsak1,
            opphørAvSmåbarnstilleggNå = true,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato1,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG
        )

        val barnFødselsdato2: LocalDate = LocalDate.now().minusYears(2)
        val personScenario2 = lagScenario(barnFødselsdato2)
        val fagsak2: RestMinimalFagsak = lagFagsak(personScenario = personScenario2)
        val beh3 = lagFullførtBehandling(
            fagsak = fagsak2,
            opphørAvSmåbarnstilleggNå = false,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato2,
            behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        )
        val beh4 = lagRevurderingBehandling(
            fagsak = fagsak2,
            opphørAvSmåbarnstilleggNå = true,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato2,
            behandlingType = BehandlingType.REVURDERING,
            behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG
        )

        // kjør spørring og verifiser riktig output / innslag i resultat
        val behandlinger: List<Long> =
            behandlingService.hentAlleBehandlingsIderMedOpphørSmåbarnstilleggIMåned(
                måned = LocalDate.now().toYearMonth().minusMonths(1)
            )

        assertTrue(behandlinger.containsAll(listOf(beh2.id, beh4.id)))
        assertFalse(behandlinger.contains(beh1.id))
        assertFalse(behandlinger.contains(beh3.id))
    }

    fun lagScenario(barnFødselsdato: LocalDate): RestScenario = mockServerKlient().lagScenario(
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

    fun lagFagsak(personScenario: RestScenario): RestMinimalFagsak {
        return familieBaSakKlient().opprettFagsak(søkersIdent = personScenario.søker.ident!!).data!!
    }

    fun lagFullførtBehandling(
        fagsak: RestMinimalFagsak,
        opphørAvSmåbarnstilleggNå: Boolean,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
        behandlingType: BehandlingType
    ): Behandling {

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = if (opphørAvSmåbarnstilleggNå) listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            ) else emptyList()
        )

        val restBehandling: Ressurs<RestUtvidetBehandling> = familieBaSakKlient().opprettBehandling(
            søkersIdent = fagsak.søkerFødselsnummer,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            behandlingType = behandlingType
        )
        val behandling = behandlingService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad = lagSøknadDTO(
                    søkerIdent = fagsak.søkerFødselsnummer,
                    barnasIdenter = personScenario.barna.map { it.ident!! },
                    underkategori = BehandlingUnderkategori.UTVIDET
                ),
                bekreftEndringerViaFrontend = false
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad
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

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
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
        return håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = personScenario.søker.ident!!,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }

    fun lagRevurderingBehandling(
        fagsak: RestMinimalFagsak,
        opphørAvSmåbarnstilleggNå: Boolean,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
        behandlingType: BehandlingType,
        behandlingÅrsak: BehandlingÅrsak
    ): Behandling {

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = if (opphørAvSmåbarnstilleggNå) listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            ) else emptyList()
        )

        val restBehandling: Ressurs<RestUtvidetBehandling> = familieBaSakKlient().opprettBehandling(
            søkersIdent = fagsak.søkerFødselsnummer,
            behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
            behandlingType = behandlingType,
            behandlingÅrsak = behandlingÅrsak
        )
        val behandling = behandlingService.hent(restBehandling.data!!.behandlingId)

        restBehandling.data!!.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restBehandling.data!!.behandlingId,
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
            behandlingId = restBehandling.data!!.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restBehandling.data!!.behandlingId
            )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
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
        return håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = personScenario.søker.ident!!,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
