package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class HjemlerService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val sanityService: SanityService,
    private val persongrunnlagService: PersongrunnlagService,
    private val refusjonEøsRepository: RefusjonEøsRepository,
) {
    fun hentHjemler(
        behandlingId: Long,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
        sorterteVedtaksperioderMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>,
    ): String {
        val refusjonEøsHjemmelSkalMedIBrev = refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId)

        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)

        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
                ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val opplysningspliktHjemlerSkalMedIBrev =
            vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT

        return hentHjemmeltekst(
            vedtaksperioder = sorterteVedtaksperioderMedBegrunnelser,
            standardbegrunnelseTilSanityBegrunnelse = sanityService.hentSanityBegrunnelser(),
            eøsStandardbegrunnelseTilSanityBegrunnelse = sanityService.hentSanityEØSBegrunnelser(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            målform = personopplysningGrunnlag.søker.målform,
            vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev,
            refusjonEøsHjemmelSkalMedIBrev = refusjonEøsHjemmelSkalMedIBrev.isNotEmpty(),
            erFritekstIBrev = sorterteVedtaksperioderMedBegrunnelser.any { it.fritekster.isNotEmpty() },
        )
    }
}
