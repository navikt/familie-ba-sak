package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ba.sak.common.lagVedtak
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.util.apr
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mar
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import no.nav.familie.felles.utbetalingsgenerator.domain.BeregnetUtbetalingsoppdragLongId
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class KontrollerNyUtbetalingsgeneratorServiceTest {

    @MockK
    private lateinit var featureToggleService: FeatureToggleService

    @MockK
    private lateinit var økonomiService: ØkonomiService

    @MockK
    private lateinit var økonomiKlient: ØkonomiKlient

    @InjectMockKs
    private lateinit var kontrollerNyUtbetalingsgeneratorService: KontrollerNyUtbetalingsgeneratorService

    @BeforeEach
    fun beforeAll() {
        every { featureToggleService.isEnabled(any()) } returns true

        val beregnetUtbetalingsoppdragMock = mockk<BeregnetUtbetalingsoppdragLongId>()

        every { beregnetUtbetalingsoppdragMock.utbetalingsoppdrag } returns mockk()

        every { beregnetUtbetalingsoppdragMock.andeler } returns mockk()

        every { økonomiService.genererUtbetalingsoppdrag(any(), any(), any()) } returns beregnetUtbetalingsoppdragMock
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal fange opp at gammel simulering har perioder med endring før ny simulering og ulikt resultat i samme perioder`() {
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 100),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(apr(2023), mai(2023), 250),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            simuleringResultatGammel = simuleringBasertPåGammelGenerator,
            utbetalingsoppdragGammel = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(2)
        assertThat(
            simuleringsPeriodeDiffFeil.containsAll(
                listOf(
                    DiffFeilType.TidligerePerioderIGammelUlik0,
                    DiffFeilType.UliktResultatISammePeriode,
                ),
            ),
        ).isTrue
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom gammel simulering har perioder uten endring før ny simulering og resultatene er like i øvrige perioder`() {
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 0),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            simuleringResultatGammel = simuleringBasertPåGammelGenerator,
            utbetalingsoppdragGammel = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom gammel simulering og ny simulering er helt like`() {
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 100),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), mar(2023), 100),
                Periode(apr(2023), mai(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            simuleringResultatGammel = simuleringBasertPåGammelGenerator,
            utbetalingsoppdragGammel = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal gi feil dersom gammel simulering og ny simulering har et ulikt resultat`() {
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mar(2023), apr(2023), 200),
                Periode(mai(2023), jun(2023), 300),
                Periode(jul(2023), aug(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mar(2023), apr(2023), 200),
                Periode(mai(2023), jun(2023), 320),
                Periode(jul(2023), aug(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            simuleringResultatGammel = simuleringBasertPåGammelGenerator,
            utbetalingsoppdragGammel = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(1)
        assertThat(
            simuleringsPeriodeDiffFeil.first(),
        ).isEqualTo(DiffFeilType.UliktResultatISammePeriode)
    }

    @Test
    fun `kontrollerNyUtbetalingsgenerator - skal ikke gi feil dersom gammel simulering og ny simulering er like og har hull i periodene`() {
        val simuleringBasertPåGammelGenerator = lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mai(2023), jun(2023), 300),
                Periode(aug(2023), sep(2023), 200),
            ),
        )

        every { økonomiKlient.hentSimulering(any()) } returns lagDetaljertSimuleringsResultat(
            listOf(
                Periode(jan(2023), feb(2023), 100),
                Periode(mai(2023), jun(2023), 300),
                Periode(aug(2023), sep(2023), 200),
            ),
        )

        val simuleringsPeriodeDiffFeil = kontrollerNyUtbetalingsgeneratorService.kontrollerNyUtbetalingsgenerator(
            vedtak = lagVedtak(),
            simuleringResultatGammel = simuleringBasertPåGammelGenerator,
            utbetalingsoppdragGammel = mockk(),
        )

        assertThat(simuleringsPeriodeDiffFeil.size).isEqualTo(0)
    }

    fun lagDetaljertSimuleringsResultat(perioder: List<Periode<Int, Måned>>) = DetaljertSimuleringResultat(
        simuleringMottaker = listOf(
            SimuleringMottaker(
                simulertPostering = perioder.map {
                    SimulertPostering(
                        fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                        fom = it.fraOgMed.tilLocalDate(),
                        tom = it.tilOgMed.tilLocalDate(),
                        betalingType = BetalingType.DEBIT,
                        beløp = BigDecimal(it.innhold!!),
                        posteringType = PosteringType.YTELSE,
                        forfallsdato = des(2023).tilLocalDate(),
                        utenInntrekk = true,
                    )
                },
                mottakerType = MottakerType.BRUKER,
            ),
        ),
    )
}
