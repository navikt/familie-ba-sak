package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat

enum class BarnetsBostedsland {
    NORGE;

    fun tilLandkode(): String {
        return when (this) {
            NORGE -> "NO"
        }
    }
}

data class RestSanityEØSBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String?,
    val annenForeldersAktivitet: List<AnnenForeldersAktivitet>? = emptyList(),
    val barnetsBostedsland: List<BarnetsBostedsland>? = emptyList(),
    val kompetanseResultat: List<KompetanseResultat>? = emptyList(),
) {
    fun tilSanityEØSBegrunnelse(): SanityEØSBegrunnelse? {
        if (apiNavn == null || navnISystem == null) return null

        return SanityEØSBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            annenForeldersAktivitet = annenForeldersAktivitet ?: emptyList(),
            barnetsBostedsland = barnetsBostedsland ?: emptyList(),
            kompetanseResultat = kompetanseResultat ?: emptyList()
        )
    }
}

data class SanityEØSBegrunnelse(
    val apiNavn: String,
    val navnISystem: String,
    val annenForeldersAktivitet: List<AnnenForeldersAktivitet>,
    val barnetsBostedsland: List<BarnetsBostedsland>,
    val kompetanseResultat: List<KompetanseResultat>,
)

fun List<SanityEØSBegrunnelse>.finnBegrunnelse(eøsBegrunnelse: EØSStandardbegrunnelse): SanityEØSBegrunnelse? =
    this.find { it.apiNavn == eøsBegrunnelse.sanityApiNavn }
