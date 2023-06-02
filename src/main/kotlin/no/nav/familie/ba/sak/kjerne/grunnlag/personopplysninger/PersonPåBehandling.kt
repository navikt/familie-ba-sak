package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.kjerne.personident.Aktør

data class PersonPåBehandling(
    val type: PersonType,
    val aktør: Aktør,
)

fun List<Pair<PersonType, Aktør>>.tilPersonerPåBehandling(): List<PersonPåBehandling>? =
    this.map { PersonPåBehandling(it.first, it.second) }
        .takeIf { it.isNotEmpty() }

fun List<PersonPåBehandling>.barn() = this.filter { it.type == PersonType.BARN }.map { it.aktør }