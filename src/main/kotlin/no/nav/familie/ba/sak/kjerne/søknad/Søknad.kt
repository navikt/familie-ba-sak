package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform

// TODO: Definer alle felter vi trenger. Dette er bare et eksempel.
data class Søknad(
    val søker: Søker,
    val barn: List<Barn>,
    val behandlingKategori: BehandlingKategori,
    val behandlingUnderkategori: BehandlingUnderkategori,
    val målform: Målform = Målform.NB,
)

data class Barn(
    val fnr: String,
    val planleggerÅBoINorge12Mnd: Boolean,
)

data class Søker(
    val fnr: String,
    val planleggerÅBoINorge12Mnd: Boolean,
)
