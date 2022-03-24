package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseStatus
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.Tidslinje
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.TidslinjeSomStykkerOppTiden
import no.nav.familie.ba.sak.kjerne.tidslinje.komposisjon.hentUtsnitt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Uendelighet
import java.time.YearMonth

object KompetanseUtil {
    fun finnRestKompetanser(gammelKompetanse: Kompetanse, oppdatertKompetanse: Kompetanse): Collection<Kompetanse> {

        val kompetanseForRestBarn = gammelKompetanse
            .copy(
                barnAktørIder = gammelKompetanse.barnAktørIder.minus(oppdatertKompetanse.barnAktørIder)
            ).takeIf { it.barnAktørIder.size > 0 }

        val kompetanseForForegåendePerioder = gammelKompetanse
            .copy(
                fom = gammelKompetanse.fom,
                tom = oppdatertKompetanse.fom?.minusMonths(1),
                barnAktørIder = oppdatertKompetanse.barnAktørIder
            ).takeIf { it.fom != null && it.fom < it.tom }

        val kompetanseForEtterfølgendePerioder = gammelKompetanse.copy(
            fom = oppdatertKompetanse.tom?.plusMonths(1),
            tom = gammelKompetanse.tom,
            barnAktørIder = oppdatertKompetanse.barnAktørIder
        ).takeIf { it.fom != null && it.fom < it.tom }

        return listOf(kompetanseForRestBarn, kompetanseForForegåendePerioder, kompetanseForEtterfølgendePerioder)
            .filterNotNull()
    }

    fun revurderStatus(kompetanser: List<Kompetanse>): List<Kompetanse> =
        kompetanser.map { revurderStatus(it) }

    fun revurderStatus(kompetanse: Kompetanse): Kompetanse {
        val sum = (kompetanse.annenForeldersAktivitet?.let { 1 } ?: 0) +
            (kompetanse.barnetsBostedsland?.let { 1 } ?: 0) +
            (kompetanse.primærland?.let { 1 } ?: 0) +
            (kompetanse.sekundærland?.let { 1 } ?: 0) +
            (kompetanse.søkersAktivitet?.let { 1 } ?: 0)

        val nyStatus = when (sum) {
            5 -> KompetanseStatus.OK
            in 1..4 -> KompetanseStatus.UFULLSTENDIG
            else -> KompetanseStatus.IKKE_UTFYLT
        }

        return kompetanse.copy(status = nyStatus)
    }

    fun mergeKompetanser(kompetanser: Collection<Kompetanse>): Collection<Kompetanse> {
        return kompetanser.map { EnkeltKompetanseTidslinje(it) }
            .let { SlåSammenKompetanserTidslinje(it) }
            .perioder().flatMap { periode -> periode.innhold?.settFomOgTom(periode) ?: emptyList() }
    }

    fun Iterable<Kompetanse>?.settFomOgTom(periode: Periode<*, Måned>) =
        this?.map {
            it.copy(
                fom = periode.fraOgMed.tilYearMonthEllerNull(),
                tom = periode.tilOgMed.tilYearMonthEllerNull()
            )
        }
}

internal class EnkeltKompetanseTidslinje(
    val kompetanse: Kompetanse
) : Tidslinje<Kompetanse, Måned>() {
    override fun fraOgMed() = kompetanse.fom.tilTidspunktEllerUendeligLengeSiden { kompetanse.tom ?: YearMonth.now() }

    override fun tilOgMed() = kompetanse.tom.tilTidspunktEllerUendeligLengeTil { kompetanse.fom ?: YearMonth.now() }

    override fun lagPerioder(): Collection<Periode<Kompetanse, Måned>> {
        return listOf(Periode(fraOgMed(), tilOgMed(), kompetanse.copy(fom = null, tom = null)))
    }
}

internal class SlåSammenKompetanserTidslinje(
    val kompetanseTidslinjer: Collection<EnkeltKompetanseTidslinje>
) : TidslinjeSomStykkerOppTiden<Set<Kompetanse>, Måned>(kompetanseTidslinjer) {
    override fun finnInnholdForTidspunkt(tidspunkt: Tidspunkt<Måned>): Set<Kompetanse>? {
        return kompetanseTidslinjer
            .map { it.hentUtsnitt(tidspunkt) }
            .filterNotNull()
            .fold(mutableSetOf()) { kompetanser, kompetanse ->
                val matchendeKompetanse = kompetanser.plukkUtHvis { it.erLikUtenBarn(kompetanse) }
                val oppdatertKompetanse = matchendeKompetanse?.leggSammenBarn(kompetanse) ?: kompetanse
                kompetanser.add(oppdatertKompetanse)
                kompetanser
            }
    }

    private fun <T> MutableSet<T>.plukkUtHvis(predicate: (T) -> Boolean): T? =
        this.find { predicate(it) }?.also { this.remove(it) }

    private fun Kompetanse.erLikUtenBarn(kompetanse: Kompetanse) =
        this.copy(barnAktørIder = emptySet()) == kompetanse.copy(barnAktørIder = emptySet())

    private fun Kompetanse.leggSammenBarn(kompetanse: Kompetanse) =
        this.copy(barnAktørIder = this.barnAktørIder.plus(kompetanse.barnAktørIder))
}

private fun YearMonth?.tilTidspunktEllerUendeligLengeSiden(default: () -> YearMonth) =
    this.tilTidspunktEllerUendelig(default, Uendelighet.FORTID)

private fun YearMonth?.tilTidspunktEllerUendeligLengeTil(default: () -> YearMonth) =
    this.tilTidspunktEllerUendelig(default, Uendelighet.FREMTID)

private fun YearMonth?.tilTidspunktEllerUendelig(default: () -> YearMonth, uendelighet: Uendelighet) =
    this?.let { MånedTidspunkt(it, Uendelighet.INGEN) } ?: MånedTidspunkt(default(), uendelighet)
