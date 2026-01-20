package no.nav.familie.ba.sak.kjerne.verdikjedetester

import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.MinimalFagsakDto
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.fake.FakeEfSakRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class AutobrevSmåbarnstilleggOpphørTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestKlient: FakeEfSakRestKlient,
    @Autowired private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {
    private val barnFødselsdato: LocalDate = LocalDate.now().minusYears(2)

    @Test
    fun `Plukk riktige behandlinger - skal være nyeste, løpende med opphør i småbarnstillegg for valgt måned`() {
        val personScenario1: RestScenario = lagScenario(barnFødselsdato)
        val fagsak1: MinimalFagsakDto = lagFagsak(personScenario = personScenario1)
        fullførBehandling(
            fagsak = fagsak1,
            personScenario = personScenario1,
        )
        val fagsak1behandling2: Behandling =
            fullførRevurderingMedOvergangstonad(
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
        val fagsak2: MinimalFagsakDto = lagFagsak(personScenario = personScenario2)
        fullførBehandling(
            fagsak = fagsak2,
            personScenario = personScenario2,
        )
        val fagsak2behandling2: Behandling =
            fullførRevurderingMedOvergangstonad(
                fagsak = fagsak2,
                personScenario = personScenario2,
                barnFødselsdato = barnFødselsdato,
            )

        val andelerForSmåbarnstilleggFagsak1Behandling2 =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = fagsak2behandling2.id)
        val førsteDagIStønadTomMåned = YearMonth.now().minusMonths(1)
        assertEquals(
            førsteDagIStønadTomMåned,
            andelerForSmåbarnstilleggFagsak1Behandling2
                .maxByOrNull {
                    it.stønadTom == YearMonth.now().minusMonths(1) && it.erSmåbarnstillegg()
                }?.stønadTom,
        )

        val fagsaker: List<Long> =
            fagsakRepository.finnAlleFagsakerMedOpphørSmåbarnstilleggIMåned(
                iverksatteLøpendeBehandlinger = listOf(fagsak1behandling2.id, fagsak2behandling2.id),
            )

        assertTrue(fagsaker.containsAll(listOf(fagsak2.id)))
        assertFalse(fagsaker.contains(fagsak1.id))
    }

    fun lagScenario(barnFødselsdato: LocalDate): RestScenario =
        RestScenario(
            søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
            barna =
                listOf(
                    RestScenarioPerson(
                        fødselsdato = barnFødselsdato.toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen",
                    ),
                ),
        ).also { stubScenario(it) }

    fun lagFagsak(personScenario: RestScenario): MinimalFagsakDto = familieBaSakKlient().opprettFagsak(søkersIdent = personScenario.søker.ident).data!!

    fun fullførBehandling(
        fagsak: MinimalFagsakDto,
        personScenario: RestScenario,
    ): Behandling {
        val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING

        efSakRestKlient.leggTilEksternPeriode(
            personIdent = personScenario.søker.ident,
            eksternePerioderResponse =
                EksternePerioderResponse(
                    perioder = emptyList(),
                ),
        )
        val restBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingType = behandlingType,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                fagsakId = fagsak.id,
            )
        val behandling = behandlingHentOgPersisterService.hent(restBehandling.data!!.behandlingId)
        val restRegistrerSøknad =
            RestRegistrerSøknad(
                søknad =
                    lagSøknadDTO(
                        søkerIdent = fagsak.søkerFødselsnummer,
                        barnasIdenter = personScenario.barna.map { it.ident },
                        underkategori = BehandlingUnderkategori.UTVIDET,
                    ),
                bekreftEndringerViaFrontend = false,
            )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().registrererSøknad(
                behandlingId = behandling.id,
                restRegistrerSøknad = restRegistrerSøknad,
            )

        return fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak,
            familieBaSakKlient = familieBaSakKlient(),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            lagToken = ::token,
            brevmalService = brevmalService,
            vedtaksperiodeService = vedtaksperiodeService,
        )
    }

    fun fullførRevurderingMedOvergangstonad(
        fagsak: MinimalFagsakDto,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        efSakRestKlient.leggTilEksternPeriode(
            personIdent = personScenario.søker.ident,
            eksternePerioderResponse =
                EksternePerioderResponse(
                    perioder =
                        listOf(
                            EksternPeriode(
                                personIdent = personScenario.søker.ident,
                                fomDato = barnFødselsdato.plusYears(1),
                                tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                                datakilde = Datakilde.EF,
                            ),
                        ),
                ),
        )

        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                fagsakId = fagsak.id,
            )

        return fullførBehandlingFraVilkårsvurderingAlleVilkårOppfylt(
            restUtvidetBehandling = restUtvidetBehandling.data!!,
            personScenario = personScenario,
            fagsak = fagsak,
            familieBaSakKlient = familieBaSakKlient(),
            fagsakService = fagsakService,
            vedtakService = vedtakService,
            stegService = stegService,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            lagToken = ::token,
            brevmalService = brevmalService,
            vedtaksperiodeService = vedtaksperiodeService,
        )
    }

    private fun startEnRevurderingNyeOpplysningerMenIkkeFullfør(
        fagsak: MinimalFagsakDto,
        personScenario: RestScenario,
        barnFødselsdato: LocalDate,
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        efSakRestKlient.leggTilEksternPeriode(
            personIdent = personScenario.søker.ident,
            eksternePerioderResponse =
                EksternePerioderResponse(
                    perioder =
                        listOf(
                            EksternPeriode(
                                personIdent = personScenario.søker.ident,
                                fomDato = barnFødselsdato.plusYears(1),
                                tomDato = LocalDate.now().minusMonths(1).førsteDagIInneværendeMåned(),
                                datakilde = Datakilde.EF,
                            ),
                        ),
                ),
        )
        val restUtvidetBehandling: Ressurs<RestUtvidetBehandling> =
            familieBaSakKlient().opprettBehandling(
                søkersIdent = fagsak.søkerFødselsnummer,
                behandlingType = behandlingType,
                behandlingÅrsak = behandlingÅrsak,
                behandlingUnderkategori = BehandlingUnderkategori.UTVIDET,
                fagsakId = fagsak.id,
            )
        return behandlingHentOgPersisterService.hent(restUtvidetBehandling.data!!.behandlingId)
    }
}
