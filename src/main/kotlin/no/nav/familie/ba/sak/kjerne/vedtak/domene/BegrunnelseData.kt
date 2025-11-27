package no.nav.familie.ba.sak.kjerne.vedtak.domene

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType

sealed interface BrevBegrunnelse : Comparable<BrevBegrunnelse> {
    val type: Begrunnelsetype
    val vedtakBegrunnelseType: VedtakBegrunnelseType?

    override fun compareTo(other: BrevBegrunnelse): Int =
        when {
            this.type == Begrunnelsetype.FRITEKST -> Int.MAX_VALUE
            other.type == Begrunnelsetype.FRITEKST -> Int.MIN_VALUE
            this.vedtakBegrunnelseType == null -> Int.MAX_VALUE
            other.vedtakBegrunnelseType == null -> Int.MIN_VALUE
            erFinnmarkEllerSvalbardBegrunnelse(this) && !erFinnmarkEllerSvalbardBegrunnelse(other) -> Int.MAX_VALUE
            !erFinnmarkEllerSvalbardBegrunnelse(this) && erFinnmarkEllerSvalbardBegrunnelse(other) -> Int.MIN_VALUE
            else -> this.vedtakBegrunnelseType!!.sorteringsrekkefølge - other.vedtakBegrunnelseType!!.sorteringsrekkefølge
        }
}

enum class Begrunnelsetype {
    STANDARD_BEGRUNNELSE,
    EØS_BEGRUNNELSE,
    FRITEKST,
}

private fun erFinnmarkEllerSvalbardBegrunnelse(begrunnelse: BrevBegrunnelse): Boolean =
    (begrunnelse is BegrunnelseMedData && begrunnelse.apiNavn.lowercase().contains("finnmarkstillegg")) ||
        (begrunnelse is BegrunnelseMedData && begrunnelse.apiNavn.lowercase().contains("svalbardtillegg"))

interface BegrunnelseMedData : BrevBegrunnelse {
    val apiNavn: String
}

data class BegrunnelseData(
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,
    val gjelderSoker: Boolean,
    val barnasFodselsdatoer: String,
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val fodselsdatoerBarnOppfyllerTriggereOgHarUtbetaling: String = "",
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val fodselsdatoerBarnOppfyllerTriggereOgHarNullutbetaling: String = "",
    val antallBarn: Int,
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val antallBarnOppfyllerTriggereOgHarUtbetaling: Int = 0,
    @Deprecated("Brukes ikke. Kan slettes når vi har fjernet gammel begrunnelseskode.")
    val antallBarnOppfyllerTriggereOgHarNullutbetaling: Int = 0,
    val maanedOgAarBegrunnelsenGjelderFor: String?,
    val maalform: String,
    val belop: String,
    val soknadstidspunkt: String,
    val avtaletidspunktDeltBosted: String,
    val sokersRettTilUtvidet: String,
) : BegrunnelseMedData {
    override val type: Begrunnelsetype = Begrunnelsetype.STANDARD_BEGRUNNELSE
}

data class FritekstBegrunnelse(
    val fritekst: String,
) : BrevBegrunnelse {
    override val vedtakBegrunnelseType: VedtakBegrunnelseType? = null
    override val type: Begrunnelsetype = Begrunnelsetype.FRITEKST
}

sealed interface EØSBegrunnelseData : BegrunnelseMedData {
    val barnasFodselsdatoer: String
    val antallBarn: Int
    val maalform: String
    val gjelderSoker: Boolean
}

data class EØSBegrunnelseDataMedKompetanse(
    override val type: Begrunnelsetype = Begrunnelsetype.EØS_BEGRUNNELSE,
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,
    override val barnasFodselsdatoer: String,
    override val antallBarn: Int,
    override val maalform: String,
    override val gjelderSoker: Boolean,
    val erAnnenForelderOmfattetAvNorskLovgivning: Boolean,
    val annenForeldersAktivitet: KompetanseAktivitet,
    val annenForeldersAktivitetsland: String?,
    val barnetsBostedsland: String,
    val sokersAktivitet: KompetanseAktivitet,
    val sokersAktivitetsland: String?,
) : EØSBegrunnelseData

data class EØSBegrunnelseDataUtenKompetanse(
    override val type: Begrunnelsetype = Begrunnelsetype.EØS_BEGRUNNELSE,
    override val vedtakBegrunnelseType: VedtakBegrunnelseType,
    override val apiNavn: String,
    override val barnasFodselsdatoer: String,
    override val antallBarn: Int,
    override val maalform: String,
    override val gjelderSoker: Boolean,
) : EØSBegrunnelseData
