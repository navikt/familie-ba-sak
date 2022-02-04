package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.RestBeslutningPåVedtak
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import no.nav.familie.kontrakter.felles.ef.PerioderOvergangsstønadResponse
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.annotation.DirtiesContext
import java.time.LocalDate
import java.time.YearMonth

@DirtiesContext
class AutobrevSmåbarnstilleggOpphørTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) : AbstractVerdikjedetest() {

    private val barnFødselsdato: LocalDate = LocalDate.now().minusYears(2)

    @Test
    fun `Plukk riktige behandlinger - skal være nyeste, løpende med opphør i småbarnstillegg for valgt måned`() {

        val personScenario1: RestScenario = lagScenario(barnFødselsdato)
        val fagsak1: RestMinimalFagsak = lagFagsak(personScenario = personScenario1)
        fullførBehandling(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )
        val fagsak1behandling2: Behandling = fullførRevurderingMedOvergangstonad(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )
        startEnRevurderingNyeOpplysningerMenIkkeFullfør(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )

        val personScenario2: RestScenario = lagScenario(barnFødselsdato)
        val fagsak2: RestMinimalFagsak = lagFagsak(personScenario = personScenario2)
        fullførBehandling(
            fagsak = fagsak2,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
        )
        val fagsak2behandling2: Behandling = fullførRevurderingMedOvergangstonad(
            fagsak = fagsak2,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
        )

        val andelerForSmåbarnstilleggFagsak1Behandling2 =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = fagsak2behandling2.id)
        val førsteDagIStønadTomMåned = YearMonth.now().minusMonths(1)
        assertEquals(
            førsteDagIStønadTomMåned,
            andelerForSmåbarnstilleggFagsak1Behandling2.maxByOrNull {
                it.stønadTom == YearMonth.now().minusMonths(1) && it.erSmåbarnstillegg()
            }?.stønadTom
        )

        val fagsaker: List<Long> =
            fagsakRepository.finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
                iverksatteLøpendeBehandlinger = listOf(fagsak1behandling2.id, fagsak2behandling2.id),
            )

        assertTrue(fagsaker.containsAll(listOf(fagsak2.id)))
        assertFalse(fagsaker.contains(fagsak1.id))
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

    fun fullførBehandling(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
    ): Behandling {

        val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = emptyList()
        )

        val restBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
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

        return fullførRestenAvBehandlingen(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak
        )
    }

    fun fullførRevurderingMedOvergangstonad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
    ): Behandling {

        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak
            )

        return fullførRestenAvBehandlingen(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak
        )
    }

    fun settAlleVilkårTilOppfylt(restUtvidetBehandling: RestUtvidetBehandling, barnFødselsdato: LocalDate) {
        restUtvidetBehandling.personResultater.forEach { restPersonResultat ->
            restPersonResultat.vilkårResultater.filter { it.resultat == Resultat.IKKE_VURDERT }.forEach {
                familieBaSakKlient().putVilkår(
                    behandlingId = restUtvidetBehandling.behandlingId,
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
    }

    private fun startEnRevurderingNyeOpplysningerMenIkkeFullfør(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak
            )
        return behandlingService.hent(restUtvidetBehandling.data!!.behandlingId)
    }

    fun fullførRestenAvBehandlingen(
        restUtvidetBehandling: RestUtvidetBehandling,
        personScenario: RestScenario,
        fagsak: RestMinimalFagsak
    ): Behandling {
        settAlleVilkårTilOppfylt(
            restUtvidetBehandling = restUtvidetBehandling,
            barnFødselsdato = barnFødselsdato
        )

        familieBaSakKlient().validerVilkårsvurdering(
            behandlingId = restUtvidetBehandling.behandlingId
        )

        val restUtvidetBehandlingEtterBehandlingsResultat =
            familieBaSakKlient().behandlingsresultatStegOgGåVidereTilNesteSteg(
                behandlingId = restUtvidetBehandling.behandlingId
            )

        val restUtvidetBehandlingEtterVurderTilbakekreving =
            familieBaSakKlient().lagreTilbakekrevingOgGåVidereTilNesteSteg(
                restUtvidetBehandlingEtterBehandlingsResultat.data!!.behandlingId,
                RestTilbakekreving(Tilbakekrevingsvalg.IGNORER_TILBAKEKREVING, begrunnelse = "begrunnelse")
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
        return håndterIverksettingAvBehandling(
            behandlingEtterVurdering = behandlingService.hentAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = personScenario.søker.ident!!,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
        )
    }
}
