package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.vilkår.Vilkår.*

object VedtakBegrunnelseUtils {

    val vilkårMedVedtakBegrunnelser = mapOf<Vilkår, List<VedtakBegrunnelseSpesifikasjon>>(
            UNDER_18_ÅR to listOf(
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_UNDER_18_ÅR,
            ),
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
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_DELT_BOSTED_OPPHØRT_UENIGHET,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_BOR_HOS_SØKER,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_OMSORG_FOR_BARN,
            ),
            BOSATT_I_RIKET to listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_MEDLEM_I_FOLKETRYGDEN,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_UTVANDRET,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_UTVANDRET,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_MEDLEM_I_FOLKETRYGDEN,
            ),
            LOVLIG_OPPHOLD to listOf(
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_EØS_BORGER_SKJØNNSMESSIG_VURDERING,
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER,
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE_BARN,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_HAR_IKKE_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_SØKER_HAR_IKKE_OPPHOLDSTILLATELSE,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_LOVLIG_OPPHOLD_TREDJELANDSBORGER,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER,
                    VedtakBegrunnelseSpesifikasjon.AVSLAG_LOVLIG_OPPHOLD_SKJØNNSMESSIG_VURDERING_TREDJELANDSBORGER,
            )
    )

    val vedtakBegrunnelserIkkeTilknyttetVilkår = listOf(VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR,
                                                        VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING)
}