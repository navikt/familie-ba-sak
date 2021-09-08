package no.nav.familie.ba.sak.kjerne.dokument


import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.Utils.storForbokstavIHvertOrd
import no.nav.familie.ba.sak.common.nesteMåned
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.AutovedtakNyfødtBarnFraFør
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.AutovedtakNyfødtFørsteBarn
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Avslag
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brev
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Dødsfall
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.DødsfallData
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Etterbetaling
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.ForsattInnvilget
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Hjemmeltekst
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.OpphørMedEndring
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Opphørt
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.SignaturVedtak
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.VedtakFellesfelter
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilBrevPeriode
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BrevService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val brevPeriodeService: BrevPeriodeService,
        private val simuleringService: SimuleringService,
        private val vedtaksperiodeService: VedtaksperiodeService,
        private val featureToggleService: FeatureToggleService,
) {

    fun hentVedtaksbrevData(vedtak: Vedtak): Vedtaksbrev {
        val brevmal = hentVedtaksbrevmal(vedtak.behandling)
        val vedtakFellesfelter = lagVedtaksbrevFellesfelter(vedtak)
        validerBrevdata(brevmal, vedtakFellesfelter)

        return when (brevmal) {
            Brevmal.VEDTAK_FØRSTEGANGSVEDTAK -> Førstegangsvedtak(vedtakFellesfelter = vedtakFellesfelter,
                                                                  etterbetaling = hentEtterbetaling(vedtak))

            Brevmal.VEDTAK_ENDRING -> VedtakEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erKlage = vedtak.behandling.erKlage(),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(vedtak.behandling.id),
            )

            Brevmal.VEDTAK_OPPHØRT -> Opphørt(vedtakFellesfelter = vedtakFellesfelter,
                                              erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(vedtak.id))

            Brevmal.VEDTAK_OPPHØR_MED_ENDRING -> OpphørMedEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(vedtak.id),
            )

            Brevmal.VEDTAK_AVSLAG -> Avslag(vedtakFellesfelter = vedtakFellesfelter)

            Brevmal.VEDTAK_FORTSATT_INNVILGET -> ForsattInnvilget(vedtakFellesfelter = vedtakFellesfelter)

            Brevmal.AUTOVEDTAK_BARN6_ÅR,
            Brevmal.AUTOVEDTAK_BARN18_ÅR -> VedtakEndring(vedtakFellesfelter = vedtakFellesfelter,
                                                          etterbetaling = null,
                                                          erKlage = false,
                                                          erFeilutbetalingPåBehandling = false)
            Brevmal.AUTOVEDTAK_NYFØDT_FØRSTE_BARN -> AutovedtakNyfødtFørsteBarn(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
            )
            Brevmal.AUTOVEDTAK_NYFØDT_BARN_FRA_FØR -> AutovedtakNyfødtBarnFraFør(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
            )
            else -> throw Feil("Forsøker å hente vedtaksbrevdata for brevmal ${brevmal.visningsTekst}")
        }
    }

    private fun validerBrevdata(brevmal: Brevmal,
                                vedtakFellesfelter: VedtakFellesfelter) {
        if (brevmal == Brevmal.VEDTAK_OPPHØRT && vedtakFellesfelter.perioder.size > 1) {
            throw Feil("Brevtypen er \"opphørt\", men mer enn én periode ble sendt med. Brev av typen opphørt skal kun ha én " +
                       "periode.")
        }
    }

    fun hentDødsfallbrevData(vedtak: Vedtak): Brev =
            hentGrunnlagOgSignaturData(vedtak).let { data ->
                Dødsfall(
                        data = DødsfallData(
                                delmalData = DødsfallData.DelmalData(
                                        signaturVedtak = SignaturVedtak(enhet = data.enhet,
                                                                        saksbehandler = data.saksbehandler,
                                                                        beslutter = data.beslutter)),
                                flettefelter = DødsfallData.Flettefelter(
                                        navn = data.grunnlag.søker.navn,
                                        fodselsnummer = data.grunnlag.søker.personIdent.ident,
                                        // Selv om det er feil å anta at alle navn er på dette formatet er det ønskelig å skrive
                                        // det slik, da uppercase kan oppleves som skrikende i et brev som skal være skånsomt
                                        navnAvdode = data.grunnlag.søker.navn.storForbokstavIHvertOrd(),
                                        virkningstidspunkt = vedtaksperiodeService.hentUtbetalingsperioder(vedtak.behandling)
                                                                     .maxByOrNull { it.periodeTom }?.periodeTom?.nesteMåned()
                                                                     ?.tilMånedÅr()
                                                             ?: throw Feil("Fant ikke opphørdato ved generering av dødsfallbrev")
                                ))
                )
            }

    private fun verifiserVedtakHarBegrunnelse(vedtak: Vedtak) {
        if (vedtak.vedtakBegrunnelser.size == 0) {
            throw FunksjonellFeil(melding = "Vedtaket har ingen begrunnelser",
                                  frontendFeilmelding = "Vedtaket har ingen begrunnelser")
        }
    }

    @Deprecated("Skal skrives om til å bruke lagVedtaksbrevFellesfelter")
    fun lagVedtaksbrevFellesfelterDeprecated(vedtak: Vedtak): VedtakFellesfelter {
        verifiserVedtakHarBegrunnelse(vedtak)
        val data = hentGrunnlagOgSignaturData(vedtak)

        return VedtakFellesfelter(
                enhet = data.enhet,
                saksbehandler = data.saksbehandler,
                beslutter = data.beslutter,
                hjemmeltekst = Hjemmeltekst(vedtak.hentHjemmelTekst()),
                søkerNavn = data.grunnlag.søker.navn,
                søkerFødselsnummer = data.grunnlag.søker.personIdent.ident,
                perioder = brevPeriodeService.hentBrevPerioder(vedtak),
        )
    }

    fun lagVedtaksbrevFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
        val vedtaksperioderMedBegrunnelser = vedtaksperiodeService.hentPersisterteVedtaksperioder(vedtak).filter {
            it.begrunnelser.isNotEmpty() || it.fritekster.isNotEmpty()
        }

        if (vedtaksperioderMedBegrunnelser.isEmpty()) {
            throw FunksjonellFeil("Vedtaket mangler begrunnelser. Du må legge til begrunnelser for å generere vedtaksbrevet.")
        }

        val grunnlagOgSignaturData = hentGrunnlagOgSignaturData(vedtak)

        val utbetalingsperioder = vedtaksperiodeService.hentUtbetalingsperioder(vedtak.behandling)

        val hjemler = hentHjemmeltekst(vedtak, vedtaksperioderMedBegrunnelser)

        val målform = persongrunnlagService.hentSøkersMålform(vedtak.behandling.id)

        val brevperioder = vedtaksperioderMedBegrunnelser.sorter().mapNotNull {
            it.tilBrevPeriode(
                    personerIPersongrunnlag = grunnlagOgSignaturData.grunnlag.personer.toList(),
                    utbetalingsperioder = utbetalingsperioder,
                    målform = målform,
                    brukBegrunnelserFraSanity = featureToggleService.isEnabled(FeatureToggleConfig.BRUK_BEGRUNNELSE_FRA_SANITY_BACKEND),
            )
        }
        return VedtakFellesfelter(
                enhet = grunnlagOgSignaturData.enhet,
                saksbehandler = grunnlagOgSignaturData.saksbehandler,
                beslutter = grunnlagOgSignaturData.beslutter,
                hjemmeltekst = Hjemmeltekst(hjemler),
                søkerNavn = grunnlagOgSignaturData.grunnlag.søker.navn,
                søkerFødselsnummer = grunnlagOgSignaturData.grunnlag.søker.personIdent.ident,
                perioder = brevperioder
        )
    }

    private fun hentAktivtPersonopplysningsgrunnlag(behandlingId: Long) =
            persongrunnlagService.hentAktiv(behandlingId = behandlingId)
            ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                          frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

    private fun hentEtterbetaling(vedtak: Vedtak): Etterbetaling? =
            hentEtterbetalingsbeløp(vedtak)?.let { Etterbetaling(it) }

    private fun hentEtterbetalingsbeløp(vedtak: Vedtak): String? =
            simuleringService.hentEtterbetaling(vedtak.behandling.id)
                    .takeIf { it > BigDecimal.ZERO }
                    ?.run { Utils.formaterBeløp(this.toInt()) }

    private fun erFeilutbetalingPåBehandling(behandlingId: Long): Boolean =
            simuleringService.hentFeilutbetaling(behandlingId) > BigDecimal.ZERO

    private fun hentGrunnlagOgSignaturData(vedtak: Vedtak): GrunnlagOgSignaturData {
        val personopplysningGrunnlag = hentAktivtPersonopplysningsgrunnlag(vedtak.behandling.id)
        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )
        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn
        return GrunnlagOgSignaturData(grunnlag = personopplysningGrunnlag,
                                      saksbehandler = saksbehandler,
                                      beslutter = beslutter,
                                      enhet = enhet
        )
    }

    private data class GrunnlagOgSignaturData(
            val grunnlag: PersonopplysningGrunnlag,
            val saksbehandler: String,
            val beslutter: String,
            val enhet: String
    )

}