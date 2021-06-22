package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate
import java.time.LocalDateTime

data class RestRegisterhistorikk(
        val hentetTidspunkt: LocalDateTime,
        val sivilstand: List<RestRegisteropplysning>? = emptyList(),
        val oppholdstillatelse: List<RestRegisteropplysning>? = emptyList(),
        val statsborgerskap: List<RestRegisteropplysning>? = emptyList(),
        val bostedsadresse: List<RestRegisteropplysning>? = emptyList(),
)

fun Person.tilRestRegisterhistorikk() = RestRegisterhistorikk(
        hentetTidspunkt = this.personopplysningGrunnlag.opprettetTidspunkt,
        oppholdstillatelse = opphold.map { it.tilRestRegisteropplysning() },
        statsborgerskap = statsborgerskap.map { it.tilRestRegisteropplysning() },
        bostedsadresse = this.bostedsadresser.map { it.tilRestRegisteropplysning() },
        sivilstand = this.sivilstander.map { it.tilRestRegisteropplysning() },
)


data class RestRegisteropplysning(
        val fom: LocalDate?,
        val tom: LocalDate?,
        var verdi: String,
)