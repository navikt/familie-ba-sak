package no.nav.familie.ba.sak.kjerne.dokument.domene

import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.BOR_MED_SOKER
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.dokument.domene.VilkårRolle.BARN
import no.nav.familie.ba.sak.kjerne.dokument.domene.VilkårRolle.SOKER
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

data class SanityBegrunnelse(
    val apiNavn: String?,
    val navnISystem: String?,
    val vilkaar: List<SanityVilkår>? = null,
    val rolle: List<VilkårRolle>? = null,
    val lovligOppholdTriggere: List<VilkårTrigger>? = null,
    val bosattIRiketTriggere: List<VilkårTrigger>? = null,
    val giftPartnerskapTriggere: List<VilkårTrigger>? = null,
    val borMedSokerTriggere: List<VilkårTrigger>? = null,
    val ovrigeTriggere: List<ØvrigTrigger>? = null,
    val endringsaarsaker: List<Årsak>? = null,
    val hjemler: List<String> = emptyList(),
)

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

enum class SanityBegrunnelseType {
    INNVILGELSE,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET,
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
}

enum class ØvrigTrigger {
    MANGLER_OPPLYSNINGER,
    SATSENDRING,
    BARN_MED_6_ÅRS_DAG,
    ALLTID_AUTOMATISK,
    ETTER_ENDRET_UTBETALING,
    ENDRET_UTBETALING
}

fun SanityBegrunnelse.tilTriggesAv(): TriggesAv {

    return TriggesAv(
        vilkår = this.vilkaar?.map { it.tilVilkår() }?.toSet() ?: emptySet(),
        personTyper = this.rolle?.map { it.tilPersonType() }?.toSet()
            ?: when {
                this.inneholderVilkår(BOSATT_I_RIKET) -> setOf(PersonType.BARN, PersonType.SØKER)
                this.inneholderVilkår(LOVLIG_OPPHOLD) -> setOf(PersonType.BARN, PersonType.SØKER)
                this.inneholderVilkår(GIFT_PARTNERSKAP) -> setOf(PersonType.BARN)
                this.inneholderVilkår(UNDER_18_ÅR) -> setOf(PersonType.BARN)
                this.inneholderVilkår(BOR_MED_SOKER) -> setOf(PersonType.BARN)
                else -> setOf(PersonType.BARN, PersonType.SØKER)
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
        valgbar = !this.inneholderØvrigTrigger(ØvrigTrigger.ALLTID_AUTOMATISK),
        etterEndretUtbetaling = this.inneholderØvrigTrigger(ØvrigTrigger.ETTER_ENDRET_UTBETALING),
        endringsaarsaker = this.endringsaarsaker?.toSet() ?: emptySet(),
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
