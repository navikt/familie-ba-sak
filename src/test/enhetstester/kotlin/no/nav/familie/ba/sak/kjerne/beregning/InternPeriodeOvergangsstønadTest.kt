package no.nav.familie.ba.sak.kjerne.beregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.randomPersonident
import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.kjerne.beregning.domene.InternPeriodeOvergangsstønad
import no.nav.familie.ba.sak.kjerne.beregning.domene.slåSammenSammenhengendePerioder
import no.nav.familie.ba.sak.kjerne.beregning.domene.splitFramtidigePerioderFraForrigeBehandling
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class InternPeriodeOvergangsstønadTest {
    @Test
    fun `Skal slå sammen perioder som er sammenhengende`() {
        val personIdent = randomFnr()
        val sammenslåttePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(6).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(3).sisteDagIMåned(),
                personIdent = personIdent
            ),
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().sisteDagIMåned(),
                personIdent = personIdent
            )
        ).slåSammenSammenhengendePerioder()

        assertEquals(1, sammenslåttePerioder.size)
    }

    @Test
    fun `Skal ikke slå sammen perioder som ikke er sammenhengende`() {
        val personIdent = randomFnr()
        val sammenslåttePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(6).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(4).sisteDagIMåned(),
                personIdent = personIdent
            ),
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(2).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().sisteDagIMåned(),
                personIdent = personIdent
            )
        ).slåSammenSammenhengendePerioder()

        assertEquals(2, sammenslåttePerioder.size)
    }

    @Test
    fun `Skal kaste feil hvis OS-perioder hører til to forskjellige personer`() {
        val aktør1 = randomAktør(fnr = "12345678910")
        val aktør2 = randomAktør(fnr = "23456789101")

        val gamlePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusYears(3).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                personIdent = aktør1.aktivFødselsnummer()
            ),
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                personIdent = aktør1.aktivFødselsnummer()
            )
        )

        val nyePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusYears(3).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                personIdent = aktør2.aktivFødselsnummer()
            )
        )

        assertThrows<Feil> { nyePerioder.splitFramtidigePerioderFraForrigeBehandling(overgangsstønadPerioderFraForrigeBehandling = gamlePerioder, søkerAktør = aktør2) }
    }
    @Test
    fun `Skal ikke kaste feil hvis OS-perioder har forskjellige personidenter, men er knyttet til samme aktør`() {
        val ident1 = "12345678910"
        val ident2 = "23456789101"
        val aktør = randomAktør(fnr = ident1).also {
            it.personidenter.add(
                randomPersonident(it, ident2)
            )
        }

        val gamlePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusYears(3).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(5).sisteDagIMåned(),
                personIdent = ident1
            ),
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusMonths(4).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                personIdent = ident1
            )
        )

        val nyePerioder = listOf(
            InternPeriodeOvergangsstønad(
                fomDato = LocalDate.now().minusYears(3).førsteDagIInneværendeMåned(),
                tomDato = LocalDate.now().minusMonths(1).sisteDagIMåned(),
                personIdent = ident2
            )
        )

        assertDoesNotThrow { nyePerioder.splitFramtidigePerioderFraForrigeBehandling(overgangsstønadPerioderFraForrigeBehandling = gamlePerioder, søkerAktør = aktør) }
    }
}
