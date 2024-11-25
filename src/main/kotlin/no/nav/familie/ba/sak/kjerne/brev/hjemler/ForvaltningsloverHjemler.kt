package no.nav.familie.ba.sak.kjerne.brev.hjemler

fun utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev: Boolean): List<String> =
    if (vedtakKorrigertHjemmelSkalMedIBrev) {
        listOf("35")
    } else {
        emptyList()
    }
