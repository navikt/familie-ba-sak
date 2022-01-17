package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.RestartAvSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import java.time.LocalDate

class RestartAvSmåbarnstilleggTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestClient: EfSakRestClient,
    @Autowired private val restartAvSmåbarnstilleggService: RestartAvSmåbarnstilleggService
) : AbstractVerdikjedetest() {

    private val barnFødselsdato: LocalDate = LocalDate.now().minusYears(2)

    @Test
    fun `Skal finne alle fagsaker hvor småbarnstillegg starter opp igjen inneværende måned, og ikke er begrunnet`() {
        val restartSmåbarnstilleggMåned = LocalDate.now().plusMonths(4)

        // Fagsak 1 - har åpen behandling og skal ikke tas med
        val personScenario1: RestScenario = lagScenario(barnFødselsdato)
        val fagsak1: RestMinimalFagsak = lagFagsak(personScenario = personScenario1)
        fullførBehandling(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )

        fullførRevurderingMedOvergangstonad(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
            mockPerioderMedOvergangsstønad = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario1.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                ),
                PeriodeOvergangsstønad(
                    personIdent = personScenario1.søker.ident,
                    fomDato = restartSmåbarnstilleggMåned.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )
        startEnRevurderingNyeOpplysningerMenIkkeFullfør(
            fagsak = fagsak1,
            personScenario = personScenario1,
            barnFødselsdato = barnFødselsdato,
        )

        // Fagsak 2 - har restart av småbarnstillegg som ikke er begrunnet og skal være med i listen
        val personScenario2: RestScenario = lagScenario(barnFødselsdato)
        val fagsak2: RestMinimalFagsak = lagFagsak(personScenario = personScenario2)
        fullførBehandling(
            fagsak = fagsak2,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
        )
        fullførRevurderingMedOvergangstonad(
            fagsak = fagsak2,
            personScenario = personScenario2,
            barnFødselsdato = barnFødselsdato,
            mockPerioderMedOvergangsstønad = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario2.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                ),
                PeriodeOvergangsstønad(
                    personIdent = personScenario2.søker.ident,
                    fomDato = restartSmåbarnstilleggMåned.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        // Fagsak 3 - har restart av småbarnstillegg som allerede er begrunnet, skal ikke være med i listen
        val personScenario3: RestScenario = lagScenario(barnFødselsdato)
        val fagsak3: RestMinimalFagsak = lagFagsak(personScenario = personScenario3)
        fullførBehandling(
            fagsak = fagsak3,
            personScenario = personScenario3,
            barnFødselsdato = barnFødselsdato,
        )
        fullførRevurderingMedOvergangstonad(
            fagsak = fagsak3,
            personScenario = personScenario3,
            barnFødselsdato = barnFødselsdato,
            skalBegrunneSmåbarnstillegg = true,
            mockPerioderMedOvergangsstønad = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario3.søker.ident!!,
                    fomDato = barnFødselsdato.plusYears(1),
                    tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                ),
                PeriodeOvergangsstønad(
                    personIdent = personScenario3.søker.ident,
                    fomDato = restartSmåbarnstilleggMåned.førsteDagIInneværendeMåned(),
                    tomDato = LocalDate.now().plusYears(3).førsteDagIInneværendeMåned(),
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )

        val fagsaker: List<Long> =
            restartAvSmåbarnstilleggService.finnAlleFagsakerMedRestartetSmåbarnstilleggIMåned(måned = restartSmåbarnstilleggMåned.toYearMonth())

        Assertions.assertTrue(fagsaker.containsAll(listOf(fagsak2.id)))
        Assertions.assertFalse(fagsaker.contains(fagsak1.id))
        Assertions.assertFalse(fagsaker.contains(fagsak3.id))
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
        mockPerioderMedOvergangsstønad: List<PeriodeOvergangsstønad> = listOf(
            PeriodeOvergangsstønad(
                personIdent = personScenario.søker.ident!!,
                fomDato = barnFødselsdato.plusYears(1),
                tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                datakilde = PeriodeOvergangsstønad.Datakilde.EF
            )
        ),
        skalBegrunneSmåbarnstillegg: Boolean = false
    ): Behandling {

        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = mockPerioderMedOvergangsstønad
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
            fagsak = fagsak,
            skalBegrunneSmåbarnstillegg = skalBegrunneSmåbarnstillegg
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
        fagsak: RestMinimalFagsak,
        skalBegrunneSmåbarnstillegg: Boolean = false
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
        if (skalBegrunneSmåbarnstillegg) {
            val småbarnstilleggVedtaksperioder =
                restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.filter {
                    it.utbetalingsperiodeDetaljer.filter { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.ytelseType == YtelseType.SMÅBARNSTILLEGG }
                        .isNotEmpty()
                }

            småbarnstilleggVedtaksperioder.forEach { periode ->
                familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
                    vedtaksperiodeId = periode.id,
                    restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                        standardbegrunnelser = listOf(
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_SMÅBARNSTILLEGG
                        )
                    )
                )
            }
        }

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