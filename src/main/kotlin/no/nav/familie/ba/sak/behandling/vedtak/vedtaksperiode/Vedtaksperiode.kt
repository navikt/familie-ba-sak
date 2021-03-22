package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import java.time.LocalDate

interface Vedtaksperiode {
    val periodeFom: LocalDate?
    val periodeTom: LocalDate?
    val vedtaksperiodetype: Vedtaksperiodetype
}

enum class Vedtaksperiodetype {
    UTBETALING,
    OPPHØR,
    AVSLAG
}
