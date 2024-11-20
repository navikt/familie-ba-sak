package no.nav.familie.ba.sak.kjerne.brev.hjemler

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.hjemlerTilHjemmeltekst
import no.nav.familie.ba.sak.kjerne.brev.slåSammen
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
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

fun hentHjemmeltekst(
    vedtaksperioder: List<VedtaksperiodeMedBegrunnelser>,
    standardbegrunnelseTilSanityBegrunnelse: Map<Standardbegrunnelse, SanityBegrunnelse>,
    eøsStandardbegrunnelseTilSanityBegrunnelse: Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse>,
    opplysningspliktHjemlerSkalMedIBrev: Boolean = false,
    målform: Målform,
    vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false,
    refusjonEøsHjemmelSkalMedIBrev: Boolean = false,
    erFritekstIBrev: Boolean,
): String {
    val sanityStandardbegrunnelser =
        vedtaksperioder.flatMap { vedtaksperiode -> vedtaksperiode.begrunnelser.mapNotNull { begrunnelse -> standardbegrunnelseTilSanityBegrunnelse[begrunnelse.standardbegrunnelse] } }
    val sanityEøsBegrunnelser =
        vedtaksperioder.flatMap { vedtaksperiode -> vedtaksperiode.eøsBegrunnelser.mapNotNull { eøsBegrunnelse -> eøsStandardbegrunnelseTilSanityBegrunnelse[eøsBegrunnelse.begrunnelse] } }

    val ordinæreHjemler =
        hentOrdinæreHjemler(
            hjemler =
                (sanityStandardbegrunnelser.flatMap { it.hjemler } + sanityEøsBegrunnelser.flatMap { it.hjemler })
                    .toMutableSet(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            finnesVedtaksperiodeMedFritekst = erFritekstIBrev,
        )

    val forvaltningsloverHjemler = hentForvaltningsloverHjemler(vedtakKorrigertHjemmelSkalMedIBrev)

    val alleHjemlerForBegrunnelser =
        hentAlleTyperHjemler(
            hjemlerSeparasjonsavtaleStorbritannia = hentSeprasjonsavtaleStorbritanniaHjemler(sanityEøsBegrunnelser),
            ordinæreHjemler = ordinæreHjemler.distinct(),
            hjemlerFraFolketrygdloven =
                (sanityStandardbegrunnelser.flatMap { it.hjemlerFolketrygdloven } + sanityEøsBegrunnelser.flatMap { it.hjemlerFolketrygdloven })
                    .distinct(),
            hjemlerEØSForordningen883 = sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen883 }.distinct(),
            hjemlerEØSForordningen987 = hentHjemlerForEøsForordningen987(sanityEøsBegrunnelser, refusjonEøsHjemmelSkalMedIBrev),
            målform = målform,
            hjemlerFraForvaltningsloven = forvaltningsloverHjemler,
        )

    return slåSammenHjemlerAvUlikeTyper(alleHjemlerForBegrunnelser)
}

private fun hentHjemlerForEøsForordningen987(
    sanityEøsBegrunnelser: List<SanityEØSBegrunnelse>,
    refusjonEøsHjemmelSkalMedIBrev: Boolean,
): List<String> {
    val hjemler = mutableListOf<String>()

    hjemler.addAll(sanityEøsBegrunnelser.flatMap { it.hjemlerEØSForordningen987 })

    if (refusjonEøsHjemmelSkalMedIBrev) {
        hjemler.add("60")
    }

    return hjemler.distinct()
}

private fun slåSammenHjemlerAvUlikeTyper(hjemler: List<String>) =
    when (hjemler.size) {
        0 -> throw FunksjonellFeil("Ingen hjemler var knyttet til begrunnelsen(e) som er valgt. Du må velge minst én begrunnelse som er knyttet til en hjemmel.")
        1 -> hjemler.single()
        else -> hjemler.slåSammen()
    }

private fun hentAlleTyperHjemler(
    hjemlerSeparasjonsavtaleStorbritannia: List<String>,
    ordinæreHjemler: List<String>,
    hjemlerFraFolketrygdloven: List<String>,
    hjemlerEØSForordningen883: List<String>,
    hjemlerEØSForordningen987: List<String>,
    målform: Målform,
    hjemlerFraForvaltningsloven: List<String>,
): List<String> {
    val alleHjemlerForBegrunnelser = mutableListOf<String>()

    // Rekkefølgen her er viktig
    if (hjemlerSeparasjonsavtaleStorbritannia.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "Separasjonsavtalen mellom Storbritannia og Norge artikkel"
                    Målform.NN -> "Separasjonsavtalen mellom Storbritannia og Noreg artikkel"
                }
            } ${
                Utils.slåSammen(
                    hjemlerSeparasjonsavtaleStorbritannia,
                )
            }",
        )
    }
    if (ordinæreHjemler.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "barnetrygdloven"
                    Målform.NN -> "barnetrygdlova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = ordinæreHjemler,
                    lovForHjemmel = "barnetrygdloven",
                )
            }",
        )
    }
    if (hjemlerFraFolketrygdloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "folketrygdloven"
                    Målform.NN -> "folketrygdlova"
                }
            } ${
                hjemlerTilHjemmeltekst(
                    hjemler = hjemlerFraFolketrygdloven,
                    lovForHjemmel = "folketrygdloven",
                )
            }",
        )
    }
    if (hjemlerEØSForordningen883.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 883/2004 artikkel ${Utils.slåSammen(hjemlerEØSForordningen883)}")
    }
    if (hjemlerEØSForordningen987.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add("EØS-forordning 987/2009 artikkel ${Utils.slåSammen(hjemlerEØSForordningen987)}")
    }
    if (hjemlerFraForvaltningsloven.isNotEmpty()) {
        alleHjemlerForBegrunnelser.add(
            "${
                when (målform) {
                    Målform.NB -> "forvaltningsloven"
                    Målform.NN -> "forvaltningslova"
                }
            } ${
                hjemlerTilHjemmeltekst(hjemler = hjemlerFraForvaltningsloven, lovForHjemmel = "forvaltningsloven")
            }",
        )
    }
    return alleHjemlerForBegrunnelser
}


