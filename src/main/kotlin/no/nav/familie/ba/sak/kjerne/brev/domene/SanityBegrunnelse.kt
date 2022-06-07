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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.slf4j.Logger
import org.slf4j.LoggerFactory

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String,
    val vilkaar: List<SanityVilkår>? = null,
    val rolle: List<VilkårRolle> = emptyList(),
    val lovligOppholdTriggere: List<VilkårTrigger>? = null,
    val bosattIRiketTriggere: List<VilkårTrigger>? = null,
    val giftPartnerskapTriggere: List<VilkårTrigger>? = null,
    val borMedSokerTriggere: List<VilkårTrigger>? = null,
    val ovrigeTriggere: List<ØvrigTrigger>? = null,
    val endringsaarsaker: List<Årsak>? = null,
    val hjemler: List<String> = emptyList(),
    val hjemlerFolketrygdloven: List<String> = emptyList(),
    val endretUtbetalingsperiodeDeltBostedUtbetalingTrigger: EndretUtbetalingsperiodeDeltBostedTriggere? = null,
    val endretUtbetalingsperiodeTriggere: List<EndretUtbetalingsperiodeTrigger>? = null,
    val utvidetBarnetrygdTriggere: List<UtvidetBarnetrygdTrigger>? = null,
)

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
) {
    fun tilSanityBegrunnelse(): SanityBegrunnelse {
        return SanityBegrunnelse(
            apiNavn = apiNavn,
            navnISystem = navnISystem,
            vilkaar = vilkaar?.mapNotNull {
                finnEnumverdi(it, SanityVilkår.values(), apiNavn)
            },
            rolle = rolle?.mapNotNull { finnEnumverdi(it, VilkårRolle.values(), apiNavn) } ?: emptyList(),
            lovligOppholdTriggere = lovligOppholdTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.values(), apiNavn)
            },
            bosattIRiketTriggere = bosattIRiketTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.values(), apiNavn)
            },
            giftPartnerskapTriggere = giftPartnerskapTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.values(), apiNavn)
            },
            borMedSokerTriggere = borMedSokerTriggere?.mapNotNull {
                finnEnumverdi(it, VilkårTrigger.values(), apiNavn)
            },
            ovrigeTriggere = ovrigeTriggere?.mapNotNull {
                finnEnumverdi(it, ØvrigTrigger.values(), apiNavn)
            },
            endringsaarsaker = endringsaarsaker?.mapNotNull {
                finnEnumverdi(it, Årsak.values(), apiNavn)
            },
            hjemler = hjemler ?: emptyList(),
            hjemlerFolketrygdloven = hjemlerFolketrygdloven ?: emptyList(),
            endretUtbetalingsperiodeDeltBostedUtbetalingTrigger =
            if (endretUtbetalingsperiodeDeltBostedUtbetalingTrigger != null) finnEnumverdi(endretUtbetalingsperiodeDeltBostedUtbetalingTrigger, EndretUtbetalingsperiodeDeltBostedTriggere.values(), apiNavn) else null,
            endretUtbetalingsperiodeTriggere = endretUtbetalingsperiodeTriggere?.mapNotNull {
                finnEnumverdi(it, EndretUtbetalingsperiodeTrigger.values(), apiNavn)
            },
            utvidetBarnetrygdTriggere = utvidetBarnetrygdTriggere?.mapNotNull {
                finnEnumverdi(it, UtvidetBarnetrygdTrigger.values(), apiNavn)
            },
        )
    }
}

private val logger: Logger = LoggerFactory.getLogger(RestSanityBegrunnelse::class.java)

