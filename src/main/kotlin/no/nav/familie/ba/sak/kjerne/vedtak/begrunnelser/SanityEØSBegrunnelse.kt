package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
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
) {
    fun tilSanityEØSBegrunnelse(): SanityEØSBegrunnelse? {
        if (apiNavn == null || navnISystem == null) return null
        return SanityEØSBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            annenForeldersAktivitet = annenForeldersAktivitet?.mapNotNull {
                konverterTilEnumverdi<KompetanseAktivitet>(it)
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
            vilkår = eosVilkaar?.mapNotNull { konverterTilEnumverdi<Vilkår>(it) } ?: emptyList(),
        )
    }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> =
        enumValues<T>().find { enum -> enum.name == it }
}

data class SanityEØSBegrunnelse(
    override val apiNavn: String,
    override val navnISystem: String,
    val annenForeldersAktivitet: List<KompetanseAktivitet>,
    val barnetsBostedsland: List<BarnetsBostedsland>,
    val kompetanseResultat: List<KompetanseResultat>,
    val hjemler: List<String>,
    val hjemlerFolketrygdloven: List<String>,
    val hjemlerEØSForordningen883: List<String>,
    val hjemlerEØSForordningen987: List<String>,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>,
    val vilkår: List<Vilkår>,
) : ISanityBegrunnelse
