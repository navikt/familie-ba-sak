package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hjemlerTilhørendeFritekst

fun hentOrdinæreHjemler(
    sanityBegrunnelser: List<SanityBegrunnelse>,
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean,
    finnesVedtaksperiodeMedFritekst: Boolean,
): List<String> {
    val hjemler = (sanityBegrunnelser.flatMap { it.hjemler } + sanityEøsBegrunnelser.flatMap { it.hjemler }).toMutableSet()

    if (opplysningspliktHjemlerSkalMedIBrev) {
        val hjemlerNårOpplysningspliktIkkeOppfylt = listOf("17", "18")
        hjemler.addAll(hjemlerNårOpplysningspliktIkkeOppfylt)
    }

    if (finnesVedtaksperiodeMedFritekst) {
        hjemler.addAll(hjemlerTilhørendeFritekst.map { it.toString() }.toSet())
    }

    return hjemler
        .map { it.toInt() }
        .sorted()
        .map { it.toString() }
        .distinct()
}
