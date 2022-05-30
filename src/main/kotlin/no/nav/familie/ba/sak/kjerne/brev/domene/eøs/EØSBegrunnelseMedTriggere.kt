package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.SanityEØSBegrunnelse

data class EØSBegrunnelseMedTriggere(
    val eøsBegrunnelse: EØSStandardbegrunnelse,
    val sanityEØSBegrunnelse: SanityEØSBegrunnelse,
)
