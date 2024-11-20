package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.slåSammen
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class HjemlerService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val sanityService: SanityService,
    private val persongrunnlagService: PersongrunnlagService,
    private val refusjonEøsService: RefusjonEøsService,
) {
    fun hentHjemler(
        behandlingId: Long,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
        sorterteVedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    ): String {
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
                ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val opplysningspliktHjemlerSkalMedIBrev =
            vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT

        val sanityStandardbegrunnelser =
            sorterteVedtaksperioderMedBegrunnelser.flatMap { vedtaksperiode -> vedtaksperiode.begrunnelser.mapNotNull { begrunnelse -> sanityService.hentSanityBegrunnelser()[begrunnelse.standardbegrunnelse] } }

        val sanityEøsBegrunnelser =
            sorterteVedtaksperioderMedBegrunnelser.flatMap { vedtaksperiode -> vedtaksperiode.eøsBegrunnelser.mapNotNull { eøsBegrunnelse -> sanityService.hentSanityEØSBegrunnelser()[eøsBegrunnelse.begrunnelse] } }

        val alleHjemlerForBegrunnelser =
            kombinerHjemler(
                hjemlerSeparasjonsavtaleStorbritannia = hentSeprasjonsavtaleStorbritanniaHjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                ordinæreHjemler =
                    hentOrdinæreHjemler(
                        sanityStandardbegrunnelser = sanityStandardbegrunnelser,
                        sanityEøsBegrunnelser = sanityEøsBegrunnelser,
                        opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
                        finnesVedtaksperiodeMedFritekst = sorterteVedtaksperioderMedBegrunnelser.any { it.fritekster.isNotEmpty() },
                    ),
                hjemlerFraFolketrygdloven = hentFolketrygdlovenHjemler(sanityStandardbegrunnelser = sanityStandardbegrunnelser, sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                hjemlerEØSForordningen883 = hentEØSForordningen883Hjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                hjemlerEØSForordningen987 = hentHjemlerForEøsForordningen987(sanityEøsBegrunnelser, refusjonEøsHjemmelSkalMedIBrev = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId)),
                målform = persongrunnlagService.hentSøkersMålform(behandlingId = behandlingId),
                hjemlerFraForvaltningsloven = hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev),
            )

        return slåSammenHjemlerAvUlikeTyper(alleHjemlerForBegrunnelser)
    }
}

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) =
    when (hjemler.size) {
        0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
        1 -> hjemler.single()
        else -> hjemler.slåSammen()
    }
