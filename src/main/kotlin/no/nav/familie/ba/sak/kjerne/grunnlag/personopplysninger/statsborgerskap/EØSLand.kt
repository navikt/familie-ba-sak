package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap

import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.Tidslinje
import no.nav.familie.tidslinje.tilTidslinje
import java.time.LocalDate

enum class EØSLand(
    val landkode: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
) {
    BELGIA("BEL", LocalDate.of(1900, 1, 1), null),
    DANMARK("DNK", LocalDate.of(1900, 1, 1), null),
    TYSKLAND("DEU", LocalDate.of(1900, 1, 1), null),
    ESTLAND("EST", LocalDate.of(2004, 5, 1), null),
    FINLAND("FIN", LocalDate.of(1900, 1, 1), null),
    FRANKRIKE("FRA", LocalDate.of(1900, 1, 1), null),
    HELLAS("GRC", LocalDate.of(1900, 1, 1), null),
    IRLAND("IRL", LocalDate.of(1900, 1, 1), null),
    ISLAND("ISL", LocalDate.of(1900, 1, 1), null),
    ITALIA("ITA", LocalDate.of(1900, 1, 1), null),
    KROATIA("HRV", LocalDate.of(2014, 4, 12), null),
    LATVIA("LVA", LocalDate.of(2004, 5, 1), null),
    LIECHTENSTEIN("LIE", LocalDate.of(1900, 1, 1), null),
    LITAUEN("LTU", LocalDate.of(2004, 5, 1), null),
    LUXEMBOURG("LUX", LocalDate.of(1900, 1, 1), null),
    MALTA("MLT", LocalDate.of(2004, 5, 1), null),
    NEDERLAND("NLD", LocalDate.of(1900, 1, 1), null),
    POLEN("POL", LocalDate.of(2004, 5, 1), null),
    PORTUGAL("PRT", LocalDate.of(1900, 1, 1), null),
    ROMANIA("ROU", LocalDate.of(2007, 8, 1), null),
    SLOVAKIA("SVK", LocalDate.of(2004, 5, 1), null),
    SLOVENIA("SVN", LocalDate.of(2004, 5, 1), null),
    SPANIA("ESP", LocalDate.of(1900, 1, 1), null),
    SVERIGE("SWE", LocalDate.of(1900, 1, 1), null),
    SVEITS("CHE", LocalDate.of(2002, 6, 1), null),
    TSJEKKIA("CZE", LocalDate.of(2004, 5, 1), null),
    UNGARN("HUN", LocalDate.of(2004, 5, 1), null),
    ØSTERRIKE("AUT", LocalDate.of(1995, 1, 1), null),
    BULGARIA("BGR", LocalDate.of(2007, 8, 1), null),
    KYPROS("CYP", LocalDate.of(2004, 5, 1), null),

    STORBRITANNIA("GBR", LocalDate.of(1900, 1, 1), LocalDate.of(2020, 12, 31)),
    ;

    companion object {
        fun hentEøsTidslinje(landkode: String): Tidslinje<Boolean> = entries.filter { it.landkode == landkode }.map { Periode(true, it.fraOgMed, it.tilOgMed) }.tilTidslinje()
    }
}
