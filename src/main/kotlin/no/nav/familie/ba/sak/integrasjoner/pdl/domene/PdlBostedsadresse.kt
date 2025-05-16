package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import java.math.BigInteger
import java.time.LocalDateTime

data class PdlBostedsadresseResponse(
    val person: PdlBostedsadressePerson?,
)

data class PdlBostedsadressePerson(
    val bostedsadresse: List<PdlBostedsadresse>,
)

data class PdlBostedsadresse(
    val gyldigFraOgMed: LocalDateTime,
    val gyldigTilOgMed: LocalDateTime?,
    val vegadresse: PdlBostedsVegadresse?,
    val matrikkeladresse: PdlMatrikkeladresse?,
    val ukjentBosted: PdlUkjentBosted?,
)

data class PdlBostedsVegadresse(
    val matrikkelId: BigInteger,
)

data class PdlMatrikkeladresse(
    val matrikkelId: BigInteger,
)

data class PdlUkjentBosted(
    val bostedskommune: String,
)
