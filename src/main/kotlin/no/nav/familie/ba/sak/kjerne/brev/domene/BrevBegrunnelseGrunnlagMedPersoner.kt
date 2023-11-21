package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import java.time.LocalDate

data class BrevBegrunnelseGrunnlagMedPersoner(
    val standardbegrunnelse: IVedtakBegrunnelse,
    val vedtakBegrunnelseType: VedtakBegrunnelseType,
    val personIdenter: List<String>,
    val avtaletidspunktDeltBosted: LocalDate? = null,
)