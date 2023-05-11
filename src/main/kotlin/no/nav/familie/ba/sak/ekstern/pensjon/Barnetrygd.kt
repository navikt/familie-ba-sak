package no.nav.familie.ba.sak.ekstern.pensjon

import java.time.YearMonth
import java.time.ZonedDateTime

data class BarnetrygdTilPensjonRequest(val ident: String, val fom: YearMonth)

data class BarnetrygdTilPensjonResponse(val list: List<BarnetrygdTilPensjon>)

data class BarnetrygdTilPensjon(
    val fagsakId: String,
    val tidspunktSisteVedtak: ZonedDateTime,
    val fagsakEiersIdent: String,
    val barnetrygdPerioder: List<BarnetrygdPeriode>,
    val kompetanseperioder: List<Kompetanse>? = null
)

data class BarnetrygdPeriode(
    val personIdent: String,
    val delingsprosentYtelse: Int,
    val ytelseType: YtelseType?,
    val utbetaltPerMnd: Int,
    val stønadFom: YearMonth,
    val stønadTom: YearMonth
)

data class Kompetanse(
    val barnsIdenter: List<String>,
    val fom: YearMonth,
    val tom: YearMonth?,
    val sokersaktivitet: SøkersAktivitet? = null,
    val sokersAktivitetsland: String? = null,
    val annenForeldersAktivitet: AnnenForeldersAktivitet? = null,
    val annenForeldersAktivitetsland: String? = null,
    val barnetsBostedsland: String? = null,
    val resultat: KompetanseResultat? = null
)

enum class SøkersAktivitet {
    ARBEIDER,
    SELVSTENDIG_NÆRINGSDRIVENDE,
    MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
    UTSENDT_ARBEIDSTAKER_FRA_NORGE,
    MOTTAR_UFØRETRYGD,
    MOTTAR_PENSJON,
    ARBEIDER_PÅ_NORSKREGISTRERT_SKIP,
    ARBEIDER_PÅ_NORSK_SOKKEL,
    ARBEIDER_FOR_ET_NORSK_FLYSELSKAP,
    ARBEIDER_VED_UTENLANDSK_UTENRIKSSTASJON,
    MOTTAR_UTBETALING_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET,
    MOTTAR_UFØRETRYGD_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET,
    MOTTAR_PENSJON_FRA_NAV_UNDER_OPPHOLD_I_UTLANDET,
    INAKTIV
}

enum class AnnenForeldersAktivitet {
    I_ARBEID,
    MOTTAR_UTBETALING_SOM_ERSTATTER_LØNN,
    FORSIKRET_I_BOSTEDSLAND,
    MOTTAR_PENSJON,
    INAKTIV,
    IKKE_AKTUELT,
    UTSENDT_ARBEIDSTAKER
}

enum class KompetanseResultat {
    NORGE_ER_PRIMÆRLAND,
    NORGE_ER_SEKUNDÆRLAND,
    TO_PRIMÆRLAND
}

enum class YtelseType {
    ORDINÆR_BARNETRYGD,
    UTVIDET_BARNETRYGD,
    SMÅBARNSTILLEGG,
    MANUELL_VURDERING
}
