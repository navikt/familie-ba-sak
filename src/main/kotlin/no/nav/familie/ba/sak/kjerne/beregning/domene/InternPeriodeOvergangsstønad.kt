package no.nav.familie.ba.sak.kjerne.beregning.domene

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
