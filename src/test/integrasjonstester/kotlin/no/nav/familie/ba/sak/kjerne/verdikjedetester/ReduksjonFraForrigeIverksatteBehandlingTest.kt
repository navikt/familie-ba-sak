package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagSøknadDTO
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestPersonResultat
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.integrasjoner.`ef-sak`.EfSakRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
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
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
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

class ReduksjonFraForrigeIverksatteBehandlingTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestClient: EfSakRestClient
) : AbstractVerdikjedetest() {

    private val barnFødselsdato: LocalDate = LocalDate.now().minusYears(2)

    @Test
    fun `Skal lage reduksjon fra sist iverksatte behandling-periode når småbarnstillegg blir borte`() {
        val personScenario: RestScenario = lagScenario(barnFødselsdato)
        val fagsak: RestMinimalFagsak = lagFagsak(personScenario)

        val osFom = LocalDate.now().førsteDagIInneværendeMåned()
        val osTom = LocalDate.now().plusMonths(2).sisteDagIMåned()

        val behandling1 = fullførBehandlingMedOvergangsstønad(
            fagsak = fagsak,
            personScenario = personScenario,
            barnFødselsdato = barnFødselsdato,
            overgangsstønadPerioder = listOf(
                PeriodeOvergangsstønad(
                    personIdent = personScenario.søker.ident!!,
                    fomDato = osFom,
                    tomDato = osTom,
                    datakilde = PeriodeOvergangsstønad.Datakilde.EF
                )
            )
        )
        val perioderBehandling1 = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak = vedtakService.hentAktivForBehandling(behandling1.id)!!)

        Assertions.assertEquals(1, perioderBehandling1.filter { it.utbetalingsperiodeDetaljer.any { it.ytelseType == YtelseType.SMÅBARNSTILLEGG } }.size)

        val behandling2 = fullførRevurderingUtenOvergangstonad(
            fagsak = fagsak,
            personScenario = personScenario,
            barnFødselsdato = barnFødselsdato
        )

        val perioderBehandling2 = vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak = vedtakService.hentAktivForBehandling(behandling2.id)!!)
        val periodeMedReduksjon = perioderBehandling2.singleOrNull { it.type == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING }

        Assertions.assertEquals(0, perioderBehandling2.filter { it.utbetalingsperiodeDetaljer.any { it.ytelseType == YtelseType.SMÅBARNSTILLEGG } }.size)
        Assertions.assertNotNull(periodeMedReduksjon)
        Assertions.assertEquals(osFom, periodeMedReduksjon!!.fom)
        Assertions.assertEquals(osTom, periodeMedReduksjon.tom)
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

    fun fullførBehandlingMedOvergangsstønad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
        overgangsstønadPerioder: List<PeriodeOvergangsstønad>
    ): Behandling {
        val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = overgangsstønadPerioder
        )

        val restBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                behandlingType = behandlingType
            )
        val behandling = behandlingHentOgPersisterService.hent(restBehandling.data!!.behandlingId)
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

    fun fullførRevurderingUtenOvergangstonad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        every { efSakRestClient.hentPerioderMedFullOvergangsstønad(any()) } returns PerioderOvergangsstønadResponse(
            perioder = emptyList()
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

        val utvidetVedtaksperiodeMedBegrunnelser =
            restUtvidetBehandlingEtterVurderTilbakekreving.data!!.vedtak!!.vedtaksperioderMedBegrunnelser.sortedBy { it.fom }
                .first()

        familieBaSakKlient().oppdaterVedtaksperiodeMedStandardbegrunnelser(
            vedtaksperiodeId = utvidetVedtaksperiodeMedBegrunnelser.id,
            restPutVedtaksperiodeMedStandardbegrunnelser = RestPutVedtaksperiodeMedStandardbegrunnelser(
                standardbegrunnelser = utvidetVedtaksperiodeMedBegrunnelser.gyldigeBegrunnelser.map { it.toString() }
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
            behandlingEtterVurdering = behandlingHentOgPersisterService.hentAktivForFagsak(fagsakId = fagsak.id)!!,
            søkerFnr = personScenario.søker.ident!!,
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService
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
}
