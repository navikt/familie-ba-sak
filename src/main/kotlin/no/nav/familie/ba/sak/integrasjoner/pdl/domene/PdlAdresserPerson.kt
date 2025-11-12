package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted
import no.nav.familie.kontrakter.felles.personopplysning.Oppholdsadresse

data class PdlAdresserPerson(
    val bostedsadresse: List<Bostedsadresse> = emptyList(),
    val deltBosted: List<DeltBosted> = emptyList(),
    val oppholdsadresse: List<Oppholdsadresse> = emptyList(),
)
