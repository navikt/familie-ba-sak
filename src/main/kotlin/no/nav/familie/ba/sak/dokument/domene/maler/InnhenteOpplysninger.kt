package no.nav.familie.ba.sak.dokument.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform


data class InnhenteOpplysninger(
        val saksbehandler: String,
        val enhet: String,
        val dokumenter: List<String>,
        val maalform: Målform
)