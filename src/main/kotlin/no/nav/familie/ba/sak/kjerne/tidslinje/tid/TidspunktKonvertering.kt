package no.nav.familie.ba.sak.kjerne.tidslinje.tid

import no.nav.familie.ba.sak.common.sisteDagIMåned
import no.nav.familie.ba.sak.common.toYearMonth

fun <T : Tidsenhet> Tidspunkt<T>.tilFørsteDagIMåneden() = when (this) {
    is DagTidspunkt -> DagTidspunkt(this.dato.withDayOfMonth(1), uendelighet)
    is MånedTidspunkt -> DagTidspunkt(this.måned.atDay(1), uendelighet)
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.tilSisteDagIMåneden() = when (this) {
    is DagTidspunkt -> DagTidspunkt(this.dato.sisteDagIMåned(), uendelighet)
    is MånedTidspunkt -> DagTidspunkt(this.måned.atEndOfMonth(), uendelighet)
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.tilInneværendeMåned() = when (this) {
    is DagTidspunkt -> MånedTidspunkt(this.dato.toYearMonth(), uendelighet)
    is MånedTidspunkt -> this
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.tilNesteMåned() = when (this) {
    is DagTidspunkt -> MånedTidspunkt(this.dato.toYearMonth(), uendelighet).neste()
    is MånedTidspunkt -> this.neste()
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.tilForrigeMåned() = when (this) {
    is DagTidspunkt -> MånedTidspunkt(this.dato.toYearMonth(), uendelighet).forrige()
    is MånedTidspunkt -> this.forrige()
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.tilDagEllerFørsteDagIPerioden() = when (this) {
    is DagTidspunkt -> this
    is MånedTidspunkt -> DagTidspunkt(this.måned.atDay(1), this.uendelighet)
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.tilDagEllerSisteDagIPerioden() = when (this) {
    is DagTidspunkt -> this
    is MånedTidspunkt -> DagTidspunkt(this.måned.atEndOfMonth(), this.uendelighet)
    else -> NullTidspunkt(this.uendelighet)
}

fun <T : Tidsenhet> Tidspunkt<T>.somFraOgMed() = when (uendelighet) {
    Uendelighet.FREMTID -> somEndelig()
    else -> this
}

fun <T : Tidsenhet> Tidspunkt<T>.somTilOgMed() = when (uendelighet) {
    Uendelighet.FORTID -> somEndelig()
    else -> this
}

fun <T : Tidsenhet> Tidspunkt<T>.neste() = flytt(1)
fun <T : Tidsenhet> Tidspunkt<T>.forrige() = flytt(-1)
fun <T : Tidsenhet> Tidspunkt<T>.erRettFør(tidspunkt: Tidspunkt<T>) = neste() == tidspunkt
fun <T : Tidsenhet> Tidspunkt<T>.erEndelig(): Boolean = uendelighet == Uendelighet.INGEN
fun <T : Tidsenhet> Tidspunkt<T>.erUendeligLengeSiden(): Boolean = uendelighet == Uendelighet.FORTID
fun <T : Tidsenhet> Tidspunkt<T>.erUendeligLengeTil(): Boolean = uendelighet == Uendelighet.FREMTID
