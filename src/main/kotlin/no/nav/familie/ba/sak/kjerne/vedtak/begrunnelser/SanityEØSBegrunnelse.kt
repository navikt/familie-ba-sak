package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

enum class BarnetsBostedsland {
    NORGE,
    IKKE_NORGE;
}

fun landkodeTilBarnetsBostedsland(landkode: String): BarnetsBostedsland = when (landkode) {
    "NO" -> BarnetsBostedsland.NORGE
    else -> BarnetsBostedsland.IKKE_NORGE
}

data class RestSanityEØSBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String?,
    val triggereIBruk: List<String>?,
    val annenForeldersAktivitet: List<String>?,
    val barnetsBostedsland: List<String>?,
    val kompetanseResultat: List<String>?,
    val eosVilkaar: List<String>?,
    val utdypendeVilkaarsvurderinger: List<String>?,
    val hjemler: List<String>?,
    val hjemlerFolketrygdloven: List<String>?,
    val hjemlerEOSForordningen883: List<String>?,
    val hjemlerEOSForordningen987: List<String>?,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>?
) {
    fun tilSanityEØSBegrunnelse(): SanityEØSBegrunnelse? {
        if (apiNavn == null || navnISystem == null) return null
        return SanityEØSBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            annenForeldersAktivitet = annenForeldersAktivitet?.tilEnumListe() ?: emptyList(),
            triggereIBruk = triggereIBruk?.tilEnumListe() ?: emptyList(),
            barnetsBostedsland = barnetsBostedsland?.tilEnumListe() ?: emptyList(),
            kompetanseResultat = kompetanseResultat?.tilEnumListe() ?: emptyList(),
            vilkår = eosVilkaar?.tilEnumListe() ?: emptyList(),
            utdypendeVilkårsvurderinger = utdypendeVilkaarsvurderinger?.tilEnumListe() ?: emptyList(),
            hjemler = hjemler ?: emptyList(),
            hjemlerFolketrygdloven = hjemlerFolketrygdloven ?: emptyList(),
            hjemlerEØSForordningen883 = hjemlerEOSForordningen883 ?: emptyList(),
            hjemlerEØSForordningen987 = hjemlerEOSForordningen987 ?: emptyList(),
            hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina ?: emptyList()
        )
    }

    private inline fun <reified T> List<String>.tilEnumListe(): List<T> where T : Enum<T> =
        this.mapNotNull { konverterTilEnumverdi<T>(it) }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> =
        enumValues<T>().find { enum -> enum.name == it }
}

data class SanityEØSBegrunnelse(
    val apiNavn: String,
    val navnISystem: String,
    val triggereIBruk: List<EØSTriggerType>,
    val annenForeldersAktivitet: List<AnnenForeldersAktivitet>,
    val barnetsBostedsland: List<BarnetsBostedsland>,
    val kompetanseResultat: List<KompetanseResultat>,
    val vilkår: List<Vilkår>,
    val utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering>,
    val hjemler: List<String>,
    val hjemlerFolketrygdloven: List<String>,
    val hjemlerEØSForordningen883: List<String>,
    val hjemlerEØSForordningen987: List<String>,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>
) {
    fun skalBrukeKompetanseData() = this.triggereIBruk.contains(EØSTriggerType.KOMPETANSE)
    fun skalBrukeVilkårData() = this.triggereIBruk.contains(EØSTriggerType.VILKÅRSVURDERING)
}

fun List<SanityEØSBegrunnelse>.finnBegrunnelse(eøsBegrunnelse: EØSStandardbegrunnelse): SanityEØSBegrunnelse? =
    this.find { it.apiNavn == eøsBegrunnelse.sanityApiNavn }

enum class EØSTriggerType {
    KOMPETANSE,
    VILKÅRSVURDERING,
}