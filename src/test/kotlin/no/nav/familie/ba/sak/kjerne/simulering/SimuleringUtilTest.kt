package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.mockk
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.MottakerType
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SimuleringUtilTest {

    fun mockØkonomiSimuleringMottaker(
            id: Long = 0,
            mottakerNummer: String? = randomFnr(),
            mottakerType: MottakerType = MottakerType.BRUKER,
            behandling: Behandling = mockk(relaxed = true),
            økonomiSimuleringPostering: List<ØkonomiSimuleringPostering> = listOf(mockVedtakSimuleringPostering()),
    ) = ØkonomiSimuleringMottaker(id, mottakerNummer, mottakerType, behandling, økonomiSimuleringPostering)

    fun mockVedtakSimuleringPostering(
            økonomiSimuleringMottaker: ØkonomiSimuleringMottaker = mockk(relaxed = true),
            beløp: Int = 0,
            fagOmrådeKode: FagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom: LocalDate = LocalDate.now().minusMonths(1),
            tom: LocalDate = LocalDate.now().minusMonths(1),
            betalingType: BetalingType = BetalingType.DEBIT,
            posteringType: PosteringType = PosteringType.YTELSE,
            forfallsdato: LocalDate = LocalDate.now().minusMonths(1),
            utenInntrekk: Boolean = false,
    ) = ØkonomiSimuleringPostering(
            økonomiSimuleringMottaker = økonomiSimuleringMottaker,
            fagOmrådeKode = fagOmrådeKode,
            fom = fom,
            tom = tom,
            betalingType = betalingType,
            beløp = beløp.toBigDecimal(),
            posteringType = posteringType,
            forfallsdato = forfallsdato,
            utenInntrekk = utenInntrekk,
    )

    @Test
    fun `Test henting av 'nytt beløp ', 'tidligere utbetalt ' og 'resultat ' for simuleringsperiode uten feilutbetaling`() {
        val vedtaksimuleringPosteringer = listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
        )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(2), hentResultatIPeriode(vedtaksimuleringPosteringer))
    }

    @Test
    fun `Test henting av 'nytt beløp', 'tidligere utbetalt' og 'resultat' for simuleringsperiode med feilutbetaling`() {
        val økonomiSimuleringPosteringer = listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
        )

        Assertions.assertEquals(BigDecimal.valueOf(4), hentNyttBeløpIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(-196), hentResultatIPeriode(økonomiSimuleringPosteringer))
    }

    @Test
    fun `Test 'nytt beløp', 'tidligere utbetalt' og 'resultat' for simuleringsperiode med reduksjon i feilutbetaling`() {
        val økonomiSimuleringPosteringer = listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.FEILUTBETALING),
        )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(197), hentTidligereUtbetaltIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(1), hentResultatIPeriode(økonomiSimuleringPosteringer))
    }

    val økonomiSimuleringPosteringerMedNegativFeilutbetaling = listOf(
            mockVedtakSimuleringPostering(beløp = -500, posteringType = PosteringType.FEILUTBETALING),
            mockVedtakSimuleringPostering(beløp = -2000, posteringType = PosteringType.YTELSE),
            mockVedtakSimuleringPostering(beløp = 3000, posteringType = PosteringType.YTELSE),
            mockVedtakSimuleringPostering(beløp = -500, posteringType = PosteringType.YTELSE),
    )

    @Test
    fun `Total etterbetaling skal bli summen av ytelsene i periode med negativ feilutbetaling`() {
        val økonomiSimuleringMottaker =
                mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedNegativFeilutbetaling)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(listOf(økonomiSimuleringMottaker))

        Assertions.assertEquals(BigDecimal.valueOf(500), restSimulering.etterbetaling)
    }

    @Test
    fun `Total feilutbetaling skal bli 0 i periode med negativ feilutbetaling`() {
        val økonomiSimuleringMottaker =
                mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedNegativFeilutbetaling)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(listOf(økonomiSimuleringMottaker))

        Assertions.assertEquals(BigDecimal.valueOf(0), restSimulering.feilutbetaling)
    }


    @Test
    fun `Skal gi 0 etterbetaling og sum feilutbetaling ved positiv feilutbetaling`() {
        val økonomiSimuleringPosteringerMedPositivFeilutbetaling = listOf(
                mockVedtakSimuleringPostering(beløp = 500, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = -2000, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 3000, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -500, posteringType = PosteringType.YTELSE),
        )

        val økonomiSimuleringMottaker =
                mockØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedPositivFeilutbetaling)
        val restSimulering = vedtakSimuleringMottakereTilRestSimulering(listOf(økonomiSimuleringMottaker))

        Assertions.assertEquals(BigDecimal.valueOf(0), restSimulering.etterbetaling)
        Assertions.assertEquals(BigDecimal.valueOf(500), restSimulering.feilutbetaling)
    }
}