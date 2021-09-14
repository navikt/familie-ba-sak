package no.nav.familie.ba.sak.kjerne.dokument.domene

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.BOR_MED_SOKER
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.GIFT_PARTNERSKAP
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.LOVLIG_OPPHOLD
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.dokument.domene.VilkårRolle.BARN
import no.nav.familie.ba.sak.kjerne.dokument.domene.VilkårRolle.SOKER
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår

data class SanityBegrunnelse(
        val apiNavn: String?,
        val navnISystem: String?,
        val begrunnelsetype: SanityBegrunnelseType? = null,
        val vilkaar: List<SanityVilkår>? = SanityVilkår.values().toList(),
        val rolle: List<VilkårRolle>? = null,
        val lovligOppholdTriggere: List<VilkårTriggere>? = null,
        val bosattIRiketTriggere: List<VilkårTriggere>? = null,
        val giftPartnerskapTriggere: List<VilkårTriggere>? = null,
        val borMedSokerTriggere: List<VilkårTriggere>? = null,
        val ovrigeTriggere: List<VilkårTriggere>? = null,
)

enum class SanityVilkår {
    UNDER_18_ÅR,
    BOR_MED_SOKER,
    GIFT_PARTNERSKAP,
    BOSATT_I_RIKET,
    LOVLIG_OPPHOLD,
}

fun SanityVilkår.tilVilkår() = when (this) {
    UNDER_18_ÅR -> Vilkår.UNDER_18_ÅR
    BOR_MED_SOKER -> Vilkår.BOR_MED_SØKER
    GIFT_PARTNERSKAP -> Vilkår.GIFT_PARTNERSKAP
    BOSATT_I_RIKET -> Vilkår.BOSATT_I_RIKET
    LOVLIG_OPPHOLD -> Vilkår.LOVLIG_OPPHOLD
}

enum class SanityBegrunnelseType {
    INNVILGELSE,
    REDUKSJON,
    AVSLAG,
    OPPHØR,
    FORTSATT_INNVILGET,
}

fun SanityBegrunnelse.hentVedtakBegrunnelseType() =
        if (this.begrunnelsetype == null) {
            throw Feil("Fikk ikke begrunnelsetype for begrunnelse ${this.navnISystem}")
        } else
            this.begrunnelsetype.tilVedtakBegrunnelseType()

private fun SanityBegrunnelseType.tilVedtakBegrunnelseType() = when (this) {
    SanityBegrunnelseType.INNVILGELSE -> VedtakBegrunnelseType.INNVILGELSE
    SanityBegrunnelseType.REDUKSJON -> VedtakBegrunnelseType.REDUKSJON
    SanityBegrunnelseType.AVSLAG -> VedtakBegrunnelseType.AVSLAG
    SanityBegrunnelseType.OPPHØR -> VedtakBegrunnelseType.OPPHØR
    SanityBegrunnelseType.FORTSATT_INNVILGET -> VedtakBegrunnelseType.FORTSATT_INNVILGET
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

enum class VilkårTriggere {
    VURDERING_ANNET_GRUNNLAG,
    MEDLEMSKAP,
    DELT_BOSTED,
    MANGLER_OPPLYSNINGER,
    SATSENDRING,
    BARN_MED_6_ÅRS_DAG,
}