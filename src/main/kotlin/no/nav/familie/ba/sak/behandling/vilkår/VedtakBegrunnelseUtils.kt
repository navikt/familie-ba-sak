package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.vilkår.Vilkår.*

object VedtakBegrunnelseUtils {

    val vilkårBegrunnelser = mapOf<Vilkår, List<VedtakBegrunnelser>>(
            UNDER_18_ÅR to listOf(VedtakBegrunnelser.REDUKSJON_UNDER_18_ÅR),
            BOR_MED_SØKER to listOf(
                    VedtakBegrunnelser.INNVILGET_OMSORG_FOR_BARN,
                    VedtakBegrunnelser.INNVILGET_BOR_HOS_SØKER,
                    VedtakBegrunnelser.INNVILGET_FAST_OMSORG_FOR_BARN,
                    VedtakBegrunnelser.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER,
                    VedtakBegrunnelser.INNVILGET_NYFØDT_BARN,
                    VedtakBegrunnelser.REDUKSJON_FLYTTET_FORELDER,
                    VedtakBegrunnelser.REDUKSJON_FLYTTET_BARN,
                    VedtakBegrunnelser.REDUKSJON_FAST_OMSORG_FOR_BARN,
                    VedtakBegrunnelser.REDUKSJON_DELT_BOSTED_ENIGHET,
                    VedtakBegrunnelser.REDUKSJON_DELT_BOSTED_UENIGHET,
                    VedtakBegrunnelser.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                    VedtakBegrunnelser.OPPHØR_SØKER_FLYTTET_FRA_BARN,
                    VedtakBegrunnelser.OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG,
                    VedtakBegrunnelser.OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET,
                    VedtakBegrunnelser.OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET
            ),
            BOSATT_I_RIKET to listOf(
                    VedtakBegrunnelser.INNVILGET_BOSATT_I_RIKTET,
                    VedtakBegrunnelser.INNVILGET_MEDLEM_I_FOLKETRYGDEN,
                    VedtakBegrunnelser.REDUKSJON_BOSATT_I_RIKTET,
                    VedtakBegrunnelser.OPPHØR_BARN_UTVANDRET,
                    VedtakBegrunnelser.OPPHØR_SØKER_UTVANDRET
            ),
            LOVLIG_OPPHOLD to listOf(
                    VedtakBegrunnelser.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelser.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
                    VedtakBegrunnelser.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING,
                    VedtakBegrunnelser.INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER,
                    VedtakBegrunnelser.REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN,
                    VedtakBegrunnelser.OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelser.OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE)
    )

    val ikkeStøttet = listOf(VedtakBegrunnelser.REDUKSJON_MANGLENDE_OPPLYSNINGER,
                             VedtakBegrunnelser.REDUKSJON_BARN_DØD)

    val utenVilkår = listOf(VedtakBegrunnelser.REDUKSJON_UNDER_6_ÅR,
                            VedtakBegrunnelser.REDUKSJON_UNDER_18_ÅR,
                            VedtakBegrunnelser.INNVILGET_SATSENDRING)
}