package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.datagenerator.lagSøknadDTO
import no.nav.familie.ba.sak.ekstern.restDomene.RestMinimalFagsak
import no.nav.familie.ba.sak.ekstern.restDomene.RestRegistrerSøknad
import no.nav.familie.ba.sak.ekstern.restDomene.RestUtvidetBehandling
import no.nav.familie.ba.sak.fake.FakeEfSakRestKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsTidspunkt
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.brev.BrevmalService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.RestScenarioPerson
import no.nav.familie.ba.sak.kjerne.verdikjedetester.scenario.stubScenario
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ReduksjonFraForrigeIverksatteBehandlingTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val vedtaksperiodeService: VedtaksperiodeService,
    @Autowired private val stegService: StegService,
    @Autowired private val efSakRestKlient: FakeEfSakRestKlient,
    @Autowired private val brevmalService: BrevmalService,
) : AbstractVerdikjedetest() {
    private val barnFødselsdato: LocalDate = LocalDate.now().minusYears(2)

    @BeforeEach
    fun førHverTest() {
        mockkObject(SatsTidspunkt)
        every { SatsTidspunkt.senesteSatsTidspunkt } returns LocalDate.of(2022, 12, 31)
    }

    @AfterEach
    fun etterHverTest() {
        unmockkObject(SatsTidspunkt)
    }

    @Test
    fun `Skal lage reduksjon fra sist iverksatte behandling-periode når småbarnstillegg blir borte`() {
        val personScenario: RestScenario = lagScenario(barnFødselsdato)
        val fagsak: RestMinimalFagsak = lagFagsak(personScenario)

        val osFom = LocalDate.now().førsteDagIInneværendeMåned()
        val osTom = LocalDate.now().plusMonths(2).sisteDagIMåned()

        val behandling1 =
            fullførBehandlingMedOvergangsstønad(
                fagsak = fagsak,
                personScenario = personScenario,
                overgangsstønadPerioder =
                    listOf(
                        EksternPeriode(
                            personIdent = personScenario.søker.ident,
                            fomDato = osFom,
                            tomDato = osTom,
                            datakilde = Datakilde.EF,
                        ),
                    ),
            )
        val perioderBehandling1 =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(
                vedtak = vedtakService.hentAktivForBehandling(behandling1.id)!!,
            )

        Assertions.assertEquals(
            1,
            perioderBehandling1.filter { utvidetVedtaksperiodeMedBegrunnelser -> utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.any { it.ytelseType == YtelseType.SMÅBARNSTILLEGG } }.size,
        )

        val behandling2 =
            fullførRevurderingUtenOvergangstonad(
                fagsak = fagsak,
                personScenario = personScenario,
            )

        val perioderBehandling2 =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(
                vedtak = vedtakService.hentAktivForBehandling(behandling2.id)!!,
            )
        val periodeMedReduksjon =
            perioderBehandling2.singleOrNull { it.type == Vedtaksperiodetype.UTBETALING_MED_REDUKSJON_FRA_SIST_IVERKSATTE_BEHANDLING }

        Assertions.assertEquals(
            0,
            perioderBehandling2.filter { it.utbetalingsperiodeDetaljer.any { it.ytelseType == YtelseType.SMÅBARNSTILLEGG } }.size,
        )
        Assertions.assertNotNull(periodeMedReduksjon)
        Assertions.assertEquals(osFom, periodeMedReduksjon!!.fom)
        Assertions.assertEquals(osTom, periodeMedReduksjon.tom)
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

    fun lagFagsak(personScenario: RestScenario): RestMinimalFagsak = familieBaSakKlient().opprettFagsak(søkersIdent = personScenario.søker.ident).data!!

    fun fullførBehandlingMedOvergangsstønad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
        overgangsstønadPerioder: List<EksternPeriode>,
    ): Behandling {
        val behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING
        efSakRestKlient.leggTilEksternPeriode(
            personIdent = personScenario.søker.ident,
            eksternePerioderResponse =
                EksternePerioderResponse(
                    perioder = overgangsstønadPerioder,
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

    fun fullførRevurderingUtenOvergangstonad(
        fagsak: RestMinimalFagsak,
        personScenario: RestScenario,
    ): Behandling {
        val behandlingType = BehandlingType.REVURDERING
        val behandlingÅrsak = BehandlingÅrsak.SMÅBARNSTILLEGG

        efSakRestKlient.leggTilEksternPeriode(
            personIdent = personScenario.søker.ident,
            eksternePerioderResponse =
                EksternePerioderResponse(
                    perioder = emptyList(),
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
}
