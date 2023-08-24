package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVedtakResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårTrigger
import no.nav.familie.ba.sak.kjerne.brev.domene.finnEnumverdi
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class BarnetsBostedsland {
    NORGE,
    IKKE_NORGE,
}

fun landkodeTilBarnetsBostedsland(landkode: String): BarnetsBostedsland = when (landkode) {
    "NO" -> BarnetsBostedsland.NORGE
    else -> BarnetsBostedsland.IKKE_NORGE
}

data class RestSanityEØSBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String?,
    val annenForeldersAktivitet: List<String>?,
    val barnetsBostedsland: List<String>?,
    val kompetanseResultat: List<String>?,
    val hjemler: List<String>?,
    val hjemlerFolketrygdloven: List<String>?,
    val hjemlerEOSForordningen883: List<String>?,
    val hjemlerEOSForordningen987: List<String>?,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>?,
    val eosVilkaar: List<String>? = null,
    val vedtakResultat: String?,
) {
    fun tilSanityEØSBegrunnelse(): SanityEØSBegrunnelse? {
        if (apiNavn == null || navnISystem == null) return null
        return SanityEØSBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            annenForeldersAktivitet = annenForeldersAktivitet?.mapNotNull {
                konverterTilEnumverdi<AnnenForeldersAktivitet>(it)
            } ?: emptyList(),
            barnetsBostedsland = barnetsBostedsland?.mapNotNull {
                konverterTilEnumverdi<BarnetsBostedsland>(it)
            } ?: emptyList(),
            kompetanseResultat = kompetanseResultat?.mapNotNull {
                konverterTilEnumverdi<KompetanseResultat>(it)
            } ?: emptyList(),
            hjemler = hjemler ?: emptyList(),
            hjemlerFolketrygdloven = hjemlerFolketrygdloven ?: emptyList(),
            hjemlerEØSForordningen883 = hjemlerEOSForordningen883 ?: emptyList(),
            hjemlerEØSForordningen987 = hjemlerEOSForordningen987 ?: emptyList(),
            hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina ?: emptyList(),
            vilkår = eosVilkaar?.mapNotNull { konverterTilEnumverdi<Vilkår>(it) }?.toSet() ?: emptySet(),
            vedtakResultat = vedtakResultat?.let {
                finnEnumverdi(it, SanityVedtakResultat.entries.toTypedArray(), apiNavn)
            },
        )
    }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> =
        enumValues<T>().find { enum -> enum.name == it }
}

data class SanityEØSBegrunnelse(
    override val apiNavn: String,
    override val navnISystem: String,
    override val vedtakResultat: SanityVedtakResultat? = null,
    override val vilkår: Set<Vilkår>,
    val annenForeldersAktivitet: List<AnnenForeldersAktivitet>,
    val barnetsBostedsland: List<BarnetsBostedsland>,
    val kompetanseResultat: List<KompetanseResultat>,
    val hjemler: List<String>,
    val hjemlerFolketrygdloven: List<String>,
    val hjemlerEØSForordningen883: List<String>,
    val hjemlerEØSForordningen987: List<String>,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>,
) : ISanityBegrunnelse {
    override val lovligOppholdTriggere: List<VilkårTrigger> = emptyList()
    override val bosattIRiketTriggere: List<VilkårTrigger> = emptyList()
    override val giftPartnerskapTriggere: List<VilkårTrigger> = emptyList()
    override val borMedSokerTriggere: List<VilkårTrigger> = emptyList()
}
