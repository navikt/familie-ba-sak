package no.nav.familie.ba.sak.kjerne.brev.domene.eøs

import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeDeltBostedTriggere
import no.nav.familie.ba.sak.kjerne.brev.domene.EndretUtbetalingsperiodeTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.finnEnumverdi
import no.nav.familie.ba.sak.kjerne.brev.domene.finnEnumverdiNullable
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.brev.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

data class SanityEØSBegrunnelseDto(
    val apiNavn: String?,
    val navnISystem: String?,
    val annenForeldersAktivitet: List<String>?,
    val barnetsBostedsland: List<String>?,
    val kompetanseResultat: List<String>?,
    val borMedSokerTriggere: List<String>? = emptyList(),
    val hjemler: List<String>?,
    val hjemlerFolketrygdloven: List<String>?,
    val hjemlerEOSForordningen883: List<String>?,
    val hjemlerEOSForordningen987: List<String>?,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>?,
    val stotterFritekst: Boolean?,
    val eosVilkaar: List<String>? = null,
    val ovrigeTriggere: List<String>? = emptyList(),
    @Deprecated("Skal bruke periodeResultatForPerson i stedet")
    val vedtakResultat: String?,
    val periodeResultatForPerson: String?,
    val fagsakType: String?,
    @Deprecated("Skal bruke regelverk i stedet")
    val tema: String?,
    val regelverk: String?,
    @Deprecated("Skal bruke periodeResultatForPerson i stedet")
    val periodeType: String?,
    val brevPeriodeType: String?,
    val begrunnelseTypeForPerson: String?,
    val valgbarhet: String?,
    val ikkeIBruk: Boolean?,
    val endringsaarsaker: List<String>? = emptyList(),
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: String?,
    val endretUtbetalingsperiodeTriggere: List<String>? = emptyList(),
) {
    fun tilSanityEØSBegrunnelse(): SanityEØSBegrunnelse? {
        if (apiNavn == null || navnISystem == null) return null
        return SanityEØSBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            annenForeldersAktivitet =
                annenForeldersAktivitet?.mapNotNull {
                    konverterTilEnumverdi<KompetanseAktivitet>(it)
                } ?: emptyList(),
            barnetsBostedsland =
                barnetsBostedsland?.mapNotNull {
                    konverterTilEnumverdi<BarnetsBostedsland>(it)
                } ?: emptyList(),
            kompetanseResultat =
                kompetanseResultat?.mapNotNull {
                    konverterTilEnumverdi<KompetanseResultat>(it)
                } ?: emptyList(),
            borMedSokerTriggere =
                borMedSokerTriggere?.mapNotNull {
                    it.finnEnumverdi<VilkårTrigger>(apiNavn)
                } ?: emptyList(),
            hjemler = hjemler ?: emptyList(),
            hjemlerFolketrygdloven = hjemlerFolketrygdloven ?: emptyList(),
            hjemlerEØSForordningen883 = hjemlerEOSForordningen883 ?: emptyList(),
            hjemlerEØSForordningen987 = hjemlerEOSForordningen987 ?: emptyList(),
            hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina ?: emptyList(),
            vilkår = eosVilkaar?.mapNotNull { konverterTilEnumverdi<Vilkår>(it) }?.toSet() ?: emptySet(),
            periodeResultat =
                (
                    periodeResultatForPerson
                        ?: vedtakResultat
                ).finnEnumverdi<SanityPeriodeResultat>(apiNavn),
            fagsakType = fagsakType.finnEnumverdiNullable<FagsakType>(),
            tema = (regelverk ?: tema).finnEnumverdi<Tema>(apiNavn),
            periodeType = (brevPeriodeType ?: periodeType).finnEnumverdi<BrevPeriodeType>(apiNavn),
            valgbarhet = valgbarhet?.finnEnumverdi<Valgbarhet>(apiNavn),
            øvrigeTriggere =
                ovrigeTriggere?.mapNotNull {
                    it.finnEnumverdi<ØvrigTrigger>(apiNavn)
                } ?: emptyList(),
            begrunnelseTypeForPerson = begrunnelseTypeForPerson.finnEnumverdi<VedtakBegrunnelseType>(apiNavn),
            endringsaarsaker =
                endringsaarsaker?.mapNotNull {
                    it.finnEnumverdi<Årsak>(apiNavn)
                } ?: emptyList(),
            endretUtbetalingsperiodeDeltBostedUtbetalingTrigger =
                endretUtbetalingsperiodeDeltBostedUtbetalingTrigger
                    .finnEnumverdiNullable<EndretUtbetalingsperiodeDeltBostedTriggere>(),
            endretUtbetalingsperiodeTriggere =
                endretUtbetalingsperiodeTriggere?.mapNotNull {
                    it.finnEnumverdi<EndretUtbetalingsperiodeTrigger>(apiNavn)
                } ?: emptyList(),
            ikkeIBruk = ikkeIBruk ?: false,
        )
    }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> = enumValues<T>().find { enum -> enum.name == it }
}

enum class BarnetsBostedsland {
    NORGE,
    IKKE_NORGE,
}

fun landkodeTilBarnetsBostedsland(landkode: String): BarnetsBostedsland =
    when (landkode) {
        "NO" -> BarnetsBostedsland.NORGE
        else -> BarnetsBostedsland.IKKE_NORGE
    }
