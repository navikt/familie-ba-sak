package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.tilpassValutakurserTilUtenlandskePeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.nov
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

/**
 * Syntaks:
 * ' ' (blank): Skjema finnes ikke for perioden
 * '-': Skjema finnes, men alle felter er null
 * '$': Skjema finnes, valutakode er satt, men ellers null-felter
 * '<siffer>': Skjema har oppgitt kurs og valutakode
 */
class TilpassValutakursTilUtenlandskePeridebeløpTest {
    private val inneværendeMåned = YearMonth.of(2021, 1)
    private val jan2020 = jan(2020)
    private val nov2020 = nov(2020)
    private val barn1 = tilfeldigPerson()
    private val barn2 = tilfeldigPerson()
    private val barn3 = tilfeldigPerson()

    @Test
    fun `test tilpasning av valutakurser mot kompleks endring av utenlandsk valutabeløp`() {
        val gjeldendeValutakurser =
            ValutakursBuilder(jan2020)
                .medKurs("--3456789-----", "EUR", barn1, barn2)
                .bygg()

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan2020)
                .medBeløp("2222 --333-", "EUR", "N", barn1)
                .medBeløp("2223333444 ", "SEK", "N", barn2)
                .medBeløp("-$$$- -23- ", "DKK", "N", barn3)
                .bygg()

        val forventedeValutakurser =
            ValutakursBuilder(jan2020)
                .medKurs("$$34 - 89$-", "EUR", barn1)
                .medKurs("$$$$$$$$$$ ", "SEK", barn2)
                .medKurs("-$$$-  $$- ", "DKK", barn3)
                .medKurs("      -    ", "DKK", barn1, barn3)
                .bygg()

        val faktiskeValutakurser =
            tilpassValutakurserTilUtenlandskePeriodebeløp(gjeldendeValutakurser, utenlandskePeriodebeløp, inneværendeMåned)

        assertEqualsUnordered(forventedeValutakurser, faktiskeValutakurser)
    }

    @Test
    fun `test at endret valuta i utenlandsk periodebeløp fører til endring i valutakurs`() {
        val gjeldendeValutakurser =
            ValutakursBuilder(jan2020)
                .medKurs("333333", "EUR", barn1)
                .bygg()

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(jan2020)
                .medBeløp("222222", "DKK", "DK", barn1)
                .bygg()

        val forventedeValutakurser =
            ValutakursBuilder(jan2020)
                .medKurs("$$$$$$", "DKK", barn1)
                .bygg()

        val faktiskeValutakurser =
            tilpassValutakurserTilUtenlandskePeriodebeløp(gjeldendeValutakurser, utenlandskePeriodebeløp, inneværendeMåned)

        assertEqualsUnordered(forventedeValutakurser, faktiskeValutakurser)
    }

    @Test
    fun `test at ikke fremtidige valutakurser genereres`() {
        val gjeldendeValutakurser =
            ValutakursBuilder(nov2020)
                .medKurs("12345", "PLN", barn1)
                .bygg()

        val utenlandskePeriodebeløp =
            UtenlandskPeriodebeløpBuilder(nov2020)
                .medBeløp("12345>", "PLN", "PL", barn1)
                .bygg()

        val forventedeValutakurser =
            ValutakursBuilder(nov2020)
                .medKurs("123", "PLN", barn1)
                .bygg()

        val faktiskeValutakurser =
            tilpassValutakurserTilUtenlandskePeriodebeløp(gjeldendeValutakurser, utenlandskePeriodebeløp, inneværendeMåned)

        assertThat(faktiskeValutakurser).isEqualTo(forventedeValutakurser)
    }
}
