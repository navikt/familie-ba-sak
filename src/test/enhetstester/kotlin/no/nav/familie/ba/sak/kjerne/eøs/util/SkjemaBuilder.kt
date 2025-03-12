package no.nav.familie.ba.sak.kjerne.eøs.util

import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.tilCharTidslinje
import no.nav.familie.tidslinje.mapVerdi
import no.nav.familie.tidslinje.utvidelser.slåSammenLikePerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import java.time.YearMonth

abstract class SkjemaBuilder<S, B>(
    private val startMåned: YearMonth = jan(2020),
    private val behandlingId: BehandlingId,
) where S : PeriodeOgBarnSkjemaEntitet<S>, B : SkjemaBuilder<S, B> {
    private val skjemaer: MutableList<S> = mutableListOf()

    protected fun medSkjema(
        k: String,
        barn: List<Person>,
        mapChar: (Char?) -> S?,
    ): B {
        val tidslinje =
            k
                .tilCharTidslinje(startMåned)
                .mapVerdi(mapChar)
                .slåSammenLikePerioder()

        tidslinje
            .tilPerioderIkkeNull()
            .map {
                it.verdi.kopier(
                    fom = it.fom?.toYearMonth(),
                    tom = it.tom?.toYearMonth(),
                    barnAktører = barn.map { person -> person.aktør }.toSet(),
                )
            }.all { skjemaer.add(it) }

        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    protected fun medTransformasjon(transformasjon: (S) -> S): B {
        val transformerteSkjemaer = skjemaer.map { skjema -> transformasjon(skjema) }
        skjemaer.clear()
        skjemaer.addAll(transformerteSkjemaer)

        @Suppress("UNCHECKED_CAST")
        return this as B
    }

    fun bygg(): Collection<S> =
        skjemaer
            .map { skjema -> skjema.also { it.behandlingId = behandlingId.id } }

    fun lagreTil(repository: PeriodeOgBarnSkjemaRepository<S>): List<S> = repository.saveAll(bygg())
}
