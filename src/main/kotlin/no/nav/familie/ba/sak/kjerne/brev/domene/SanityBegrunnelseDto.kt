package no.nav.familie.ba.sak.kjerne.brev.domene

import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.BOR_MED_SOKER
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityVilkår.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle.BARN
import no.nav.familie.ba.sak.kjerne.brev.domene.VilkårRolle.SOKER
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.BrevPeriodeType
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class SanityBegrunnelseDto(
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
    val stotterFritekst: Boolean?,
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: String?,
    val endretUtbetalingsperiodeTriggere: List<String>? = emptyList(),
    val utvidetBarnetrygdTriggere: List<String>? = emptyList(),
    val valgbarhet: String? = null,
    val periodeResultatForPerson: String?,
    val fagsakType: String?,
    val regelverk: String?,
    val brevPeriodeType: String?,
    val begrunnelseTypeForPerson: String?,
    val ikkeIBruk: Boolean?,
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse? {
        if (apiNavn == null || apiNavn !in Standardbegrunnelse.entries.map { it.sanityApiNavn }) return null
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            vilkår =
                vilkaar
                    ?.mapNotNull {
                        it.finnEnumverdi<SanityVilkår>(apiNavn)
                    }?.map { it.tilVilkår() }
                    ?.toSet()
                    ?: emptySet(),
            rolle =
                rolle?.mapNotNull { it.finnEnumverdi<VilkårRolle>(apiNavn) }
                    ?: emptyList(),
            lovligOppholdTriggere =
                lovligOppholdTriggere?.mapNotNull {
                    it.finnEnumverdi<VilkårTrigger>(apiNavn)
                } ?: emptyList(),
            bosattIRiketTriggere =
                bosattIRiketTriggere?.mapNotNull {
                    it.finnEnumverdi<VilkårTrigger>(apiNavn)
                } ?: emptyList(),
            giftPartnerskapTriggere =
                giftPartnerskapTriggere?.mapNotNull {
                    it.finnEnumverdi<VilkårTrigger>(apiNavn)
                } ?: emptyList(),
            borMedSokerTriggere =
                borMedSokerTriggere?.mapNotNull {
                    it.finnEnumverdi<VilkårTrigger>(apiNavn)
                } ?: emptyList(),
            øvrigeTriggere =
                ovrigeTriggere?.mapNotNull {
                    it.finnEnumverdi<ØvrigTrigger>(apiNavn)
                } ?: emptyList(),
            endringsaarsaker =
                endringsaarsaker?.mapNotNull {
                    it.finnEnumverdi<Årsak>(apiNavn)
                } ?: emptyList(),
            hjemler = hjemler ?: emptyList(),
            hjemlerFolketrygdloven = hjemlerFolketrygdloven ?: emptyList(),
            støtterFritekst = stotterFritekst ?: false,
            endretUtbetalingsperiodeDeltBostedUtbetalingTrigger =
                endretUtbetalingsperiodeDeltBostedUtbetalingTrigger
                    .finnEnumverdiNullable<EndretUtbetalingsperiodeDeltBostedTriggere>(),
            endretUtbetalingsperiodeTriggere =
                endretUtbetalingsperiodeTriggere?.mapNotNull {
                    it.finnEnumverdi<EndretUtbetalingsperiodeTrigger>(apiNavn)
                } ?: emptyList(),
            utvidetBarnetrygdTriggere =
                utvidetBarnetrygdTriggere?.mapNotNull {
                    it.finnEnumverdi<UtvidetBarnetrygdTrigger>(apiNavn)
                } ?: emptyList(),
            valgbarhet = valgbarhet.finnEnumverdi<Valgbarhet>(apiNavn),
            periodeResultat = (periodeResultatForPerson).finnEnumverdi<SanityPeriodeResultat>(apiNavn),
            fagsakType = fagsakType.finnEnumverdiNullable<FagsakType>(),
            tema = (regelverk).finnEnumverdi<Tema>(apiNavn),
            periodeType = (brevPeriodeType).finnEnumverdi<BrevPeriodeType>(apiNavn),
            begrunnelseTypeForPerson = begrunnelseTypeForPerson.finnEnumverdi<VedtakBegrunnelseType>(apiNavn),
            ikkeIBruk = ikkeIBruk ?: false,
        )
    }
}

inline fun <reified T : Enum<T>> String?.finnEnumverdi(apiNavn: String): T? {
    val enumverdi = enumValues<T>().find { this != null && it.name == this }
    if (enumverdi == null) {
        val logger: Logger = LoggerFactory.getLogger(SanityBegrunnelseDto::class.java)
        logger.error("$this på begrunnelsen $apiNavn er ikke blant verdiene til enumen ${enumValues<T>().javaClass.simpleName}")
    }
    return enumverdi
}

inline fun <reified T : Enum<T>> String?.finnEnumverdiNullable(): T? = enumValues<T>().find { this != null && it.name == this }

enum class SanityVilkår {
    UNDER_18_ÅR,
    BOR_MED_SOKER,
    GIFT_PARTNERSKAP,
    BOSATT_I_RIKET,
    LOVLIG_OPPHOLD,
    UTVIDET_BARNETRYGD,
}

fun SanityVilkår.tilVilkår() =
    when (this) {
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
    BOSATT_PÅ_SVALBARD,
    BOSATT_I_FINNMARK_NORD_TROMS,
    DELT_BOSTED_SKAL_IKKE_DELES,
    FAST_BOSTED,
}

fun VilkårTrigger.stemmerMedVilkårsvurdering(utdypendeVilkårPåVilkårResultat: List<UtdypendeVilkårsvurdering>): Boolean =
    when (this) {
        VilkårTrigger.VURDERING_ANNET_GRUNNLAG -> utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.VURDERING_ANNET_GRUNNLAG)
        VilkårTrigger.MEDLEMSKAP -> utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP)
        VilkårTrigger.DELT_BOSTED -> utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.DELT_BOSTED)
        VilkårTrigger.DELT_BOSTED_SKAL_IKKE_DELES -> utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES)
        VilkårTrigger.BOSATT_PÅ_SVALBARD -> utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
        VilkårTrigger.BOSATT_I_FINNMARK_NORD_TROMS -> utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        VilkårTrigger.FAST_BOSTED -> !utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.DELT_BOSTED) && !utdypendeVilkårPåVilkårResultat.contains(UtdypendeVilkårsvurdering.DELT_BOSTED_SKAL_IKKE_DELES)
    }

enum class SanityPeriodeResultat {
    INNVILGET_ELLER_ØKNING,
    INGEN_ENDRING,
    IKKE_INNVILGET,
    REDUKSJON,
    IKKE_RELEVANT,
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
