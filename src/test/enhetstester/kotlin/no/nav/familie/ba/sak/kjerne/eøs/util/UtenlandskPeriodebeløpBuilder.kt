package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje

class UtenlandskPeriodebeløpBuilder(
    private val startMåned: Tidspunkt<Måned> = jan(2020),
    private val behandlingId: Long = 1
) {
    private val utenlandskePeriodebeløp: MutableList<UtenlandskPeriodebeløp> = mutableListOf()

    fun medBeløp(k: String, valutakode: String, vararg barn: Person): UtenlandskPeriodebeløpBuilder {
        val mal = UtenlandskPeriodebeløp.NULL.copy(barnAktører = barn.map { it.aktør }.toSet())
        val utenlandskPeriodebeløpTidslinje = k.tilCharTidslinje(startMåned)
            .map {
                when {
                    it == '-' -> mal
                    it?.isDigit() ?: false -> {
                        mal.copy(
                            beløp = it?.digitToInt()?.toBigDecimal(),
                            valutakode = "EUR",
                            intervall = "MND"
                        )
                    }
                    else -> null
                }
            }

        utenlandskPeriodebeløpTidslinje.perioder()
            .filter { it.innhold != null }
            .map {
                it.innhold!!.copy(
                    fom = it.fraOgMed.tilYearMonthEllerNull(),
                    tom = it.tilOgMed.tilYearMonthEllerNull()
                )
            }
            .all { utenlandskePeriodebeløp.add(it) }

        return this
    }

    fun bygg(): Collection<UtenlandskPeriodebeløp> = utenlandskePeriodebeløp
        .map { utenlandskperiodebeløp -> utenlandskperiodebeløp.also { it.behandlingId = behandlingId } }

    fun lagreTil(repository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp>) {
        repository.saveAll(bygg())
    }
}
