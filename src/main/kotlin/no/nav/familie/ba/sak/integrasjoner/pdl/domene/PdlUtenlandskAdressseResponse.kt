package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.UtenlandskAdresse

class PdlUtenlandskAdressseResponse(
    val person: PdlUtenlandskAdresssePerson?,
)

class PdlUtenlandskAdresssePerson(
    val bostedsadresse: List<PdlUtenlandskAdresssePersonBostedsadresse>,
)

class PdlUtenlandskAdresssePersonBostedsadresse(
    val utenlandskAdresse: UtenlandskAdresse?,
)
