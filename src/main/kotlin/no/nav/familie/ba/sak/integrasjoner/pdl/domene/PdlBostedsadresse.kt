package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse

data class PdlBostedsadresseResponse(
    val person: PdlBostedsadressePerson?,
)

data class PdlBostedsadressePerson(
    val bostedsadresse: List<Bostedsadresse>,
)
