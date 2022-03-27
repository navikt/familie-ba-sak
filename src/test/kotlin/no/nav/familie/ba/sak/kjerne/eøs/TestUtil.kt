package no.nav.familie.ba.sak.kjerne.eøs

import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import org.junit.jupiter.api.Assertions
import java.time.YearMonth

object TestUtil {

    fun jan(år: Int = 2022): YearMonth = YearMonth.of(år, 1)
    fun feb(år: Int = 2022): YearMonth = YearMonth.of(år, 2)
    fun mar(år: Int = 2022): YearMonth = YearMonth.of(år, 3)
    fun apr(år: Int = 2022): YearMonth = YearMonth.of(år, 4)
    fun mai(år: Int = 2022): YearMonth = YearMonth.of(år, 5)
    fun jun(år: Int = 2022): YearMonth = YearMonth.of(år, 6)
    fun jul(år: Int = 2022): YearMonth = YearMonth.of(år, 7)
    fun aug(år: Int = 2022): YearMonth = YearMonth.of(år, 8)
    fun sep(år: Int = 2022): YearMonth = YearMonth.of(år, 9)
    fun okt(år: Int = 2022): YearMonth = YearMonth.of(år, 10)
    fun nov(år: Int = 2022): YearMonth = YearMonth.of(år, 11)
    fun des(år: Int = 2022): YearMonth = YearMonth.of(år, 12)

    fun Collection<VilkårResultat>.tilVilkårResultatMåneder() =
        RegelverkPeriodeUtil.lagVilkårResultatMåneder(this)
}

fun <T> assertEqualsUnordered(
    expected: Collection<T>,
    actual: Collection<T>
) {
    Assertions.assertEquals(
        expected.size, actual.size,
        "Forskjellig antall. Forventet ${expected.size} men fikk ${actual.size}"
    )
    Assertions.assertTrue(
        expected.containsAll(actual),
        "Forvantet liste inneholder ikke alle elementene fra faktisk liste"
    )
    Assertions.assertTrue(
        actual.containsAll(expected),
        "Faktisk liste inneholder ikke alle elementene fra forventet liste"
    )
}
