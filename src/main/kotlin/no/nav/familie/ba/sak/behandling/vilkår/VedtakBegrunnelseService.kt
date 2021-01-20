package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.vilkår.Vilkår.*

object VedtakBegrunnelseSerivce {

    val vilkårBegrunnelser = mapOf<Vilkår, List<VedtakBegrunnelse>>(
            UNDER_18_ÅR to listOf(VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR),
            BOR_MED_SØKER to listOf(
                    VedtakBegrunnelse.INNVILGET_OMSORG_FOR_BARN,
                    VedtakBegrunnelse.INNVILGET_BOR_HOS_SØKER,
                    VedtakBegrunnelse.INNVILGET_FAST_OMSORG_FOR_BARN,
                    VedtakBegrunnelse.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER,
                    VedtakBegrunnelse.REDUKSJON_FLYTTET_FORELDER,
                    VedtakBegrunnelse.REDUKSJON_FLYTTET_BARN,
                    VedtakBegrunnelse.REDUKSJON_FAST_OMSORG_FOR_BARN,
                    VedtakBegrunnelse.REDUKSJON_DELT_BOSTED_ENIGHET,
                    VedtakBegrunnelse.REDUKSJON_DELT_BOSTED_UENIGHET,
                    VedtakBegrunnelse.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                    VedtakBegrunnelse.OPPHØR_SØKER_FLYTTET_FRA_BARN,
                    VedtakBegrunnelse.OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG,
                    VedtakBegrunnelse.OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET,
                    VedtakBegrunnelse.OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET
            ),
            BOSATT_I_RIKET to listOf(
                    VedtakBegrunnelse.INNVILGET_BOSATT_I_RIKTET,
                    VedtakBegrunnelse.INNVILGET_MEDLEM_I_FOLKETRYGDEN,
                    VedtakBegrunnelse.REDUKSJON_BOSATT_I_RIKTET,
                    VedtakBegrunnelse.OPPHØR_BARN_UTVANDRET,
                    VedtakBegrunnelse.OPPHØR_SØKER_UTVANDRET
            ),
            LOVLIG_OPPHOLD to listOf(
                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING,
                    VedtakBegrunnelse.INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER,
                    VedtakBegrunnelse.REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN,
                    VedtakBegrunnelse.OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelse.OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE)
    )

    val ikkeStøttet = listOf(VedtakBegrunnelse.REDUKSJON_MANGLENDE_OPPLYSNINGER,
                             VedtakBegrunnelse.REDUKSJON_BARN_DØD)

    val utenVilkår = listOf(VedtakBegrunnelse.REDUKSJON_UNDER_6_ÅR,
                            VedtakBegrunnelse.REDUKSJON_UNDER_18_ÅR,
                            VedtakBegrunnelse.INNVILGET_NYFØDT_BARN,
                            VedtakBegrunnelse.INNVILGET_SATSENDRING)
}