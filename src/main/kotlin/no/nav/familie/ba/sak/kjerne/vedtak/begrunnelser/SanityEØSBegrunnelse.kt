package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.AnnenForeldersAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat

enum class BarnetsBostedsland {
    Norge
}

data class SanityEØSBegrunnelse(
    val apiNavn: String,
    val navnISystem: String,
    val annenForeldersAktivitet: List<AnnenForeldersAktivitet> = emptyList(),
    val barnetsBostedsland: List<BarnetsBostedsland> = emptyList(),
    val kompetanseResultat: List<KompetanseResultat> = emptyList(),
)

fun List<SanityEØSBegrunnelse>.finnBegrunnelse(eøsBegrunnelse: EØSStandardbegrunnelse): SanityEØSBegrunnelse? =
    this.find { it.apiNavn == eøsBegrunnelse.sanityApiNavn }
