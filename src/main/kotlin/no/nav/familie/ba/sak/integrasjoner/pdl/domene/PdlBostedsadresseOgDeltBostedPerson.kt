package no.nav.familie.ba.sak.integrasjoner.pdl.domene

import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.DeltBosted

data class PdlBostedsadresseOgDeltBostedPerson(
    val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
)
