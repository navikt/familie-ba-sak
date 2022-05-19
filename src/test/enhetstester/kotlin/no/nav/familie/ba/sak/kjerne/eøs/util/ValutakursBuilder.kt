package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.valutakurs.Valutakurs
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.transformasjon.map
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje

class ValutakursBuilder(
    private val startMåned: Tidspunkt<Måned> = jan(2020),
    private val behandlingId: Long = 1
) {
    private val valutakurser: MutableList<Valutakurs> = mutableListOf()

    fun medKurs(k: String, valutakode: String, vararg barn: Person): ValutakursBuilder {
        val mal = Valutakurs.NULL.copy(barnAktører = barn.map { it.aktør }.toSet())
        val utenlandskPeriodebeløpTidslinje = k.tilCharTidslinje(startMåned)
            .map {
                when {
                    it == '-' -> mal
                    it?.isDigit() ?: false -> {
                        mal.copy(
                            kurs = it?.digitToInt()?.toBigDecimal(),
                            valutakode = "EUR",
                            valutakursdato = null
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
            .all { valutakurser.add(it) }

        return this
    }

    fun bygg(): Collection<Valutakurs> = valutakurser
        .map { valutakurs -> valutakurs.also { it.behandlingId = behandlingId } }

    fun lagreTil(repository: PeriodeOgBarnSkjemaRepository<Valutakurs>) {
        repository.saveAll(bygg())
    }
}
