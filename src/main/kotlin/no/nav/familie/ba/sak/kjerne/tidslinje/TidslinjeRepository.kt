package no.nav.familie.ba.sak.kjerne.tidslinje

import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.Periode
import no.nav.familie.ba.sak.kjerne.eøs.temaperiode.mapInnhold

interface PeriodeRepository {
    fun hentPerioder(
        tidslinjeId: String,
        innhold: List<String>
    ): Iterable<Periode<String>>

    fun lagrePerioder(
        tidslinjeId: String,
        perioder: Iterable<Periode<String>>
    ): Iterable<Periode<String>>
}

interface TidslinjeRepository<T> {
    fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>>
    fun hent(): Collection<Periode<T>>
}

abstract class AbstraktTidslinjeRepository<T>(
    protected val innhold: Iterable<T>,
    private val periodeRepository: PeriodeRepository
) : TidslinjeRepository<T> {

    abstract val tidslinjeId: String
    abstract fun innholdTilString(innhold: T?): String
    private fun stringTilInnhold(referanse: String?): T? =
        innhold.find { innholdTilString(it) == referanse }!!

    final override fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>> {
        return periodeRepository
            .lagrePerioder(tidslinjeId, perioder.mapInnhold { innholdTilString(it) })
            .mapInnhold { stringTilInnhold(it) }
    }

    final override fun hent(): Collection<Periode<T>> {
        return periodeRepository.hentPerioder(
            tidslinjeId = tidslinjeId,
            innhold = innhold.map { innholdTilString(it) }
        ).mapInnhold { stringTilInnhold(it) }
    }
}

class IngenTidslinjeRepository<T> : TidslinjeRepository<T> {
    override fun lagre(perioder: Collection<Periode<T>>): Collection<Periode<T>> {
        return perioder
    }

    override fun hent(): Collection<Periode<T>> = emptyList()
}
