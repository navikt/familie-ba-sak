package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.BostedsadresserOgDelteBosteder
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.tilAdresse
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted

data class PdlBostedsadresseDeltBostedOppholdsadressePerson(
    val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
)

fun PdlBostedsadresseDeltBostedOppholdsadressePerson?.tilAdresser(): BostedsadresserOgDelteBosteder =
    BostedsadresserOgDelteBosteder(
        bostedsadresser = this?.let { bostedsadresse.map { it.tilAdresse() } } ?: emptyList(),
        delteBosteder = this?.let { deltBosted.map { it.tilAdresse() } } ?: emptyList(),
    )
