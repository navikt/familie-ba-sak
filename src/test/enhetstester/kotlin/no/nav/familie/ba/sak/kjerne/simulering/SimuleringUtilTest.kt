package no.nav.familie.ba.sak.kjerne.simulering

import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.simulering.domene.ØkonomiSimuleringPostering
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.simulering.BetalingType
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class SimuleringUtilTest {
    fun mockVedtakSimuleringPosteringer(
        måned: YearMonth = YearMonth.of(2021, 1),
        antallMåneder: Int = 1,
        beløp: Int = 5000,
        posteringstype: PosteringType = PosteringType.YTELSE,
        betalingstype: BetalingType = if (beløp >= 0) BetalingType.DEBIT else BetalingType.KREDIT,
        fagsakId: Long = 0,
    ): List<ØkonomiSimuleringPostering> =
        MutableList(antallMåneder) { index ->
            ØkonomiSimuleringPostering(
                økonomiSimuleringMottaker = mockk(relaxed = true),
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = måned.plusMonths(index.toLong()).atDay(1),
                tom = måned.plusMonths(index.toLong()).atEndOfMonth(),
                betalingType = betalingstype,
                beløp = beløp.toBigDecimal(),
                posteringType = posteringstype,
                forfallsdato = måned.plusMonths(index.toLong()).atEndOfMonth(),
                utenInntrekk = false,
                fagsakId = fagsakId,
            )
        }

    @Test
    fun `Test henting av fagsakId`() {
        // Arrange
        val fagsakId = 123456L

        val simuleringPostering =
            SimulertPostering(
                fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                fom = LocalDate.now().minusYears(1),
                tom = LocalDate.now(),
                betalingType = BetalingType.DEBIT,
                beløp = BigDecimal(500),
                posteringType = PosteringType.YTELSE,
                forfallsdato = LocalDate.now().plusDays(1),
                fagsakId = "$fagsakId     ",
            )

        val økonomiSimuleringMottaker = lagØkonomiSimuleringMottaker()

        // Act
        val postering = simuleringPostering.tilVedtakSimuleringPostering(økonomiSimuleringMottaker)

        // Assert
        assertThat(postering.fagsakId).isEqualTo(fagsakId)
    }

    @Test
    fun `Test henting av 'nytt beløp ', 'tidligere utbetalt ' og 'resultat ' for simuleringsperiode uten feilutbetaling`() {
        val vedtaksimuleringPosteringer =
            listOf(
                lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
            )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(vedtaksimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(2), hentResultatIPeriode(vedtaksimuleringPosteringer))
    }

    @Test
    fun `Test henting av 'nytt beløp', 'tidligere utbetalt' og 'resultat' for simuleringsperiode med feilutbetaling`() {
        val økonomiSimuleringPosteringer =
            listOf(
                lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                lagØkonomiSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
            )

        Assertions.assertEquals(BigDecimal.valueOf(4), hentNyttBeløpIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(198), hentTidligereUtbetaltIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(-196), hentResultatIPeriode(økonomiSimuleringPosteringer))
    }

    @Test
    fun `Test 'nytt beløp', 'tidligere utbetalt' og 'resultat' for simuleringsperiode med reduksjon i feilutbetaling`() {
        val økonomiSimuleringPosteringer =
            listOf(
                lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = 100, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = 98, posteringType = PosteringType.FEILUTBETALING),
                lagØkonomiSimuleringPostering(beløp = -99, posteringType = PosteringType.FEILUTBETALING),
            )

        Assertions.assertEquals(BigDecimal.valueOf(200), hentNyttBeløpIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(197), hentTidligereUtbetaltIPeriode(økonomiSimuleringPosteringer))
        Assertions.assertEquals(BigDecimal.valueOf(3), hentResultatIPeriode(økonomiSimuleringPosteringer))
    }

    private val økonomiSimuleringPosteringerMedNegativFeilutbetaling =
        listOf(
            lagØkonomiSimuleringPostering(beløp = -500, posteringType = PosteringType.FEILUTBETALING),
            lagØkonomiSimuleringPostering(beløp = -2000, posteringType = PosteringType.YTELSE),
            lagØkonomiSimuleringPostering(beløp = 3000, posteringType = PosteringType.YTELSE),
            lagØkonomiSimuleringPostering(beløp = -500, posteringType = PosteringType.YTELSE),
        )

    @Test
    fun `Total etterbetaling skal bli summen av ytelsene i periode med negativ feilutbetaling`() {
        val økonomiSimuleringMottaker =
            lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedNegativFeilutbetaling)
        val restSimulering = vedtakSimuleringMottakereTilSimuleringDto(listOf(økonomiSimuleringMottaker))

        Assertions.assertEquals(BigDecimal.valueOf(500), restSimulering.etterbetaling)
    }

    @Test
    fun `Total feilutbetaling skal bli 0 i periode med negativ feilutbetaling`() {
        val økonomiSimuleringMottaker =
            lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedNegativFeilutbetaling)
        val restSimulering = vedtakSimuleringMottakereTilSimuleringDto(listOf(økonomiSimuleringMottaker))

        Assertions.assertEquals(BigDecimal.valueOf(0), restSimulering.feilutbetaling)
    }

    @Test
    fun `Skal gi 0 etterbetaling og sum feilutbetaling ved positiv feilutbetaling`() {
        val økonomiSimuleringPosteringerMedPositivFeilutbetaling =
            listOf(
                lagØkonomiSimuleringPostering(beløp = 500, posteringType = PosteringType.FEILUTBETALING),
                lagØkonomiSimuleringPostering(beløp = -2000, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = 3000, posteringType = PosteringType.YTELSE),
                lagØkonomiSimuleringPostering(beløp = -500, posteringType = PosteringType.YTELSE),
            )

        val økonomiSimuleringMottaker =
            lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = økonomiSimuleringPosteringerMedPositivFeilutbetaling)
        val restSimulering = vedtakSimuleringMottakereTilSimuleringDto(listOf(økonomiSimuleringMottaker))

        Assertions.assertEquals(BigDecimal.valueOf(0), restSimulering.etterbetaling)
        Assertions.assertEquals(BigDecimal.valueOf(500), restSimulering.feilutbetaling)
    }

    @Test
    fun `Test at bare perioder med passert forfalldato blir inludert i summeringen av etterbetaling`() {
        val vedtaksimuleringPosteringer =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = 100,
                    posteringType = PosteringType.YTELSE,
                    forfallsdato = LocalDate.now().plusDays(1),
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 200,
                    posteringType = PosteringType.YTELSE,
                    forfallsdato = LocalDate.now().minusDays(1),
                ),
            )

        Assertions.assertEquals(
            BigDecimal.valueOf(200),
            hentEtterbetalingIPeriode(
                vedtaksimuleringPosteringer,
                LocalDate.now(),
            ),
        )
    }

    /*
    De neste testene antar at brukeren går gjennom følgende for ÉN periode:
    - Førstegangsbehandling gir ytelse på kr 10 000
    - Revurdering reduserer ytelse fra kr 10 000 til kr 2 000, dvs kr 8 000 feilutbetalt
    - Revurdering øker ytelse fra kr 2 000 til kr 3 000, dvs feilutbetaling reduseres
    - Revurdering øker ytelse fra kr 3 000 tik kr 12 000, dvs feilutbetaling nulles ut, og etterbetaling skjer
     */
    @Test
    fun `ytelse på 10000 korrigert til 2000`() {
        val redusertYtelseTil2000 =
            listOf(
                // Forrige
                lagØkonomiSimuleringPostering(
                    beløp = -10_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
                // Ny
                lagØkonomiSimuleringPostering(
                    beløp = 2_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                // Feilutbetaling
                lagØkonomiSimuleringPostering(
                    beløp = 8_000,
                    posteringType = PosteringType.FEILUTBETALING,
                    betalingType = BetalingType.DEBIT,
                ),
                // "Nuller ut" Feilutbetalingen
                lagØkonomiSimuleringPostering(
                    beløp = -8_000,
                    posteringType = PosteringType.MOTP,
                    betalingType = BetalingType.KREDIT,
                ),
                // "Nuller ut" forrige og ny
                lagØkonomiSimuleringPostering(
                    beløp = 8_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = redusertYtelseTil2000))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(10_000.toBigDecimal())
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(2_000.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(-8_000.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(8_000.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(0.toBigDecimal())
    }

    @Test
    fun `ytelse med manuelle posteringer på trekk av 770 over 3 mnd`() {
        val fil = File("./src/test/resources/kjerne/simulering/simulering_med_manuell_postering.json")

        val ytelseMedManuellePosteringer =
            jsonMapper
                .readValue<DetaljertSimuleringResultat>(fil)

        val vedtakSimuleringMottakere =
            ytelseMedManuellePosteringer.simuleringMottaker.map {
                it.tilBehandlingSimuleringMottaker(
                    lagBehandling(),
                )
            }

        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(vedtakSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(vedtakSimuleringMottakere)

        val simuleringJanuar22 = simuleringsperioder.single { it.fom == LocalDate.of(2022, 1, 1) }
        val simuleringFebruar22 = simuleringsperioder.single { it.fom == LocalDate.of(2022, 2, 1) }
        val simuleringMars22 = simuleringsperioder.single { it.fom == LocalDate.of(2022, 3, 1) }
        val simuleringApril22 = simuleringsperioder.single { it.fom == LocalDate.of(2022, 4, 1) }

        assertThat(simuleringJanuar22.tidligereUtbetalt).isEqualTo(305.toBigDecimal())
        assertThat(simuleringJanuar22.resultat).isEqualTo(0.toBigDecimal())
        assertThat(simuleringJanuar22.manuellPostering).isEqualTo(0.toBigDecimal())

        assertThat(simuleringFebruar22.tidligereUtbetalt).isEqualTo(0.toBigDecimal())
        assertThat(simuleringFebruar22.resultat).isEqualTo(305.toBigDecimal())
        assertThat(simuleringFebruar22.manuellPostering).isEqualTo(305.toBigDecimal())

        assertThat(simuleringMars22.tidligereUtbetalt).isEqualTo(0.toBigDecimal())
        assertThat(simuleringMars22.resultat).isEqualTo(305.toBigDecimal())
        assertThat(simuleringMars22.manuellPostering).isEqualTo(305.toBigDecimal())

        assertThat(simuleringApril22.tidligereUtbetalt).isEqualTo(140.toBigDecimal())
        assertThat(simuleringApril22.resultat).isEqualTo(165.toBigDecimal())
        assertThat(simuleringApril22.manuellPostering).isEqualTo(165.toBigDecimal())

        assertThat(simuleringsperioder.sumOf { it.manuellPostering }).isEqualTo(775.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(775.toBigDecimal())
    }

    @Test
    fun `ytelse på 2000 korrigert til 3000`() {
        val øktYtelseFra2000Til3000 =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = -2_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 3_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                // Reduser feilutbetaling
                lagØkonomiSimuleringPostering(
                    beløp = -1_000,
                    posteringType = PosteringType.FEILUTBETALING,
                    betalingType = BetalingType.KREDIT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 1_000,
                    posteringType = PosteringType.MOTP,
                    betalingType = BetalingType.DEBIT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -1_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = øktYtelseFra2000Til3000))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(2_000.toBigDecimal())
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(3_000.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(1_000.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(0.toBigDecimal())
    }

    @Test
    fun `ytelse med manuellt trekk av valutajustering deler er trukket`() {
        val ytelsefraBA =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = 305,
                    posteringType = PosteringType.YTELSE,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -198,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 305,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -305,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -107,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 165,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = ytelsefraBA))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(305.toBigDecimal())
        assertThat(simuleringsperioder[0].manuellPostering).isEqualTo(165.toBigDecimal())
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(140.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(165.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(165.toBigDecimal())
    }

    @Test
    fun `ytelse med manuellt trekk av valutajustering trukket på fagområdekode MBA`() {
        val ytelsefraBA =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = 305,
                    posteringType = PosteringType.YTELSE,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -198,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 305,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -305,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -107,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 165,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_MANUELT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = ytelsefraBA))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(305.toBigDecimal())
        assertThat(simuleringsperioder[0].manuellPostering).isEqualTo((165).toBigDecimal())
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(140.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(165.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(165.toBigDecimal())
    }

    @Test
    fun `ytelse med manuellt trekk av valutajustering alt er trukket`() {
        val ytelsefraBA =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = 305,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -153,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 306,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -305,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -153,
                    posteringType = PosteringType.JUSTERING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 305,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = ytelsefraBA))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        val simuleringsperiode = simuleringsperioder.single()

        assertThat(simuleringsperiode.nyttBeløp).isEqualTo(305.toBigDecimal())
        assertThat(simuleringsperiode.manuellPostering).isEqualTo(305.toBigDecimal())
        assertThat(simuleringsperiode.tidligereUtbetalt).isEqualTo(0.toBigDecimal())
        assertThat(simuleringsperiode.resultat).isEqualTo(305.toBigDecimal())
        assertThat(simuleringsperiode.feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(305.toBigDecimal())
    }

    @Test
    fun `ytelse på 3000 korrigert til 12000`() {
        val øktYtelseFra3000Til12000 =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = -3_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 12_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.DEBIT,
                ),
                // Reduser feilutb
                lagØkonomiSimuleringPostering(
                    beløp = -7_000,
                    posteringType = PosteringType.FEILUTBETALING,
                    betalingType = BetalingType.KREDIT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 7_000,
                    posteringType = PosteringType.MOTP,
                    betalingType = BetalingType.DEBIT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -7_000,
                    posteringType = PosteringType.YTELSE,
                    betalingType = BetalingType.KREDIT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = øktYtelseFra3000Til12000))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(simuleringsperioder.size).isEqualTo(1)
        assertThat(simuleringsperioder[0].tidligereUtbetalt).isEqualTo(3_000.toBigDecimal())
        assertThat(simuleringsperioder[0].nyttBeløp).isEqualTo(12_000.toBigDecimal())
        assertThat(simuleringsperioder[0].resultat).isEqualTo(9_000.toBigDecimal())
        assertThat(simuleringsperioder[0].feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(2_000.toBigDecimal())
    }

    /*
    De neste testene antar at brukeren går gjennom følgende førstegangsbehandling og revurderinger i november 2021:
    2021	Feb	    Mar	    Apr	    Mai	    Jun	    Jul	    Aug	    Sep	    Okt	    Nov
    18/11	17153	17153	17153	18195	18195	18195	18195	18195	18195
    22/11	17153	17153   17153   17257	17257	17257	17257   18195   18195
    23/11	17341	17341	17341	18382	18382	18382	18382	18382	18382	18382
     */

    @Test
    fun `førstegangsbehandling 18 nov`() {
        val førstegangsbehandling18Nov =
            mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, 17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 6, 18_195, PosteringType.YTELSE)

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = førstegangsbehandling18Nov))
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(oppsummering.feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(160_629.toBigDecimal())
    }

    @Test
    fun `revurdering 22 nov`() {
        val revurering22Nov =
            // Forrige ytelse
            mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, -17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 6, -18_195, PosteringType.YTELSE) +
                // Ny ytelse
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, 17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 17_257, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 9), 2, 18_195, PosteringType.YTELSE) +
                // Feilutbetaling
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 938, PosteringType.FEILUTBETALING) +
                // Motpost feilutbetaling
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -938, PosteringType.MOTP) +
                // Teknisk postering
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 938, PosteringType.YTELSE)

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = revurering22Nov))
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        assertThat(oppsummering.feilutbetaling).isEqualTo(3_752.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(0.toBigDecimal())
    }

    @Test
    fun `revurdering 23 nov`() {
        val revurdering23Nov =
            // Forrige ytelse
            mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, -17_153, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -17_257, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 9), 2, -18_195, PosteringType.YTELSE) +
                // Ny ytelse
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 2), 3, 17_341, PosteringType.YTELSE) +
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 7, 18_382, PosteringType.YTELSE) +
                // Teknisk postering
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -938, PosteringType.YTELSE) +
                // Reduser feilutbetaling til null
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, -938, PosteringType.FEILUTBETALING) +
                // Motpost feilutbetaling
                mockVedtakSimuleringPosteringer(YearMonth.of(2021, 5), 4, 938, PosteringType.MOTP)

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = revurdering23Nov))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)
        val oppsummering = vedtakSimuleringMottakereTilSimuleringDto(økonomiSimuleringMottakere)

        (3..6).forEach {
            assertThat(simuleringsperioder[it].tidligereUtbetalt).isEqualTo(17_257.toBigDecimal())
            assertThat(simuleringsperioder[it].resultat).isEqualTo(1_125.toBigDecimal())
        }

        assertThat(oppsummering.feilutbetaling).isEqualTo(0.toBigDecimal())
        assertThat(oppsummering.etterbetaling).isEqualTo(20_068.toBigDecimal()) // 1 686 hvis revurderingen ble gjort nov 2021, ikke "i dag"
    }

    @Test
    fun `ytelse med ikke reelle feilutbetalinger skal gi riktig resultat`() {
        val ytelseMetMotposteringerOgManuellePosteringer =
            listOf(
                lagØkonomiSimuleringPostering(
                    beløp = 658,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -657,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -50,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 46,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 46,
                    posteringType = PosteringType.FEILUTBETALING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -46,
                    posteringType = PosteringType.MOTP,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 3,
                    posteringType = PosteringType.YTELSE,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = 3,
                    posteringType = PosteringType.FEILUTBETALING,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
                lagØkonomiSimuleringPostering(
                    beløp = -3,
                    posteringType = PosteringType.MOTP,
                    fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                ),
            )

        val økonomiSimuleringMottakere =
            listOf(lagØkonomiSimuleringMottaker(økonomiSimuleringPostering = ytelseMetMotposteringerOgManuellePosteringer))
        val simuleringsperioder = vedtakSimuleringMottakereTilSimuleringPerioder(økonomiSimuleringMottakere)

        val simuleringsperiode = simuleringsperioder.single()

        assertThat(simuleringsperiode.nyttBeløp).isEqualTo(658.toBigDecimal())
        assertThat(simuleringsperiode.manuellPostering).isEqualTo((-50).toBigDecimal())
        assertThat(simuleringsperiode.tidligereUtbetalt).isEqualTo(707.toBigDecimal())
        assertThat(simuleringsperiode.feilutbetaling).isEqualTo((49).toBigDecimal())
        assertThat(simuleringsperiode.resultat).isEqualTo((-49).toBigDecimal())
        assertThat(simuleringsperiode.etterbetaling).isEqualTo((0).toBigDecimal())
    }
}
