package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Component

@Component
class HjemmeltekstUtleder(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val sanityService: SanityService,
    private val persongrunnlagService: PersongrunnlagService,
    private val refusjonEøsService: RefusjonEøsService,
) {
    fun utledHjemmeltekst(
        behandlingId: Long,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean,
        sorterteVedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    ): String {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
        if (vilkårsvurdering == null) {
            throw Feil("Finner ikke vilkårsvurdering ved begrunning av vedtak")
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
                separasjonsavtaleStorbritanniaHjemler = utledSeprasjonsavtaleStorbritanniaHjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                ordinæreHjemler =
                    utledOrdinæreHjemler(
                        sanityBegrunnelser = sanitybegrunnelser,
                        sanityEøsBegrunnelser = sanityEøsBegrunnelser,
                        opplysningspliktHjemlerSkalMedIBrev = vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT,
                        finnesVedtaksperiodeMedFritekst = sorterteVedtaksperioderMedBegrunnelser.any { it.fritekster.isNotEmpty() },
                    ),
                folketrygdlovenHjemler = utledFolketrygdlovenHjemler(sanityBegrunnelser = sanitybegrunnelser, sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                eøsForordningen883Hjemler = utledEØSForordningen883Hjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser),
                eøsForordningen987Hjemler = utledEØSForordningen987Hjemler(sanityEøsBegrunnelser = sanityEøsBegrunnelser, refusjonEøsHjemmelSkalMedIBrev = refusjonEøsService.harRefusjonEøsPåBehandling(behandlingId)),
                forvaltningslovenHjemler = utledForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev),
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
