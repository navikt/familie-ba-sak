package no.nav.familie.ba.sak.config

import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.EndretUtbetalingsperiodeTrigger
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.dokument.domene.SanityVilkår
import no.nav.familie.ba.sak.kjerne.dokument.domene.VilkårRolle
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.dokument.domene.ØvrigTrigger
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.Årsak
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.BegrunnelseData
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
@Profile("mock-brev-klient")
@Primary
class BrevKlientMock : BrevKlient(
    familieBrevUri = "brev_uri_mock",
    restTemplate = RestTemplate()
) {

    override fun genererBrev(målform: String, brev: Brev): ByteArray {
        return TEST_PDF
    }

    override fun hentSanityBegrunnelser(): List<SanityBegrunnelse> {
        return sanityBegrunnelserMock
    }

    override fun hentBegrunnelsestekst(begrunnelseData: BegrunnelseData): String {
        return "Dummytekst for ${begrunnelseData.apiNavn}"
    }
}

val sanityBegrunnelserMock: List<SanityBegrunnelse> =
    VedtakBegrunnelseSpesifikasjon.values()
        .map {
            SanityBegrunnelse(
                apiNavn = it.sanityApiNavn,
                navnISystem = it.name,
                ovrigeTriggere = when (it) {
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR -> listOf(ØvrigTrigger.BARN_MED_6_ÅRS_DAG)
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING -> listOf(ØvrigTrigger.SATSENDRING)
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER -> listOf(ØvrigTrigger.MANGLER_OPPLYSNINGER)
                    else -> null
                },
                vilkaar = when (it) {
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET -> listOf(SanityVilkår.BOSATT_I_RIKET)
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE -> listOf(SanityVilkår.LOVLIG_OPPHOLD)
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING -> null
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER -> listOf(SanityVilkår.BOR_MED_SOKER)

                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR -> null
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR -> listOf(SanityVilkår.UNDER_18_ÅR)
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET -> listOf(SanityVilkår.BOSATT_I_RIKET)

                    VedtakBegrunnelseSpesifikasjon.OPPHØR_UTVANDRET -> listOf(SanityVilkår.BOSATT_I_RIKET)
                    VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER -> null

                    else -> SanityVilkår.values()
                        /*.filter { sanityVilkår -> sanityVilkår != SanityVilkår.UTVIDET_BARNETRYGD }*/.toList()
                },
                rolle = when (it) {
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET -> listOf(VilkårRolle.BARN)
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOR_HOS_SØKER -> listOf(
                        VilkårRolle.BARN,
                        VilkårRolle.SOKER
                    )
                    else -> null
                },
                hjemler = when (it) {
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET -> listOf("2", "4", "11")
                    VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING -> listOf("2", "10")
                    else -> listOf("98", "99", "100")
                },
                endringsaarsaker = when (it.vedtakBegrunnelseType) {
                    VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> listOf(Årsak.DELT_BOSTED)
                    VedtakBegrunnelseType.ENDRET_UTBETALING -> listOf(Årsak.DELT_BOSTED)
                    else -> emptyList()
                },
                endretUtbetalingsperiodeTriggere = when (it.vedtakBegrunnelseType) {
                    VedtakBegrunnelseType.ETTER_ENDRET_UTBETALING -> listOf(EndretUtbetalingsperiodeTrigger.ETTER_ENDRET_UTBETALINGSPERIODE)
                    else -> emptyList()
                }
            )
        }
