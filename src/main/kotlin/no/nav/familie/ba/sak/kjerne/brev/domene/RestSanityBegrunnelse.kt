package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.BOR_MED_SOKER
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle.BARN
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle.SOKER
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class RestSanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<String>? = emptyList(),
    val rolle: List<String>? = emptyList(),
    val lovligOppholdTriggere: List<String>? = emptyList(),
    val bosattIRiketTriggere: List<String>? = emptyList(),
    val giftPartnerskapTriggere: List<String>? = emptyList(),
    val borMedSokerTriggere: List<String>? = emptyList(),
    val ovrigeTriggere: List<String>? = emptyList(),
    val endringsaarsaker: List<String>? = emptyList(),
    val hjemler: List<String>? = emptyList(),
    val hjemlerFolketrygdloven: List<String>?,
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: String?,
    val endretUtbetalingsperiodeTriggere: List<String>? = emptyList(),
    val utvidetBarnetrygdTriggere: List<String>? = emptyList(),
    val valgbarhet: String? = null,
    val vedtakResultat: String?,
    val fagsakType: String?,
    val tema: String?,
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse? {
        if (apiNavn == null) return null
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            vilkaar = vilkaar?.mapNotNull {
                finnEnumverdi(it, SanityVilkår.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            vilkår = vilkaar?.mapNotNull {
                finnEnumverdi(it, SanityVilkår.entries.toTypedArray(), apiNavn)
            }?.map { it.tilVilkår() }?.toSet() ?: emptySet(),
            rolle = rolle?.mapNotNull { finnEnumverdi(it, VilkårRolle.entries.toTypedArray(), apiNavn) } ?: emptyList(),
            lovligOppholdTriggere = lovligOppholdTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            bosattIRiketTriggere = bosattIRiketTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            giftPartnerskapTriggere = giftPartnerskapTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            borMedSokerTriggere = borMedSokerTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            ovrigeTriggere = ovrigeTriggere?.mapNotNull {
                finnEnumverdi(it, ØvrigTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            endringsaarsaker = endringsaarsaker?.mapNotNull {
                finnEnumverdi(it, Årsak.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            hjemler = hjemler ?: emptyList(),
            hjemlerFolketrygdloven = hjemlerFolketrygdloven ?: emptyList(),
            endretUtbetalingsperiodeDeltBostedUtbetalingTrigger =
            if (endretUtbetalingsperiodeDeltBostedUtbetalingTrigger != null) {
                finnEnumverdi(
                    endretUtbetalingsperiodeDeltBostedUtbetalingTrigger,
                    EndretUtbetalingsperiodeDeltBostedTriggere.entries.toTypedArray(),
                    apiNavn,
                )
            } else {
                null
            },
            endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere?.mapNotNull {
                finnEnumverdi(it, EndretUtbetalingsperiodeTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            utvidetBarnetrygdTriggere = utvidetBarnetrygdTriggere?.mapNotNull {
                finnEnumverdi(it, UtvidetBarnetrygdTrigger.entries.toTypedArray(), apiNavn)
            } ?: emptyList(),
            valgbarhet = valgbarhet?.let { finnEnumverdi(valgbarhet, Valgbarhet.entries.toTypedArray(), apiNavn) },
            periodeResultat = vedtakResultat?.let {
                finnEnumverdi(it, SanityPeriodeResultat.entries.toTypedArray(), apiNavn)
            },
            fagsakType = fagsakType?.let {
                finnEnumverdi(it, FagsakType.entries.toTypedArray(), apiNavn)
            },
            tema = tema?.let {
                finnEnumverdi(it, Tema.entries.toTypedArray(), apiNavn)
            },

        )
    }
}

private val logger: Logger = LoggerFactory.getLogger(RestSanityBegrunnelse::class.java)

fun <T : Enum<T>> finnEnumverdi(verdi: String, enumverdier: Array<T>, apiNavn: String): T? {
    val enumverdi = enumverdier.firstOrNull { verdi == it.name }
    if (enumverdi == null) {
        logger.error(
            "$verdi på begrunnelsen $apiNavn er ikke blant verdiene til enumen ${enumverdier.javaClass.simpleName}",
        )
    }
    return enumverdi
}

enum class SanityVilkår {
    UNDER_18_ÅR,
    BOR_MED_SOKER,
    GIFT_PARTNERSKAP,
    BOSATT_I_RIKET,
    LOVLIG_OPPHOLD,
    UTVIDET_BARNETRYGD,
}

fun SanityVilkår.tilVilkår() = when (this) {
    UNDER_18_ÅR -> Vilkår.UNDER_18_ÅR
    BOR_MED_SOKER -> Vilkår.BOR_MED_SØKER
    GIFT_PARTNERSKAP -> Vilkår.GIFT_PARTNERSKAP
    BOSATT_I_RIKET -> Vilkår.BOSATT_I_RIKET
    LOVLIG_OPPHOLD -> Vilkår.LOVLIG_OPPHOLD
    UTVIDET_BARNETRYGD -> Vilkår.UTVIDET_BARNETRYGD
}

fun VilkårRolle.tilPersonType() =
    when (this) {
        SOKER -> PersonType.SØKER
        BARN -> PersonType.BARN
    }

enum class VilkårRolle {
    SOKER,
    BARN,
}

enum class VilkårTrigger {
    VURDERING_ANNET_GRUNNLAG,
    MEDLEMSKAP,
    DELT_BOSTED,
    DELT_BOSTED_SKAL_IKKE_DELES,
}

fun List<VilkårTrigger>.tilUtdypendeVilkårsvurderinger() = this.map {
    when (it) {
        VilkårTrigger.VURDERING_ANNET_GRUNNLAG -> UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG
        VilkårTrigger.MEDLEMSKAP -> UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP
        VilkårTrigger.DELT_BOSTED -> UtdypendeVilkårsvurdering.DELT_BOSTED
        VilkårTrigger.DELT_BOSTED_SKAL_IKKE_DELES -> UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES
    }
}

enum class SanityPeriodeResultat {
    INNVILGET_ELLER_ØKNING,
    INGEN_ENDRING,
    IKKE_INNVILGET,
    REDUKSJON,
}

enum class ØvrigTrigger {
    MANGLER_OPPLYSNINGER,
    SATSENDRING,
    BARN_MED_6_ÅRS_DAG,
    ALLTID_AUTOMATISK,
    ETTER_ENDRET_UTBETALING,
    ENDRET_UTBETALING,
    OPPHØR_FRA_FORRIGE_BEHANDLING,
    REDUKSJON_FRA_FORRIGE_BEHANDLING,
    BARN_DØD,

    @Deprecated("Skal erstattes med OPPHØR_FRA_FORRIGE_BEHANDLING, må endres i sanity")
    GJELDER_FØRSTE_PERIODE,

    @Deprecated("Skal erstattes med REDUKSJON_FRA_FORRIGE_BEHANDLING, må endres i sanity")
    GJELDER_FRA_INNVILGELSESTIDSPUNKT,
}

enum class EndretUtbetalingsperiodeTrigger {
    ETTER_ENDRET_UTBETALINGSPERIODE,
}

enum class EndretUtbetalingsperiodeDeltBostedTriggere {
    SKAL_UTBETALES,
    SKAL_IKKE_UTBETALES,
    UTBETALING_IKKE_RELEVANT,
}

enum class UtvidetBarnetrygdTrigger {
    SMÅBARNSTILLEGG,
}

enum class Valgbarhet {
    STANDARD,
    AUTOMATISK,
    TILLEGGSTEKST,
    SAKSPESIFIKK,
}

enum class Tema {
    NASJONAL,
    EØS,
    FELLES,
}

fun SanityBegrunnelse.inneholderVilkår(vilkår: SanityVilkår) =
    this.vilkaar.contains(vilkår)

fun SanityBegrunnelse.inneholderØvrigTrigger(øvrigTrigger: ØvrigTrigger) =
    this.ovrigeTriggere.contains(øvrigTrigger)

fun SanityBegrunnelse.inneholderLovligOppholdTrigger(vilkårTrigger: VilkårTrigger) =
    this.lovligOppholdTriggere.contains(vilkårTrigger)

fun SanityBegrunnelse.inneholderBosattIRiketTrigger(vilkårTrigger: VilkårTrigger) =
    this.bosattIRiketTriggere.contains(vilkårTrigger)

fun SanityBegrunnelse.inneholderGiftPartnerskapTrigger(vilkårTrigger: VilkårTrigger) =
    this.giftPartnerskapTriggere.contains(vilkårTrigger)

fun SanityBegrunnelse.inneholderBorMedSøkerTrigger(vilkårTrigger: VilkårTrigger) =
    this.borMedSokerTriggere.contains(vilkårTrigger)

fun SanityBegrunnelse.inneholderUtvidetBarnetrygdTrigger(utvidetBarnetrygdTrigger: UtvidetBarnetrygdTrigger) =
    this.utvidetBarnetrygdTriggere.contains(utvidetBarnetrygdTrigger)
