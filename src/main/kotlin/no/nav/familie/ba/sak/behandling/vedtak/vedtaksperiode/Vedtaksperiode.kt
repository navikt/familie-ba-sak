package no.nav.familie.ba.sak.behandling.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.common.FunksjonellFeil
import java.time.LocalDate

interface Vedtaksperiode {
    val periodeFom: LocalDate?
    val periodeTom: LocalDate?
    val vedtaksperiodetype: Vedtaksperiodetype
}

enum class Vedtaksperiodetype(val displayName: String, val støtterFritekst: Boolean) {
    UTBETALING(displayName = "utbetalingsperiode", støtterFritekst = false),
    OPPHØR(displayName = "opphørsperiode", støtterFritekst = true),
    AVSLAG(displayName = "avslagsperiode", støtterFritekst = true)
}

fun Vedtaksperiodetype.toVedtakBegrunnelseSpesifikasjon(): VedtakBegrunnelseSpesifikasjon = when (this) {
    Vedtaksperiodetype.OPPHØR -> VedtakBegrunnelseSpesifikasjon.OPPHØR_FRITEKST
    else -> throw FunksjonellFeil(melding = "Fritekstbegrunnelse er ikke støttet for ${this.displayName}")
}

