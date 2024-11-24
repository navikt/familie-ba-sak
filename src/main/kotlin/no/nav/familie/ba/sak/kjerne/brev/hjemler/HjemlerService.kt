package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
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
    fun hentHjemmeltekst(
        behandlingId: Long,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
        sorterteVedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    ): String {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
        if (vilkårsvurdering == null) {
            throw IllegalStateException("Finner ikke vilkårsvurdering ved begrunning av vedtak")
        }

        val begrunnelseTilSanityBegrunnelse = sanityService.hentSanityBegrunnelser()
        val eøsBegrunnelseTilSanityEøsBegrunnelse = sanityService.hentSanityEØSBegrunnelser()

        val sanitybegrunnelser =
            sorterteVedtaksperioderMedBegrunnelser.flatMap { vedtaksperiode ->
                vedtaksperiode.begrunnelser.mapNotNull { begrunnelse ->
                    begrunnelseTilSanityBegrunnelse[begrunnelse.standardbegrunnelse]
                }
            }

        val sanityEøsBegrunnelser =
            sorterteVedtaksperioderMedBegrunnelser.flatMap { vedtaksperiode ->
                vedtaksperiode.eøsBegrunnelser.mapNotNull { eøsBegrunnelse ->
                    eøsBegrunnelseTilSanityEøsBegrunnelse[eøsBegrunnelse.begrunnelse]
                }
            }

        val alleHjemlerForBegrunnelser =
            kombinerHjemler(
                målform = persongrunnlagService.hentSøkersMålform(behandlingId = behandlingId),
                hjemlerSeparasjonsavtaleStorbritannia = hentSeprasjonsavtaleStorbritanniaHjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                ordinæreHjemler =
                    hentOrdinæreHjemler(
                        sanityBegrunnelser = sanitybegrunnelser,
                        sanityEøsBegrunnelser = sanityEøsBegrunnelser,
                        opplysningspliktHjemlerSkalMedIBrev = !vilkårsvurdering.erOpplysningspliktVilkårOppfylt(),
                        finnesVedtaksperiodeMedFritekst = sorterteVedtaksperioderMedBegrunnelser.any { it.fritekster.isNotEmpty() },
                    ),
                hjemlerFraFolketrygdloven = utledFolketrygdlovenHjemler(sanityBegrunnelser = sanitybegrunnelser, sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                hjemlerEØSForordningen883 = utledEØSForordningen883Hjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                hjemlerEØSForordningen987 = utledHjemlerForEøsForordningen987(sanityEøsBegrunnelser = sanityEøsBegrunnelser, refusjonEøsHjemmelSkalMedIBrev = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId)),
                hjemlerFraForvaltningsloven = utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev),
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
