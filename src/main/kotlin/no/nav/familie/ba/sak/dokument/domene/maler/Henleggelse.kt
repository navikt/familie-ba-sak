package no.nav.familie.ba.sak.dokument.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform


data class Henleggelse(
        val saksbehandler: String,
        val enhet: String,
        val maalform: Målform
)