package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.forrigeMåned
import no.nav.familie.ba.sak.common.isSameOrBefore
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad
import java.time.LocalDate

data class InternPeriodeOvergangsstønad(
    val personIdent: String,
    val fomDato: LocalDate,
    val tomDato: LocalDate,
)

fun PeriodeOvergangsstønad.tilInternPeriodeOvergangsstønad() = InternPeriodeOvergangsstønad(
    personIdent = this.personIdent,
    fomDato = this.fomDato,
    tomDato = this.tomDato
)

fun List<InternPeriodeOvergangsstønad>.slåSammenTidligerePerioder(): List<InternPeriodeOvergangsstønad> {
    val tidligerePerioder = this.filter { it.tomDato.isSameOrBefore(LocalDate.now()) }
    val nyePerioder = this.minus(tidligerePerioder)
    return tidligerePerioder.slåSammenSammenhengendePerioder() + nyePerioder
}

fun List<InternPeriodeOvergangsstønad>.slåSammenSammenhengendePerioder(): List<InternPeriodeOvergangsstønad> {
    return this.sortedBy { it.fomDato }
        .fold(mutableListOf()) { sammenslåttePerioder, nestePeriode ->
            if (sammenslåttePerioder.lastOrNull()?.tomDato?.toYearMonth() == nestePeriode.fomDato.forrigeMåned()
            ) {
                sammenslåttePerioder.apply { add(removeLast().copy(tomDato = nestePeriode.tomDato)) }
            } else sammenslåttePerioder.apply { add(nestePeriode) }
        }
}
