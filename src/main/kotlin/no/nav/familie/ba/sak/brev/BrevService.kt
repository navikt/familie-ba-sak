package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.BrevPeriodeService
import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.dokument.DokumentUtils
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

    fun hentVedtaksbrevData(vedtak: Vedtak, behandlingResultat: BehandlingResultat): Vedtaksbrev {

        val vedtaksbrevtype =
                hentVedtaksbrevtype(vedtak.behandling.skalBehandlesAutomatisk, vedtak.behandling.type, behandlingResultat)

        return when (vedtaksbrevtype) {
            Vedtaksbrevtype.FØRSTEGANGSVEDTAK -> hentFørstegangsvedtakData(vedtak)
            Vedtaksbrevtype.VEDTAK_ENDRING -> hentVedtakEndringData(vedtak)
            Vedtaksbrevtype.OPPHØRT -> throw Feil("'Opphørt'-brev er ikke støttet for ny brevløsning")
            Vedtaksbrevtype.OPPHØRT_ENDRING -> throw Feil("'Opphørt endring'-brev er ikke støttet for ny brevløsning")
        }
    }


    private fun hentFørstegangsvedtakData(vedtak: Vedtak): Førstegangsvedtak {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")


        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val etterbetalingsbeløp =
                økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }?.run { Utils.formaterBeløp(this) }

        return Førstegangsvedtak(
                enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                hjemler = vedtak.hentHjemmelTekst(),
                søkerNavn = personopplysningGrunnlag.søker.navn,
                søkerFødselsnummer = personopplysningGrunnlag.søker.personIdent.ident,
                perioder = brevPeriodeService.hentVedtaksperioder(vedtak),

                etterbetalingsbeløp = etterbetalingsbeløp,
        )
    }

    private fun hentVedtakEndringData(vedtak: Vedtak): VedtakEndring {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                                       ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                                                     frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")

        val (saksbehandler, beslutter) = DokumentUtils.hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )

        val etterbetalingsbeløp =
                økonomiService.hentEtterbetalingsbeløp(vedtak).etterbetaling.takeIf { it > 0 }?.run { Utils.formaterBeløp(this) }

        return VedtakEndring(
                enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                hjemler = vedtak.hentHjemmelTekst(),
                søkerNavn = personopplysningGrunnlag.søker.navn,
                søkerFødselsnummer = personopplysningGrunnlag.søker.personIdent.ident,
                perioder = brevPeriodeService.hentVedtaksperioder(vedtak),

                klage = vedtak.behandling.erKlage(),
                feilutbetaling = tilbakekrevingsbeløpFraSimulering() > 0,
                etterbetalingsbeløp = etterbetalingsbeløp,
        )
    }

    private fun tilbakekrevingsbeløpFraSimulering() = 0 //TODO Må legges inn senere når simulering er implementert.
    // Inntil da er det tryggest å utelate denne informasjonen fra brevet.

}

fun hentVedtaksbrevtype(skalBehandlesAutomatisk: Boolean,
                        behandlingType: BehandlingType,
                        behandlingResultat: BehandlingResultat): Vedtaksbrevtype {
    val feilmeldingBehandlingTypeOgResultat =
            "Brev ikke støttet for behandlingstype=${behandlingType} og behandlingsresultat=${behandlingResultat}"
    val feilmeliding =
            "Brev ikke støttet for behandlingstype=${behandlingType}"

    return if (skalBehandlesAutomatisk) {
        throw Feil("Det er ikke laget funksjonalitet for automatisk behandling med ny brevløsning.")
    } else {
        when (behandlingType) {

            BehandlingType.FØRSTEGANGSBEHANDLING ->
                when (behandlingResultat) {
                    BehandlingResultat.INNVILGET, BehandlingResultat.INNVILGET_OG_OPPHØRT, BehandlingResultat.DELVIS_INNVILGET -> Vedtaksbrevtype.FØRSTEGANGSVEDTAK
                    else -> throw FunksjonellFeil(melding = feilmeldingBehandlingTypeOgResultat,
                                                  frontendFeilmelding = feilmeldingBehandlingTypeOgResultat)
                }

            BehandlingType.REVURDERING ->
                when (behandlingResultat) {
                    BehandlingResultat.INNVILGET, BehandlingResultat.DELVIS_INNVILGET -> Vedtaksbrevtype.VEDTAK_ENDRING
                    BehandlingResultat.OPPHØRT -> Vedtaksbrevtype.OPPHØRT
                    BehandlingResultat.INNVILGET_OG_OPPHØRT, BehandlingResultat.ENDRET_OG_OPPHØRT -> Vedtaksbrevtype.OPPHØRT_ENDRING
                    else -> throw FunksjonellFeil(melding = feilmeldingBehandlingTypeOgResultat,
                                                  frontendFeilmelding = feilmeldingBehandlingTypeOgResultat)
                }

            else -> throw FunksjonellFeil(melding = feilmeliding,
                                          frontendFeilmelding = feilmeliding)
        }
    }
}