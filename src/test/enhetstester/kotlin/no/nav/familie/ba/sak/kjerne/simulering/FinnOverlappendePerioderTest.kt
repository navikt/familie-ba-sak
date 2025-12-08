package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.simulering.domene.OverlappendePerioderMedAndreFagsaker
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FinnOverlappendePerioderTest {
    @Test
    fun `finnOverlappendePerioder finner overlappende perioder i forskjellige fagsaker`() {
        // Arrange
        val økonomiSimuleringMottakere =
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                beløp = 100,
                                fagsakId = 1,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(2),
                                tom = LocalDate.now().minusMonths(4),
                                posteringType = PosteringType.FEILUTBETALING,
                                beløp = 100,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusMonths(7),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.FEILUTBETALING,
                                beløp = 100,
                                fagsakId = 3,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(4),
                                tom = LocalDate.now().minusMonths(13),
                                posteringType = PosteringType.FEILUTBETALING,
                                beløp = 100,
                                fagsakId = 4,
                            ),
                        ),
                ),
            )

        // Act
        val overlappendePerioder = finnOverlappendePerioder(økonomiSimuleringMottakere = økonomiSimuleringMottakere, 1)

        // Assert
        val for9MånederSiden = LocalDate.now().minusMonths(9)
        val fagsakerSomOverlapperFor9MånederSiden = overlappendePerioder.finnPeriodeForDato(for9MånederSiden)?.fagsakerMedFeilutbetaling
        assertThat(fagsakerSomOverlapperFor9MånederSiden).isEqualTo(listOf(2L))

        val for6MånederSiden = LocalDate.now().minusMonths(6)
        val fagsakerSomOverlapperFor6MånederSiden = overlappendePerioder.finnPeriodeForDato(for6MånederSiden)?.fagsakerMedFeilutbetaling
        assertThat(fagsakerSomOverlapperFor6MånederSiden).isEqualTo(listOf(2L, 3L))

        assertThat(overlappendePerioder.none { it.fagsakerMedFeilutbetaling.contains(1L) }).`as`("Genererer overlappende perioder for samme fagsak det skal finnes overlapp for").isTrue()
        assertThat(overlappendePerioder.none { it.fagsakerMedFeilutbetaling.contains(4L) }).`as`("Genererer overlappende perioder for fagsak som ikke skal overlappe").isTrue()
    }

    @Test
    fun `finnOverlappendePerioder finner overlappende perioder selv hvis det er forskjellige mottakere på samme behandling`() {
        // Arrange
        val økonomiSimuleringMottakere =
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.YTELSE,
                                beløp = 984,
                                fagsakId = 1,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.JUSTERING,
                                beløp = -984,
                                fagsakId = 1,
                            ),
                        ),
                ),
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.YTELSE,
                                beløp = 984,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.FEILUTBETALING,
                                beløp = 984,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.JUSTERING,
                                beløp = -984,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.MOTP,
                                beløp = -984,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.of(2025, 8, 1),
                                tom = LocalDate.of(2025, 8, 31),
                                posteringType = PosteringType.YTELSE,
                                beløp = -1968,
                                fagsakId = 2,
                            ),
                        ),
                ),
            )

        // Act
        val overlappendePerioder = finnOverlappendePerioder(økonomiSimuleringMottakere = økonomiSimuleringMottakere, 1)

        // Assert
        assertThat(overlappendePerioder).hasSize(1)
        val overlappendeFagsak = overlappendePerioder.singleOrNull()?.fagsakerMedFeilutbetaling?.singleOrNull()
        assertThat(overlappendeFagsak).isEqualTo(2L)
    }

    @Test
    fun `finnOverlappendePerioder finner overlappende perioder i forskjellige fagsaker med både feilutbetaling og etterbetaling`() {
        // Arrange
        val økonomiSimuleringMottakere =
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                beløp = 100,
                                fagsakId = 1,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(2),
                                tom = LocalDate.now().minusMonths(4),
                                posteringType = PosteringType.FEILUTBETALING,
                                forfallsdato = LocalDate.now().minusMonths(4),
                                beløp = 100,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusMonths(7),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.FEILUTBETALING,
                                beløp = -100,
                                fagsakId = 3,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusMonths(7),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.YTELSE,
                                beløp = 50,
                                fagsakId = 3,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(4),
                                tom = LocalDate.now().minusMonths(13),
                                posteringType = PosteringType.FEILUTBETALING,
                                beløp = 100,
                                fagsakId = 4,
                            ),
                        ),
                ),
            )

        // Act
        val overlappendePerioder = finnOverlappendePerioder(økonomiSimuleringMottakere = økonomiSimuleringMottakere, 1)

        // Assert
        val for9MånederSiden = LocalDate.now().minusMonths(9)
        val fagsakerSomOverlapperFor9MånederSiden = overlappendePerioder.finnPeriodeForDato(for9MånederSiden)?.fagsakerMedFeilutbetaling
        assertThat(fagsakerSomOverlapperFor9MånederSiden).isEqualTo(listOf(2L))

        val for6MånederSiden = LocalDate.now().minusMonths(6)
        val fagsakerSomOverlapperPåFeilUtbetalingFor6MånederSiden = overlappendePerioder.finnPeriodeForDato(for6MånederSiden)?.fagsakerMedFeilutbetaling
        assertThat(fagsakerSomOverlapperPåFeilUtbetalingFor6MånederSiden).isEqualTo(listOf(2L))

        val fagsakerSomOverlapperPåEtterbetalingFor6MånederSiden = overlappendePerioder.finnPeriodeForDato(for6MånederSiden)?.fagsakerMedEtterbetaling
        assertThat(fagsakerSomOverlapperPåEtterbetalingFor6MånederSiden).isEqualTo(listOf(3L))

        assertThat(overlappendePerioder.none { it.fagsakerMedFeilutbetaling.contains(1L) }).`as`("Genererer overlappende perioder for samme fagsak det skal finnes overlapp for").isTrue()
        assertThat(overlappendePerioder.none { it.fagsakerMedFeilutbetaling.contains(4L) }).`as`("Genererer overlappende perioder for fagsak som ikke skal overlappe").isTrue()
    }

    @Test
    fun `finnOverlappendePerioder finner ingen overlappende perioder hvis det ikke er overlapp`() {
        // Arrange
        val økonomiSimuleringMottakere =
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                beløp = 100,
                                fagsakId = 1,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(4),
                                tom = LocalDate.now().minusMonths(13),
                                beløp = 100,
                                fagsakId = 4,
                            ),
                        ),
                ),
            )

        // Act
        val overlappendePerioder = finnOverlappendePerioder(økonomiSimuleringMottakere = økonomiSimuleringMottakere, 1)

        // Assert
        assertThat(overlappendePerioder).isEmpty()
    }

    @Test
    fun `finnOverlappendePerioder skal fjerne like perioder per fagsak`() {
        // Arrange
        val økonomiSimuleringMottakere =
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.YTELSE,
                                beløp = 100,
                                fagsakId = 1,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.YTELSE,
                                beløp = 100,
                                fagsakId = 1,
                            ),
                        ),
                ),
            )

        // Act
        val overlappendePerioder = finnOverlappendePerioder(økonomiSimuleringMottakere = økonomiSimuleringMottakere, 1)

        // Assert
        assertThat(overlappendePerioder).isEmpty()
    }

    @Test
    fun `finnOverlappendePerioder skal fjerne like perioder per fagsak uavhengig av posteringstype`() {
        // Arrange
        val økonomiSimuleringMottakere =
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.YTELSE,
                                beløp = 100,
                                fagsakId = 1,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusYears(1),
                                tom = LocalDate.now(),
                                posteringType = PosteringType.JUSTERING,
                                beløp = 100,
                                fagsakId = 1,
                            ),
                        ),
                ),
            )

        // Act
        val overlappendePerioder = finnOverlappendePerioder(økonomiSimuleringMottakere = økonomiSimuleringMottakere, 1)

        // Assert
        assertThat(overlappendePerioder).isEmpty()
    }

    private fun List<OverlappendePerioderMedAndreFagsaker>.finnPeriodeForDato(dato: LocalDate): OverlappendePerioderMedAndreFagsaker? =
        find {
            it.fom <= dato && it.tom >= dato
        }
}
