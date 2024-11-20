package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.internal.TestVerktøyService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.brev.brevBegrunnelseProdusent.BrevBegrunnelseFeil
import no.nav.familie.ba.sak.kjerne.brev.brevPeriodeProdusent.lagBrevPeriode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakFellesfelter
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.refusjonEøs.RefusjonEøsRepository
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service

@Service
class VedtaksbrevFellesfelterService(
    private val korrigertVedtakService: KorrigertVedtakService,
    private val refusjonEøsRepository: RefusjonEøsRepository,
    private val integrasjonClient: IntegrasjonClient,
    private val testVerktøyService: TestVerktøyService,
    private val persongrunnlagService: PersongrunnlagService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val organisasjonService: OrganisasjonService,
    private val opprettGrunnlagOgSignaturDataService: OpprettGrunnlagOgSignaturDataService,
) {
    fun lagVedtaksbrevFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
        val sorterteVedtaksperioderMedBegrunnelser = hentSorterteVedtaksperioderMedBegrunnelser(vedtak)

        if (sorterteVedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet.",
            )
        }

        val grunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak)

        val behandlingId = vedtak.behandling.id
        val personopplysningGrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)

        val grunnlagForBegrunnelser = vedtaksperiodeService.hentGrunnlagForBegrunnelse(vedtak.behandling)
        val brevperioder =
            sorterteVedtaksperioderMedBegrunnelser.mapNotNull { vedtaksperiode ->
                try {
                    vedtaksperiode.lagBrevPeriode(
                        grunnlagForBegrunnelse = grunnlagForBegrunnelser,
                        landkoder = integrasjonClient.hentLandkoderISO2(),
                    )
                } catch (e: BrevBegrunnelseFeil) {
                    secureLogger.info(
                        "Brevbegrunnelsefeil for behandling $behandlingId, " +
                            "fagsak ${vedtak.behandling.fagsak.id} " +
                            "på periode ${vedtaksperiode.fom} - ${vedtaksperiode.tom}. " +
                            "\nAutogenerert test:\n" + testVerktøyService.hentBegrunnelsetest(behandlingId),
                    )
                    throw IllegalStateException(e.message, e)
                }
            }

        val utbetalingerPerMndEøs = hentUtbetalingerPerMndEøs(vedtak)

        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(behandlingId)
        val refusjonEøs = refusjonEøsRepository.finnRefusjonEøsForBehandling(behandlingId)

        val hjemler =
            hentHjemler(
                behandlingId = behandlingId,
                erFritekstIBrev = sorterteVedtaksperioderMedBegrunnelser.any { it.fritekster.isNotEmpty() },
                vedtaksperioder = sorterteVedtaksperioderMedBegrunnelser,
                målform = personopplysningGrunnlag.søker.målform,
                vedtakKorrigertHjemmelSkalMedIBrev = korrigertVedtak != null,
                refusjonEøsHjemmelSkalMedIBrev = refusjonEøs.isNotEmpty(),
            )

        val organisasjonsnummer =
            vedtak.behandling.fagsak.institusjon
                ?.orgNummer
        val organisasjonsnavn = organisasjonsnummer?.let { organisasjonService.hentOrganisasjon(it).navn }

        return VedtakFellesfelter(
            enhet = grunnlagOgSignaturData.enhet,
            saksbehandler = grunnlagOgSignaturData.saksbehandler,
            beslutter = grunnlagOgSignaturData.beslutter,
            hjemmeltekst = Hjemmeltekst(hjemler),
            søkerNavn = organisasjonsnavn ?: grunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer =
                grunnlagOgSignaturData.grunnlag.søker.aktør
                    .aktivFødselsnummer(),
            perioder = brevperioder,
            organisasjonsnummer = organisasjonsnummer,
            gjelder = if (organisasjonsnummer != null) grunnlagOgSignaturData.grunnlag.søker.navn else null,
            korrigertVedtakData = korrigertVedtak?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) },
            utbetalingerPerMndEøs = utbetalingerPerMndEøs,
        )
    }

    private fun hentSorterteVedtaksperioderMedBegrunnelser(vedtak: Vedtak) =
        vedtaksperiodeService
            .hentPersisterteVedtaksperioder(vedtak)
            .filter { it.erBegrunnet() }
            .sortedBy { it.fom }

    private fun hentHjemler(
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
