package no.nav.familie.ba.sak.dataGenerator.brev

import no.nav.familie.ba.sak.common.lagTriggesAv
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

fun lagBrevBegrunnelseGrunnlagMedPersoner(
    vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
    vedtakBegrunnelseType: VedtakBegrunnelseType = VedtakBegrunnelseType.INNVILGET,
    triggesAv: TriggesAv = lagTriggesAv(),
    personIdenter: List<String> = emptyList(),
): BrevBegrunnelseGrunnlagMedPersoner {
    return BrevBegrunnelseGrunnlagMedPersoner(
        vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
        vedtakBegrunnelseType = vedtakBegrunnelseType,
        triggesAv = triggesAv,
        personIdenter = personIdenter
    )
}
