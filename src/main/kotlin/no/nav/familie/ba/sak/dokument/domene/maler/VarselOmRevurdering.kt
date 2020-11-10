package no.nav.familie.ba.sak.dokument.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform


data class VarselOmRevurdering(
        val fritekst: String,
        val saksbehandler: String,
        val enhet: String,
        val aarsaker: List<String>,
        val maalform: Målform
)