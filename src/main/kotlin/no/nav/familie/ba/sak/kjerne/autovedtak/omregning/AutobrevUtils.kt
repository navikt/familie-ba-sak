package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon

object AutobrevUtils {
    fun hentStandardbegrunnelserReduksjonForAlder(alder: Int): List<VedtakBegrunnelseSpesifikasjon> =
        when (alder) {
            Alder.SEKS.år -> listOf(
                VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK,
                VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
            )
            Alder.ATTEN.år -> listOf(
                VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK,
                VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
            )
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }

    fun hentGjeldendeVedtakbegrunnelseReduksjonForAlder(alder: Int): VedtakBegrunnelseSpesifikasjon =
        when (alder) {
            Alder.SEKS.år -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR_AUTOVEDTAK
            Alder.ATTEN.år -> VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
            else -> throw Feil("Alder må være oppgitt til enten 6 eller 18 år.")
        }
}
