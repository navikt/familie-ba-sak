package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import java.time.LocalDate
import java.time.LocalDateTime

data class RegisterhistorikkDto(
    val hentetTidspunkt: LocalDateTime,
    val sivilstand: List<RegisteropplysningDto>? = emptyList(),
    val oppholdstillatelse: List<RegisteropplysningDto>? = emptyList(),
    val statsborgerskap: List<RegisteropplysningDto>? = emptyList(),
    val bostedsadresse: List<RegisteropplysningDto>? = emptyList(),
    val deltBosted: List<RegisteropplysningDto>? = emptyList(),
    val oppholdsadresse: List<RegisteropplysningDto>? = emptyList(),
    val dødsboadresse: List<RegisteropplysningDto>? = emptyList(),
    val historiskeIdenter: List<RegisteropplysningDto>? = emptyList(),
)

fun Person.tilRegisterhistorikkDto(eldsteBarnsFødselsdato: LocalDate?) =
    RegisterhistorikkDto(
        hentetTidspunkt = this.personopplysningGrunnlag.opprettetTidspunkt,
        oppholdstillatelse = opphold.map { it.tilRegisteropplysningDto() },
        statsborgerskap = statsborgerskap.map { it.tilRegisteropplysningDto() },
        bostedsadresse = this.bostedsadresser.map { it.tilRegisteropplysningDto() }.fyllInnTomDatoer(),
        oppholdsadresse = this.oppholdsadresser.map { it.tilRegisteropplysningDto() }.fyllInnTomDatoer(),
        deltBosted = this.deltBosted.map { it.tilRegisteropplysningDto() }.fyllInnTomDatoer(),
        sivilstand = this.sivilstander.map { it.tilRegisteropplysningDto() },
        dødsboadresse = if (this.dødsfall == null) emptyList() else listOf(this.dødsfall!!.tilRegisteropplysningDto()),
        historiskeIdenter =
            this.aktør.personidenter
                .filter { ident ->
                    eldsteBarnsFødselsdato == null ||
                        ident.gjelderTil?.toLocalDate()?.isSameOrAfter(eldsteBarnsFødselsdato) != false
                }.map {
                    RegisteropplysningDto(
                        fom = null,
                        tom = it.gjelderTil?.toLocalDate(),
                        verdi = it.fødselsnummer,
                    )
                },
    )

data class RegisteropplysningDto(
    val fom: LocalDate?,
    val tom: LocalDate?,
    var verdi: String,
)

fun List<RegisteropplysningDto>.fyllInnTomDatoer(): List<RegisteropplysningDto> =
    this
        .sortedBy { it.fom }
        .foldRight(mutableListOf<RegisteropplysningDto>()) { foregående, acc ->
            if (acc.isEmpty() || foregående.tom != null || foregående.fom == null) {
                acc.add(foregående)
            } else {
                acc.add(foregående.copy(tom = acc.last().fom?.minusDays(1)))
            }
            acc
        }.reversed()
