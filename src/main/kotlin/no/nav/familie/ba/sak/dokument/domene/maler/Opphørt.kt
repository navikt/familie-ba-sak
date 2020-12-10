package no.nav.familie.ba.sak.dokument.domene.maler

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import java.util.*

data class Opphørt(
        val enhet: String,
        val saksbehandler: String,
        val beslutter: String,
        var hjemler: SortedSet<Int> = sortedSetOf(),
        var opphor: Opphør,
        val maalform: Målform,
)

data class Opphør(
        val dato: String,
        val begrunnelser: List<String>
)