package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import java.time.LocalDate

data class Opphørsperiode(
        override val periodeFom: LocalDate,
        override val periodeTom: LocalDate,
        override val vedtaksperiodetype: Vedtaksperiodetype = Vedtaksperiodetype.OPPHØR
) : Vedtaksperiode

