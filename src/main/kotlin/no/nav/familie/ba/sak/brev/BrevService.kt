package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.BrevPeriodeService
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ba.sak.brev.domene.maler.Opphørt
import no.nav.familie.ba.sak.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.brev.domene.maler.VedtakFellesfelter
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.økonomi.ØkonomiService
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

        val vedtakFellesfelter = hentVetakFellesfelter(vedtak)
        return when (hentVedtaksbrevtype(vedtak.behandling)) {
            Vedtaksbrevtype.FØRSTEGANGSVEDTAK -> Førstegangsvedtak(vedtakFellesfelter = vedtakFellesfelter,
                                                                   etterbetalingsbeløp = hentEtterbetalingsbeløp(vedtak))
            Vedtaksbrevtype.VEDTAK_ENDRING -> VedtakEndring(vedtakFellesfelter = vedtakFellesfelter,
                                                            etterbetalingsbeløp = hentEtterbetalingsbeløp(vedtak),
                                                            erKlage = vedtak.behandling.erKlage(),
                                                            erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling())
            Vedtaksbrevtype.OPPHØRT -> Opphørt(vedtakFellesfelter = vedtakFellesfelter,
                                               erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling())
            Vedtaksbrevtype.OPPHØRT_ENDRING -> throw Feil("'Opphørt endring'-brev er ikke støttet for ny brevløsning")
        }
    }

    private fun hentVetakFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
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
                perioder = brevPeriodeService.hentVedtaksperioder(vedtak),
        )
    }

    private fun hentEtterbetalingsbeløp(vedtak: Vedtak) = økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }
            ?.run { Utils.formaterBeløp(this) }

    private fun erFeilutbetalingPåBehandling() = hentFeilutbetaling() > 0

    private fun hentFeilutbetaling() = 0 //TODO Må legges inn senere når simulering er implementert.
    // Inntil da er det tryggest å utelate denne informasjonen fra brevet.
}