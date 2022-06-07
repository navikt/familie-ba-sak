package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat

enum class BarnetsBostedsland {
    NORGE,
    IKKE_NORGE;
}

fun hentBarnetsBostedslandFraLandkode(landKode: String): BarnetsBostedsland {
    return if (landKode == "NO") {
        BarnetsBostedsland.NORGE
    } else {
        BarnetsBostedsland.IKKE_NORGE
    }
}

data class RestSanityEØSBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String?,
    val annenForeldersAktivitet: List<String>?,
    val barnetsBostedsland: List<String>?,
    val kompetanseResultat: List<String>?,
    val hjemlerEOSForordningen883: List<String>?,
    val hjemlerEOSForordningen987: List<String>?,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>?,
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
            hjemlerEØSForordningen883 = hjemlerEOSForordningen883 ?: emptyList(),
            hjemlerEØSForordningen987 = hjemlerEOSForordningen987 ?: emptyList(),
            hjemlerSeperasjonsavtalenStorbritannina = hjemlerSeperasjonsavtalenStorbritannina ?: emptyList(),
        )
    }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> =
        enumValues<T>().find { enum -> enum.name == it }
}

data class SanityEØSBegrunnelse(
    val apiNavn: String,
    val navnISystem: String,
    val annenForeldersAktivitet: List<AnnenForeldersAktivitet>,
    val barnetsBostedsland: List<BarnetsBostedsland>,
    val kompetanseResultat: List<KompetanseResultat>,
    val hjemlerEØSForordningen883: List<String>,
    val hjemlerEØSForordningen987: List<String>,
    val hjemlerSeperasjonsavtalenStorbritannina: List<String>,
)

fun List<SanityEØSBegrunnelse>.finnBegrunnelse(eøsBegrunnelse: EØSStandardbegrunnelse): SanityEØSBegrunnelse? =
    this.find { it.apiNavn == eøsBegrunnelse.sanityApiNavn }
