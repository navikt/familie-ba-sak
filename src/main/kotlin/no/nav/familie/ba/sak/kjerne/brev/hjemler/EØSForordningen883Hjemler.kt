package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse

fun hentEØSForordningen883Hjemler(
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
) = sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen883 }.distinct()
