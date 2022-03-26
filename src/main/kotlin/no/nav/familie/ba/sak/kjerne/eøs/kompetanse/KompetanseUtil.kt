package no.nav.familie.ba.sak.kjerne.eøs.kompetanse

import no.nav.familie.ba.sak.kjerne.beregning.AktørId
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.Kompetanse
import no.nav.familie.ba.sak.kjerne.tidslinje.Periode
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Måned
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.MånedTidspunkt
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
}

internal fun YearMonth?.tilTidspunktEllerUendeligLengeSiden(default: () -> YearMonth) =
    this.tilTidspunktEllerUendelig(default, Uendelighet.FORTID)

internal fun YearMonth?.tilTidspunktEllerUendeligLengeTil(default: () -> YearMonth) =
    this.tilTidspunktEllerUendelig(default, Uendelighet.FREMTID)

private fun YearMonth?.tilTidspunktEllerUendelig(default: () -> YearMonth, uendelighet: Uendelighet) =
    this?.let { MånedTidspunkt(it, Uendelighet.INGEN) } ?: MånedTidspunkt(default(), uendelighet)

fun Iterable<Kompetanse>?.settFomOgTom(periode: Periode<*, Måned>) =
    this?.map { kompetanse -> kompetanse.settFomOgTom(periode) }

fun Kompetanse.settFomOgTom(periode: Periode<*, Måned>) =
    this.copy(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull()
    ).also { it.id = this.id }

fun Kompetanse?.settFomOgTomOgBarn(periode: Periode<*, Måned>, barn: AktørId) =
    this?.copy(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull(),
        barnAktørIder = setOf(barn)
    )?.also { it.id = this.id } ?: Kompetanse(
        fom = periode.fraOgMed.tilYearMonthEllerNull(),
        tom = periode.tilOgMed.tilYearMonthEllerNull(),
        barnAktørIder = setOf(barn)
    )
