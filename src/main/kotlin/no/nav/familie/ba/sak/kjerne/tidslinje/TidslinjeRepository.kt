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
    val innholdReferanse: String
)

fun <T> PeriodeDto.tilPeriode(refTilInnhold: (String) -> T?): Periode<T> {
    if (tom == null && fom == null)
        throw IllegalStateException("Både fom og tom er null")

    return Periode(
        id = id,
        fom = fom.tilTidspunktEllerUendeligLengeSiden { tom!! },
        tom = tom.tilTidspunktEllerUendeligLengeTil { fom!! },
        innhold = refTilInnhold(innholdReferanse),
        avhengerAv = avhengerAv
    )
}

fun <T> Periode<T>.tilDto(innholdRef: String) =
    PeriodeDto(
        id = this.id,
        fom = this.fom.tilLocalDateEllerNull(),
        tom = this.tom.tilLocalDateEllerNull(),
        avhengerAv = avhengerAv,
        innholdReferanse = innholdRef
    )

interface PeriodeRepository {
    fun hentPerioder(
        tidslinjeId: String,
        innholdReferanser: List<String>
    ): Iterable<PeriodeDto>

    fun lagrePerioder(
        tidslinjeId: String,
        perioder: Iterable<PeriodeDto>
    ): Iterable<PeriodeDto>
}

interface TidslinjeRepository<T> {
    fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>>
    fun hent(): Collection<Periode<T>>
}

class IngenTidslinjeRepository<T> : TidslinjeRepository<T> {
    override fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>> {
        return perioder
    }

    override fun hent(): Collection<Periode<T>> = emptyList()
}
