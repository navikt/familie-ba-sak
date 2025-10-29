package no.nav.familie.ba.sak.kjerne.simulering

import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.kjerne.simulering.domene.OverlappendePerioderMedAndreFagsaker
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
                                beløp = 100,
                                fagsakId = 2,
                            ),
                            lagØkonomiSimuleringPostering(
                                fom = LocalDate.now().minusMonths(7),
                                tom = LocalDate.now(),
                                beløp = 100,
                                fagsakId = 3,
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
        val for9MånederSiden = LocalDate.now().minusMonths(9)
        val fagsakerSomOverlapperFor9MånederSiden = overlappendePerioder.finnPeriodeForDato(for9MånederSiden)?.fagsaker
        assertThat(fagsakerSomOverlapperFor9MånederSiden).isEqualTo(listOf(2L))

        val for6MånederSiden = LocalDate.now().minusMonths(6)
        val fagsakerSomOverlapperFor6MånederSiden = overlappendePerioder.finnPeriodeForDato(for6MånederSiden)?.fagsaker
        assertThat(fagsakerSomOverlapperFor6MånederSiden).isEqualTo(listOf(2L, 3L))

        assertThat(overlappendePerioder.none { it.fagsaker.contains(1L) }).isTrue() // listen skal ikke inneholde fagsaken det er søkt for
        assertThat(overlappendePerioder.none { it.fagsaker.contains(4L) }).isTrue()
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

    private fun List<OverlappendePerioderMedAndreFagsaker>.finnPeriodeForDato(dato: LocalDate): OverlappendePerioderMedAndreFagsaker? =
        find {
            it.fom <= dato && it.tom >= dato
        }
}