fun <T : Enum<T>> finnEnumverdi(verdi: String, enumverdier: Array<T>, apiNavn: String?): T? {
    val enumverdi = enumverdier.firstOrNull { verdi == it.name }
    if (enumverdi == null) {
        logger.error(
            "$verdi på begrunnelsen $apiNavn er ikke blant verdiene til enumen ${enumverdier.javaClass.simpleName}"
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
    UTVIDET_BARNETRYGD
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
    DELT_BOSTED_SKAL_IKKE_DELES
}

enum class ØvrigTrigger {
    MANGLER_OPPLYSNINGER,
    SATSENDRING,
    BARN_MED_6_ÅRS_DAG,
    ALLTID_AUTOMATISK,
    ETTER_ENDRET_UTBETALING,
    ENDRET_UTBETALING,
    GJELDER_FØRSTE_PERIODE,
    GJELDER_FRA_INNVILGELSESTIDSPUNKT,
}

enum class EndretUtbetalingsperiodeTrigger {
    ETTER_ENDRET_UTBETALINGSPERIODE,
}

enum class EndretUtbetalingsperiodeDeltBostedTriggere {
    SKAL_UTBETALES,
    SKAL_IKKE_UTBETALES,
    UTBETALING_IKKE_RELEVANT
}

enum class UtvidetBarnetrygdTrigger {
    SMÅBARNSTILLEGG,
}

fun SanityBegrunnelse.tilTriggesAv(): TriggesAv {

    return TriggesAv(
        vilkår = this.vilkaar?.map { it.tilVilkår() }?.toSet() ?: emptySet(),
        personTyper = if (this.rolle.isEmpty()) {
            when {
                this.inneholderVilkår(BOSATT_I_RIKET) -> setOf(PersonType.BARN, PersonType.SØKER)
                this.inneholderVilkår(LOVLIG_OPPHOLD) -> setOf(PersonType.BARN, PersonType.SØKER)
                this.inneholderVilkår(GIFT_PARTNERSKAP) -> setOf(PersonType.BARN)
                this.inneholderVilkår(UNDER_18_ÅR) -> setOf(PersonType.BARN)
                this.inneholderVilkår(BOR_MED_SOKER) -> setOf(PersonType.BARN)
                else -> setOf(PersonType.BARN, PersonType.SØKER)
            }
        } else {
            this.rolle.map { it.tilPersonType() }.toSet()
        },
        personerManglerOpplysninger = this.inneholderØvrigTrigger(ØvrigTrigger.MANGLER_OPPLYSNINGER),
        satsendring = this.inneholderØvrigTrigger(ØvrigTrigger.SATSENDRING),
        barnMedSeksårsdag = this.inneholderØvrigTrigger(ØvrigTrigger.BARN_MED_6_ÅRS_DAG),
        vurderingAnnetGrunnlag = (
            this.inneholderLovligOppholdTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderBosattIRiketTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderGiftPartnerskapTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG) ||
                this.inneholderBorMedSøkerTrigger(VilkårTrigger.VURDERING_ANNET_GRUNNLAG)
            ),
        medlemskap = this.inneholderBosattIRiketTrigger(VilkårTrigger.MEDLEMSKAP),
        deltbosted = this.inneholderBorMedSøkerTrigger(VilkårTrigger.DELT_BOSTED),
        deltBostedSkalIkkeDeles = this.inneholderBorMedSøkerTrigger(VilkårTrigger.DELT_BOSTED_SKAL_IKKE_DELES),
        valgbar = !this.inneholderØvrigTrigger(ØvrigTrigger.ALLTID_AUTOMATISK),
        etterEndretUtbetaling = this.endretUtbetalingsperiodeTriggere
            ?.contains(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE) ?: false,
        endretUtbetalingSkalUtbetales = this.endretUtbetalingsperiodeDeltBostedUtbetalingTrigger ?: EndretUtbetalingsperiodeDeltBostedTriggere.UTBETALING_IKKE_RELEVANT,
        endringsaarsaker = this.endringsaarsaker?.toSet() ?: emptySet(),
        småbarnstillegg = this.inneholderUtvidetBarnetrygdTrigger(UtvidetBarnetrygdTrigger.SMÅBARNSTILLEGG),
        gjelderFørstePeriode = this.inneholderØvrigTrigger(ØvrigTrigger.GJELDER_FØRSTE_PERIODE),
        gjelderFraInnvilgelsestidspunkt = this.inneholderØvrigTrigger(ØvrigTrigger.GJELDER_FRA_INNVILGELSESTIDSPUNKT)
    )
}

fun SanityBegrunnelse.inneholderVilkår(vilkår: SanityVilkår) =
    this.vilkaar?.contains(vilkår) ?: false

fun SanityBegrunnelse.inneholderØvrigTrigger(øvrigTrigger: ØvrigTrigger) =
    this.ovrigeTriggere?.contains(øvrigTrigger) ?: false

fun SanityBegrunnelse.inneholderLovligOppholdTrigger(vilkårTrigger: VilkårTrigger) =
    this.lovligOppholdTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderBosattIRiketTrigger(vilkårTrigger: VilkårTrigger) =
    this.bosattIRiketTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderGiftPartnerskapTrigger(vilkårTrigger: VilkårTrigger) =
    this.giftPartnerskapTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderBorMedSøkerTrigger(vilkårTrigger: VilkårTrigger) =
    this.borMedSokerTriggere?.contains(vilkårTrigger) ?: false

fun SanityBegrunnelse.inneholderUtvidetBarnetrygdTrigger(utvidetBarnetrygdTrigger: UtvidetBarnetrygdTrigger) =
    this.utvidetBarnetrygdTriggere?.contains(utvidetBarnetrygdTrigger) ?: false
