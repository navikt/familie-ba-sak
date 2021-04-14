package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.brev.domene.maler.*
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.økonomi.ØkonomiService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BrevService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val økonomiService: ØkonomiService,
        private val brevPeriodeService: BrevPeriodeService,
) {

    fun hentVedtaksbrevData(vedtak: Vedtak): Vedtaksbrev {
        val vedtakstype = hentVedtaksbrevtype(vedtak.behandling)
        val vedtakFellesfelter = hentVedtaksbrevFellesfelter(vedtak)
        logger.info("Genererer data for '${vedtakstype.visningsTekst}'")
        return when (vedtakstype) {
            Vedtaksbrevtype.FØRSTEGANGSVEDTAK -> Førstegangsvedtak(vedtakFellesfelter = vedtakFellesfelter,
                                                                   etterbetaling = hentEtterbetaling(vedtak))


            Vedtaksbrevtype.VEDTAK_ENDRING -> VedtakEndring(vedtakFellesfelter = vedtakFellesfelter,
                                                            etterbetaling = hentEtterbetaling(vedtak),
                                                            erKlage = vedtak.behandling.erKlage(),
                                                            erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling())

            Vedtaksbrevtype.OPPHØRT -> Opphørt(vedtakFellesfelter = vedtakFellesfelter,
                                               erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling())

            Vedtaksbrevtype.OPPHØR_MED_ENDRING -> OpphørMedEndring(vedtakFellesfelter = vedtakFellesfelter,
                                                                   etterbetaling = hentEtterbetaling(vedtak),
                                                                   erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling())

            Vedtaksbrevtype.AVSLAG -> Avslag(vedtakFellesfelter = vedtakFellesfelter)

            Vedtaksbrevtype.AUTOVEDTAK_BARN6_ÅR,
            Vedtaksbrevtype.AUTOVEDTAK_BARN18_ÅR -> VedtakEndring(vedtakFellesfelter = vedtakFellesfelter,
                                                                  etterbetaling = null,
                                                                  erKlage = false,
                                                                  erFeilutbetalingPåBehandling = false)
        }
    }

    private fun verifiserVedtakHarBegrunnelse(vedtak: Vedtak) {
        if (vedtak.vedtakBegrunnelser.size == 0) {
            throw FunksjonellFeil(melding = "Vedtaket har ingen begrunnelser",
                                  frontendFeilmelding = "Vedtaket har ingen begrunnelser")
        }
    }

    fun hentVedtaksbrevFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
        verifiserVedtakHarBegrunnelse(vedtak)

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")


        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        return VedtakFellesfelter(
                enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                hjemmeltekst = Hjemmeltekst(vedtak.hentHjemmelTekst()),
                søkerNavn = personopplysningGrunnlag.søker.navn,
                søkerFødselsnummer = personopplysningGrunnlag.søker.personIdent.ident,
                perioder = brevPeriodeService.hentBrevPerioder(vedtak),
        )
    }

    private fun hentEtterbetaling(vedtak: Vedtak): Etterbetaling? {
        val etterbetalingsbeløp = hentEtterbetalingsbeløp(vedtak)
        return if (!etterbetalingsbeløp.isNullOrBlank()) {
            Etterbetaling(etterbetalingsbeløp = etterbetalingsbeløp)
        } else {
            null
        }
    }

    private fun hentEtterbetalingsbeløp(vedtak: Vedtak) = økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }
            ?.run { Utils.formaterBeløp(this) }

    private fun erFeilutbetalingPåBehandling() = hentFeilutbetaling() > 0

    private fun hentFeilutbetaling() = 0 //TODO Må legges inn senere når simulering er implementert.
    // Inntil da er det tryggest å utelate denne informasjonen fra brevet.

    companion object {
        private val logger = LoggerFactory.getLogger(BrevService::class.java)
    }
}