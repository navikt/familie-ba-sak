package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.BrevBegrunnelseGrunnlagMedPersoner
import no.nav.familie.ba.sak.kjerne.brev.domene.RestBehandlingsgrunnlagForBrev

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vedtakBegrunnelseType")
@JsonSubTypes(
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "INNVILGET"),
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "REDUKSJON"),
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "AVSLAG"),
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "OPPHØR"),
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "FORTSATT_INNVILGET"),
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "ENDRET_UTBETALING"),
    JsonSubTypes.Type(value = Standardbegrunnelse::class, name = "ETTER_ENDRET_UTBETALING"),
    JsonSubTypes.Type(value = EØSStandardbegrunnelse::class, name = "EØS_INNVILGET"),
    JsonSubTypes.Type(value = EØSStandardbegrunnelse::class, name = "EØS_AVSLAG"),
    JsonSubTypes.Type(value = EØSStandardbegrunnelse::class, name = "EØS_OPPHØR"),
)
interface IVedtakBegrunnelse {

    val sanityApiNavn: String
    val vedtakBegrunnelseType: VedtakBegrunnelseType
    val kanDelesOpp: Boolean

    fun delOpp(
        restBehandlingsgrunnlagForBrev: RestBehandlingsgrunnlagForBrev,
        triggesAv: TriggesAv,
        periode: NullablePeriode
    ): List<BrevBegrunnelseGrunnlagMedPersoner>

    fun enumnavnTilString(): String
}
