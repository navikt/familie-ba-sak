package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse

fun hentHjemlerForEøsForordningen987(
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): List<String> {
    val hjemler = mutableListOf<String>()

    hjemler.addAll(sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen987 })

    if (refusjonEøsHjemmelSkalMedIBrev) {
        hjemler.add("60")
    }

    return hjemler.distinct()
}
