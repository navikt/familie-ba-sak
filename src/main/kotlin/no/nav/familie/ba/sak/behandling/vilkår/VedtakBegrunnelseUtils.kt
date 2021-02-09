package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.vilkår.Vilkår.*

object VedtakBegrunnelseUtils {

    val vilkårBegrunnelser = mapOf<Vilkår, List<VedtakBegrunnelseSpesifikasjon>>(
            UNDER_18_ÅR to listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR),
            BOR_MED_SØKER to listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_OMSORG_FOR_BARN,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_FAST_OMSORG_FOR_BARN,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BARN_BOR_SAMMEN_MED_MOTTAKER,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_NYFØDT_BARN,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_FLYTTET_FORELDER,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_FLYTTET_BARN,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_FAST_OMSORG_FOR_BARN,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_DELT_BOSTED_ENIGHET,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_DELT_BOSTED_UENIGHET,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_FLYTTET_FRA_SØKER,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_FLYTTET_FRA_BARN,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_HAR_IKKE_FAST_OMSORG,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_DELT_BOSTED_OPPHØRT_ENIGHET,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET
            ),
            BOSATT_I_RIKET to listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_MEDLEM_I_FOLKETRYGDEN,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_UTVANDRET,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_UTVANDRET
            ),
            LOVLIG_OPPHOLD to listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE)
    )

    val ikkeStøttet = listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_MANGLENDE_OPPLYSNINGER,
                             VedtakBegrunnelseSpesifikasjon.REDUKSJON_BARN_DØD)

    val utenVilkår = listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR,
                            VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR,
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING)
}