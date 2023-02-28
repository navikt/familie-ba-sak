package no.nav.familie.ba.sak.kjerne.brev

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstavIHvertOrd
import no.nav.familie.ba.sak.common.tilDagMånedÅr
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.integrasjoner.sanity.SanityService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.brev.domene.MinimertVedtaksperiode
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Autovedtak6og18årOgSmåbarnstillegg
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakNyfødtBarnFraFør
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.AutovedtakNyfødtFørsteBarn
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Avslag
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Dødsfall
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.DødsfallData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Etterbetaling
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.EtterbetalingInstitusjon
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.FeilutbetaltValuta
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.ForsattInnvilget
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorreksjonVedtaksbrev
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorreksjonVedtaksbrevData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.KorrigertVedtakData
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.OpphørMedEndring
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Opphørt
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.SignaturVedtak
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.VedtakFellesfelter
import no.nav.familie.ba.sak.kjerne.brev.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.korrigertetterbetaling.KorrigertEtterbetalingService
import no.nav.familie.ba.sak.kjerne.korrigertvedtak.KorrigertVedtakService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.domene.Totrinnskontroll
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.sikkerhet.SaksbehandlerContext
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BrevService(
    private val totrinnskontrollService: TotrinnskontrollService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val simuleringService: SimuleringService,
    private val vedtaksperiodeService: VedtaksperiodeService,
    private val brevPeriodeService: BrevPeriodeService,
    private val sanityService: SanityService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val korrigertEtterbetalingService: KorrigertEtterbetalingService,
    private val organisasjonService: OrganisasjonService,
    private val korrigertVedtakService: KorrigertVedtakService,
    private val saksbehandlerContext: SaksbehandlerContext,
    private val brevmalService: BrevmalService,
    private val featureToggleService: FeatureToggleService
) {

    fun hentVedtaksbrevData(vedtak: Vedtak): Vedtaksbrev {
        val behandling = vedtak.behandling

        val brevmal = brevmalService.hentBrevmal(
            behandling
        )

        val vedtakFellesfelter = lagVedtaksbrevFellesfelter(vedtak)
        validerBrevdata(brevmal, vedtakFellesfelter)

        return when (brevmal) {
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK -> Førstegangsvedtak(
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetaling = hentEtterbetaling(vedtak),
                informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak)
            )

            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON -> Førstegangsvedtak(
                mal = Brevmal.VEDTAK_FØRSTEGANGSVEDTAK_INSTITUSJON,
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak)
            )

            Brevmal.VEDTAK_ENDRING -> VedtakEndring(
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetaling = hentEtterbetaling(vedtak),
                erKlage = behandling.erKlage(),
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak),
                feilutbetaltValuta = vedtaksperiodeService.beskrivPerioderMedFeilutbetaltValuta(vedtak)?.let {
                    FeilutbetaltValuta(perioderMedForMyeUtbetalt = it)
                }
            )

            Brevmal.VEDTAK_ENDRING_INSTITUSJON -> VedtakEndring(
                mal = Brevmal.VEDTAK_ENDRING_INSTITUSJON,
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak),
                erKlage = behandling.erKlage(),
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id),
                informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak)
            )

            Brevmal.VEDTAK_OPPHØRT -> Opphørt(
                vedtakFellesfelter = vedtakFellesfelter,
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id)
            )

            Brevmal.VEDTAK_OPPHØRT_INSTITUSJON -> Opphørt(
                mal = Brevmal.VEDTAK_OPPHØRT_INSTITUSJON,
                vedtakFellesfelter = vedtakFellesfelter,
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id)
            )

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING -> OpphørMedEndring(
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetaling = hentEtterbetaling(vedtak),
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id)
            )

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON -> OpphørMedEndring(
                mal = Brevmal.VEDTAK_OPPHØR_MED_ENDRING_INSTITUSJON,
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetalingInstitusjon = hentEtterbetalingInstitusjon(vedtak),
                erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(behandlingId = behandling.id)
            )

            Brevmal.VEDTAK_AVSLAG -> Avslag(vedtakFellesfelter = vedtakFellesfelter)
            Brevmal.VEDTAK_AVSLAG_INSTITUSJON -> Avslag(
                mal = Brevmal.VEDTAK_AVSLAG_INSTITUSJON,
                vedtakFellesfelter = vedtakFellesfelter
            )

            Brevmal.VEDTAK_FORTSATT_INNVILGET -> ForsattInnvilget(
                vedtakFellesfelter = vedtakFellesfelter,
                informasjonOmAarligKontroll = vedtaksperiodeService.skalHaÅrligKontroll(vedtak)
            )

            Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON -> ForsattInnvilget(
                mal = Brevmal.VEDTAK_FORTSATT_INNVILGET_INSTITUSJON,
                vedtakFellesfelter = vedtakFellesfelter
            )

            Brevmal.AUTOVEDTAK_BARN_6_OG_18_ÅR_OG_SMÅBARNSTILLEGG -> Autovedtak6og18årOgSmåbarnstillegg(
                vedtakFellesfelter = vedtakFellesfelter
            )

            Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN -> AutovedtakNyfødtFørsteBarn(
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetaling = hentEtterbetaling(vedtak)
            )

            Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> AutovedtakNyfødtBarnFraFør(
                vedtakFellesfelter = vedtakFellesfelter,
                etterbetaling = hentEtterbetaling(vedtak)
            )

            else -> throw Feil("Forsøker å hente vedtaksbrevdata for brevmal ${brevmal.visningsTekst}")
        }
    }

    private fun validerBrevdata(
        brevmal: Brevmal,
        vedtakFellesfelter: VedtakFellesfelter
    ) {
        if (listOf(
                Brevmal.VEDTAK_OPPHØRT,
                Brevmal.VEDTAK_OPPHØRT_INSTITUSJON
            ).contains(brevmal) && vedtakFellesfelter.perioder.size > 1
        ) {
            throw Feil(
                "Brevtypen er \"opphørt\", men mer enn én periode ble sendt med. Brev av typen opphørt skal kun ha én " + "periode."
            )
        }
    }

    fun hentDødsfallbrevData(vedtak: Vedtak): Brev =
        hentGrunnlagOgSignaturData(vedtak).let { data ->
            Dødsfall(
                data = DødsfallData(
                    delmalData = DødsfallData.DelmalData(
                        signaturVedtak = SignaturVedtak(
                            enhet = data.enhet,
                            saksbehandler = data.saksbehandler,
                            beslutter = data.beslutter
                        )
                    ),
                    flettefelter = DødsfallData.Flettefelter(
                        navn = data.grunnlag.søker.navn,
                        fodselsnummer = data.grunnlag.søker.aktør.aktivFødselsnummer(),
                        // Selv om det er feil å anta at alle navn er på dette formatet er det ønskelig å skrive
                        // det slik, da uppercase kan oppleves som skrikende i et brev som skal være skånsomt
                        navnAvdode = data.grunnlag.søker.navn.storForbokstavIHvertOrd(),
                        virkningstidspunkt = hentVirkningstidspunkt(
                            opphørsperioder = vedtaksperiodeService.hentOpphørsperioder(vedtak.behandling),
                            behandlingId = vedtak.behandling.id
                        )
                    )
                )
            )
        }

    fun hentKorreksjonbrevData(vedtak: Vedtak): Brev =
        hentGrunnlagOgSignaturData(vedtak).let { data ->
            KorreksjonVedtaksbrev(
                data = KorreksjonVedtaksbrevData(
                    delmalData = KorreksjonVedtaksbrevData.DelmalData(
                        signaturVedtak = SignaturVedtak(
                            enhet = data.enhet,
                            saksbehandler = data.saksbehandler,
                            beslutter = data.beslutter
                        )
                    ),
                    flettefelter = KorreksjonVedtaksbrevData.Flettefelter(
                        navn = data.grunnlag.søker.navn,
                        fodselsnummer = data.grunnlag.søker.aktør.aktivFødselsnummer()
                    )
                )
            )
        }

    fun lagVedtaksbrevFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
        val utvidetVedtaksperioderMedBegrunnelser =
            vedtaksperiodeService.hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak).filter {
                !(it.begrunnelser.isEmpty() && it.fritekster.isEmpty() && it.eøsBegrunnelser.isEmpty())
            }.sortedBy { it.fom }

        if (utvidetVedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil(
                "Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet."
            )
        }

        val grunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak)
        val brevPerioderData = brevPeriodeService.hentBrevperioderData(
            vedtaksperioderId = utvidetVedtaksperioderMedBegrunnelser.map { it.id },
            behandlingId = BehandlingId(vedtak.behandling.id)
        )
        val brevperioder = brevPerioderData.sorted().mapNotNull {
            it.tilBrevPeriodeGenerator().genererBrevPeriode()
        }
        val korrigertVedtak = korrigertVedtakService.finnAktivtKorrigertVedtakPåBehandling(vedtak.behandling.id)

        val hjemler = hentHjemler(
            behandlingId = vedtak.behandling.id,
            minimerteVedtaksperioder = brevPerioderData.map { it.minimertVedtaksperiode },
            målform = brevPerioderData.first().brevMålform,
            vedtakKorrigertHjemmelSkalMedIBrev = korrigertVedtak != null
        )

        val organisasjonsnummer = vedtak.behandling.fagsak.institusjon?.orgNummer
        val organisasjonsnavn = organisasjonsnummer?.let { organisasjonService.hentOrganisasjon(it).navn }

        return VedtakFellesfelter(
            enhet = grunnlagOgSignaturData.enhet,
            saksbehandler = grunnlagOgSignaturData.saksbehandler,
            beslutter = grunnlagOgSignaturData.beslutter,
            hjemmeltekst = Hjemmeltekst(hjemler),
            søkerNavn = organisasjonsnavn ?: grunnlagOgSignaturData.grunnlag.søker.navn,
            søkerFødselsnummer = grunnlagOgSignaturData.grunnlag.søker.aktør.aktivFødselsnummer(),
            perioder = brevperioder,
            organisasjonsnummer = organisasjonsnummer,
            gjelder = if (organisasjonsnummer != null) grunnlagOgSignaturData.grunnlag.søker.navn else null,
            korrigertVedtakData = korrigertVedtak?.let { KorrigertVedtakData(datoKorrigertVedtak = it.vedtaksdato.tilDagMånedÅr()) }
        )
    }

    private fun hentHjemler(
        behandlingId: Long,
        minimerteVedtaksperioder: List<MinimertVedtaksperiode>,
        målform: Målform,
        vedtakKorrigertHjemmelSkalMedIBrev: Boolean = false
    ): String {
        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandlingId)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val opplysningspliktHjemlerSkalMedIBrev =
            vilkårsvurdering.finnOpplysningspliktVilkår()?.resultat == Resultat.IKKE_OPPFYLT

        return hentHjemmeltekst(
            minimerteVedtaksperioder = minimerteVedtaksperioder,
            sanityBegrunnelser = sanityService.hentSanityBegrunnelser(),
            opplysningspliktHjemlerSkalMedIBrev = opplysningspliktHjemlerSkalMedIBrev,
            målform = målform,
            vedtakKorrigertHjemmelSkalMedIBrev = vedtakKorrigertHjemmelSkalMedIBrev
        )
    }

    private fun hentAktivtPersonopplysningsgrunnlag(behandlingId: Long) =
        persongrunnlagService.hentAktivThrows(behandlingId = behandlingId)

    private fun hentEtterbetaling(vedtak: Vedtak): Etterbetaling? =
        hentEtterbetalingsbeløp(vedtak)?.let { Etterbetaling(it) }

    private fun hentEtterbetalingInstitusjon(vedtak: Vedtak): EtterbetalingInstitusjon? =
        hentEtterbetalingsbeløp(vedtak)?.let { EtterbetalingInstitusjon(it) }

    private fun hentEtterbetalingsbeløp(vedtak: Vedtak): String? {
        val etterbetalingsBeløp =
            korrigertEtterbetalingService.finnAktivtKorrigeringPåBehandling(vedtak.behandling.id)?.beløp?.toBigDecimal()
                ?: simuleringService.hentEtterbetaling(vedtak.behandling.id)

        return etterbetalingsBeløp.takeIf { it > BigDecimal.ZERO }?.run { Utils.formaterBeløp(this.toInt()) }
    }

    private fun erFeilutbetalingPåBehandling(behandlingId: Long): Boolean =
        simuleringService.hentFeilutbetaling(behandlingId) > BigDecimal.ZERO

    private fun hentGrunnlagOgSignaturData(vedtak: Vedtak): GrunnlagOgSignaturData {
        val personopplysningGrunnlag = hentAktivtPersonopplysningsgrunnlag(vedtak.behandling.id)
        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
            behandling = vedtak.behandling,
            totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )
        val enhet = arbeidsfordelingService.hentArbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn
        return GrunnlagOgSignaturData(
            grunnlag = personopplysningGrunnlag,
            saksbehandler = saksbehandler,
            beslutter = beslutter,
            enhet = enhet
        )
    }

    fun hentSaksbehandlerOgBeslutter(
        behandling: Behandling,
        totrinnskontroll: Totrinnskontroll?
    ): Pair<String, String> {
        return when {
            behandling.steg <= StegType.SEND_TIL_BESLUTTER || totrinnskontroll == null -> {
                Pair(saksbehandlerContext.hentSaksbehandlerSignaturTilBrev(), "Beslutter")
            }

            totrinnskontroll.erBesluttet() -> {
                Pair(totrinnskontroll.saksbehandler, totrinnskontroll.beslutter!!)
            }

            behandling.steg == StegType.BESLUTTE_VEDTAK -> {
                Pair(
                    totrinnskontroll.saksbehandler,
                    if (totrinnskontroll.saksbehandler == saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()) {
                        "Beslutter"
                    } else {
                        saksbehandlerContext.hentSaksbehandlerSignaturTilBrev()
                    }
                )
            }

            else -> {
                throw Feil("Prøver å hente saksbehandler og beslutters navn for generering av brev i en ukjent tilstand.")
            }
        }
    }

    private data class GrunnlagOgSignaturData(
        val grunnlag: PersonopplysningGrunnlag,
        val saksbehandler: String,
        val beslutter: String,
        val enhet: String
    )
}
