package no.nav.familie.ba.sak.kjerne.verdikjedetester

import io.mockk.every
import no.nav.familie.ba.sak.common.LocalDateService
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.dokument.totaltUtbetalt
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenario
import no.nav.familie.ba.sak.kjerne.verdikjedetester.mockserver.domene.RestScenarioPerson
import no.nav.familie.ba.sak.task.BehandleFødselshendelseTask
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class FødselshendelseFørstegangsbehandlingTest(
    @Autowired private val behandleFødselshendelseTask: BehandleFødselshendelseTask,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val vedtakService: VedtakService,
    @Autowired private val stegService: StegService,
    @Autowired private val mockLocalDateService: LocalDateService
) : AbstractVerdikjedetest() {

    @Test
    fun `Skal innvilge fødselshendelse på mor med 1 barn født november 2021 og behandles desember 2021 uten utbetalinger`() {
        // Behandler desember 2021 for å få med automatisk begrunnelse av satsendring januar 2022
        every { mockLocalDateService.now() } returns LocalDate.of(2021, 12, 12) andThen LocalDate.now()

        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.of(2021, 11, 18).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    )
                )
            )
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = listOf(scenario.barna.first().ident!!)
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            vedtakService = vedtakService,
            stegService = stegService,
            personidentService = personidentService,
        )

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id)
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET
        )

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.single()
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)

        val vedtaksperioder = aktivBehandling.vedtak?.vedtaksperioderMedBegrunnelser

        val desember2021Vedtaksperiode = vedtaksperioder?.find { it.fom == LocalDate.of(2021, 12, 1) }
        val januar2022Vedtaksperiode = vedtaksperioder?.find { it.fom == LocalDate.of(2022, 1, 1) }

        assertEquals(
            0,
            vedtaksperioder
                ?.filter { it != desember2021Vedtaksperiode && it != januar2022Vedtaksperiode }
                ?.flatMap { it.begrunnelser }
                ?.size
        )

        assertEquals(
            1654,
            desember2021Vedtaksperiode?.utbetalingsperiodeDetaljer?.totaltUtbetalt(),
        )
        assertEquals(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE,
            desember2021Vedtaksperiode?.begrunnelser?.first()?.vedtakBegrunnelseSpesifikasjon,
        )

        assertEquals(
            1676,
            januar2022Vedtaksperiode?.utbetalingsperiodeDetaljer?.totaltUtbetalt(),
        )
        assertEquals(
            VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING,
            januar2022Vedtaksperiode?.begrunnelser?.first()?.vedtakBegrunnelseSpesifikasjon,
        )
    }

    @Test
    fun `Skal innvilge fødselshendelse på mor med 2 barn uten utbetalinger`() {
        val scenario = mockServerKlient().lagScenario(
            RestScenario(
                søker = RestScenarioPerson(fødselsdato = "1996-01-12", fornavn = "Mor", etternavn = "Søker"),
                barna = listOf(
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen"
                    ),
                    RestScenarioPerson(
                        fødselsdato = LocalDate.now().minusDays(2).toString(),
                        fornavn = "Barn",
                        etternavn = "Barnesen 2"
                    )
                )
            )
        )
        val behandling = behandleFødselshendelse(
            nyBehandlingHendelse = NyBehandlingHendelse(
                morsIdent = scenario.søker.ident!!,
                barnasIdenter = scenario.barna.map { it.ident!! }
            ),
            behandleFødselshendelseTask = behandleFødselshendelseTask,
            fagsakService = fagsakService,
            behandlingService = behandlingService,
            personidentService = personidentService,
            vedtakService = vedtakService,
            stegService = stegService
        )

        val restFagsakEtterBehandlingAvsluttet =
            familieBaSakKlient().hentFagsak(fagsakId = behandling!!.fagsak.id)
        generellAssertFagsak(
            restFagsak = restFagsakEtterBehandlingAvsluttet,
            fagsakStatus = FagsakStatus.LØPENDE,
            behandlingStegType = StegType.BEHANDLING_AVSLUTTET
        )

        val aktivBehandling = restFagsakEtterBehandlingAvsluttet.getDataOrThrow().behandlinger.single()
        assertEquals(BehandlingResultat.INNVILGET, aktivBehandling.resultat)

        val utbetalingsperioder = aktivBehandling.utbetalingsperioder
        val gjeldendeUtbetalingsperiode = utbetalingsperioder.find {
            it.periodeFom.toYearMonth() >= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigFom.toYearMonth() &&
                it.periodeFom.toYearMonth() <= SatsService.tilleggOrdinærSatsNesteMånedTilTester.gyldigTom.toYearMonth()
        }!!

        assertUtbetalingsperiode(
            gjeldendeUtbetalingsperiode,
            2,
            SatsService.tilleggOrdinærSatsNesteMånedTilTester.beløp * 2
        )
    }
}
