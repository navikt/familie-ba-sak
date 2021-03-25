package no.nav.familie.ba.sak.simulering

import io.mockk.mockk
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringMottaker
import no.nav.familie.ba.sak.simulering.domene.VedtakSimuleringPostering
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class SimuleringUtilTest {

    fun mockVedtakSimuleringPostering(
            vedtakSimuleringMottaker: VedtakSimuleringMottaker = mockk<VedtakSimuleringMottaker>(relaxed = true),
            beløp: Int = 0,
            fagOmrådeKode: FagOmrådeKode = FagOmrådeKode.BARNETRYGD,
            fom: LocalDate = LocalDate.now(),
            tom: LocalDate = LocalDate.now(),
            betalingType: BetalingType = BetalingType.DEBIT,
            posteringType: PosteringType = PosteringType.YTELSE,
            forfallsdato: LocalDate = LocalDate.now(),
            utenInntrekk: Boolean = false,
    ) = VedtakSimuleringPostering(
            vedtakSimuleringMottaker = vedtakSimuleringMottaker,
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
        val vedtaksimuleringPosteringer = listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
        )

        Assertions.assertEquals(BigDecimal.valueOf(4), hentNyttBeløpIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(-196), hentResultatIPeriode(vedtaksimuleringPosteringer))
    }

    @Test
    fun `Test 'nytt beløp', 'tidligere utbetalt' og 'resultat' for simuleringsperiode med reduksjon i feilutbetaling`() {
        val vedtaksimuleringPosteringer = listOf(
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                mockVedtakSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                mockVedtakSimuleringPostering(beløp = -99, posteringType = PosteringType.FEILUTBETALING),
        )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(199), hentTidligereUtbetaltIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(1), hentResultatIPeriode(vedtaksimuleringPosteringer))
    }
}