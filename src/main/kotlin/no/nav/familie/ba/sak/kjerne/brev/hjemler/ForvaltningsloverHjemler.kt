package no.nav.familie.ba.sak.kjerne.brev.hjemler

fun hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> = if (vedtakKorrigertHjemmelSkalMedIBrev) listOf("35") else emptyList()
