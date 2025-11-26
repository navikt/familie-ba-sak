package no.nav.familie.ba.sak.kjerne.autovedtak.omregning

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse

object AutobrevUtils {
    fun hentStandardbegrunnelserReduksjonForAlder(alder: Int): List<Standardbegrunnelse> =
        when (alder) {
            Alder.ATTEN.år -> {
                listOf(
                    Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK,
                    Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR,
                )
            }

            else -> {
                throw Feil("Alder må være oppgitt til 18 år.")
            }
        }

    fun hentGjeldendeVedtakbegrunnelseReduksjonForAlder(alder: Int): Standardbegrunnelse =
        when (alder) {
            Alder.ATTEN.år -> Standardbegrunnelse.REDUKSJON_UNDER_18_ÅR_AUTOVEDTAK
            else -> throw Feil("Alder må være oppgitt til 18 år.")
        }
}
