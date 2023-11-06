package no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityPeriodeResultat
import no.nav.familie.ba.sak.kjerne.brev.domene.Tema
import no.nav.familie.ba.sak.kjerne.brev.domene.Valgbarhet
import no.nav.familie.ba.sak.kjerne.brev.domene.finnEnumverdi
import no.nav.familie.ba.sak.kjerne.brev.domene.finnEnumverdiNullable
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseAktivitet
import no.nav.familie.ba.sak.kjerne.eøs.kompetanse.domene.KompetanseResultat
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
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
            vilkår = eosVilkaar?.mapNotNull { konverterTilEnumverdi<Vilkår>(it) }?.toSet() ?: emptySet(),
            periodeResultat = (
                periodeResultatForPerson
                    ?: vedtakResultat
                ).finnEnumverdi<SanityPeriodeResultat>(apiNavn),
            fagsakType = fagsakType.finnEnumverdiNullable<FagsakType>(),
            tema = (regelverk ?: tema).finnEnumverdi<Tema>(apiNavn),
            periodeType = (brevPeriodeType ?: periodeType).finnEnumverdi<BrevPeriodeType>(apiNavn),
            valgbarhet = valgbarhet?.finnEnumverdi<Valgbarhet>(apiNavn),
        )
    }

    private inline fun <reified T> konverterTilEnumverdi(it: String): T? where T : Enum<T> =
        enumValues<T>().find { enum -> enum.name == it }
}
