package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeSiden
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.tilTidspunktEllerUendeligLengeTil
import java.time.LocalDate

data class PeriodeDto(
    val id: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val avhengerAv: Collection<Long>,
    val innholdReferanse: Long
)

fun <T> PeriodeDto.tilPeriode(innhold: T): Periode<T> {
    if (tom == null && fom == null)
        throw IllegalStateException("Både fom og tom er null")

    val tidligste = fom ?: tom!!
    val seneste = tom ?: fom!!

    return Periode(
        id = id,
        fom = fom.tilTidspunktEllerUendeligLengeSiden(tidligste),
        tom = tom.tilTidspunktEllerUendeligLengeTil(seneste),
        innhold = innhold,
        avhengerAv = avhengerAv
    )
}

fun <T> Periode<T>.tilDto(tilInnholdReferanse: (Periode<T>) -> Long) =
    PeriodeDto(
        id = this.id,
        fom = this.fom.tilLocalDateEllerNull(),
        tom = this.tom.tilLocalDateEllerNull(),
        avhengerAv = avhengerAv,
        tilInnholdReferanse(this)
    )

interface PeriodeRepository {
    fun hentPerioder(
        tidslinjeType: String,
        tidslinjeId: String,
        innholdReferanser: List<Long>
    ): Iterable<PeriodeDto>

    fun lagrePerioder(
        tidslinjeType: String,
        tidslinjeId: String,
        perioder: Iterable<PeriodeDto>
    ): Iterable<PeriodeDto>
}

interface TidslinjeRepository<T> {
    fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>>
    fun hent(): Collection<Periode<T>>?
}

class IngenTidslinjeSerialisering<T> : TidslinjeRepository<T> {
    override fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>> {
        return perioder
    }

    override fun hent(): Collection<Periode<T>> = emptyList()
}
