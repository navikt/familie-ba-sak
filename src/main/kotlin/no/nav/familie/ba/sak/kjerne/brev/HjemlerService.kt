package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service

@Service
class HjemlerService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val sanityService: SanityService,
) {
    fun hentHjemler(
        behandlingId: Long,
        vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
        målform: Målform,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
        refusjonEøsHjemmelSkalMedIBrev: Boolean,
        erFritekstIBrev: Boolean,
    ): String {
        val vilkårsvurdering =
            vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
                ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val opplysningspliktHjemlerSkalMedIBrev =
            vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT

        return hentHjemmeltekst(
            vedtaksperioder = vedtaksperioder,
            standardbegrunnelseTilSanityBegrunnelse = sanityService.hentSanityBegrunnelser(),
            eøsStandardbegrunnelseTilSanityBegrunnelse = sanityService.hentSanityEØSBegrunnelser(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            målform = målform,
            vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev,
            refusjonEøsHjemmelSkalMedIBrev = refusjonEøsHjemmelSkalMedIBrev,
            erFritekstIBrev = erFritekstIBrev,
        )
    }
}
