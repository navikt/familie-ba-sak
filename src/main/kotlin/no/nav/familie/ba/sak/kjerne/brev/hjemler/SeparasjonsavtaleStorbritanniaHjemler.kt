package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse

fun hentSeprasjonsavtaleStorbritanniaHjemler(sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>) =
    sanityEøsBegrunnelser
        .flatMap { it.hjemlerSeperasjonsavtalenStorbritannina }
        .distinct()
