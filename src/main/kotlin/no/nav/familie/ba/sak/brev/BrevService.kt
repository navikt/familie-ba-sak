package no.nav.familie.ba.sak.brev

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.brev.domene.maler.Avslag
import no.nav.familie.ba.sak.brev.domene.maler.Brev
import no.nav.familie.ba.sak.brev.domene.maler.Dødsfall
import no.nav.familie.ba.sak.brev.domene.maler.DødsfallData
import no.nav.familie.ba.sak.brev.domene.maler.Etterbetaling
import no.nav.familie.ba.sak.brev.domene.maler.ForsattInnvilget
import no.nav.familie.ba.sak.brev.domene.maler.Førstegangsvedtak
import no.nav.familie.ba.sak.brev.domene.maler.Hjemmeltekst
import no.nav.familie.ba.sak.brev.domene.maler.OpphørMedEndring
import no.nav.familie.ba.sak.brev.domene.maler.Opphørt
import no.nav.familie.ba.sak.brev.domene.maler.SignaturVedtak
import no.nav.familie.ba.sak.brev.domene.maler.VedtakEndring
import no.nav.familie.ba.sak.brev.domene.maler.VedtakFellesfelter
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrev
import no.nav.familie.ba.sak.brev.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Utils
import no.nav.familie.ba.sak.common.tilMånedÅr
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate

@Service
class BrevService(
        private val totrinnskontrollService: TotrinnskontrollService,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
        private val brevPeriodeService: BrevPeriodeService,
        private val simuleringService: SimuleringService
) {

    fun hentVedtaksbrevData(vedtak: Vedtak): Vedtaksbrev {
        val vedtakstype = hentVedtaksbrevtype(vedtak.behandling)
        val vedtakFellesfelter = lagVedtaksbrevFellesfelter(vedtak)
        return when (vedtakstype) {
            Vedtaksbrevtype.FØRSTEGANGSVEDTAK -> Førstegangsvedtak(vedtakFellesfelter = vedtakFellesfelter,
                                                                   etterbetaling = hentEtterbetaling(vedtak))

            Vedtaksbrevtype.VEDTAK_ENDRING -> VedtakEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erKlage = vedtak.behandling.erKlage(),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(vedtak.behandling.id),
            )

            Vedtaksbrevtype.OPPHØRT -> Opphørt(vedtakFellesfelter = vedtakFellesfelter,
                                               erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(vedtak.id))

            Vedtaksbrevtype.OPPHØR_MED_ENDRING -> OpphørMedEndring(
                    vedtakFellesfelter = vedtakFellesfelter,
                    etterbetaling = hentEtterbetaling(vedtak),
                    erFeilutbetalingPåBehandling = erFeilutbetalingPåBehandling(vedtak.id),
            )

            Vedtaksbrevtype.AVSLAG -> Avslag(vedtakFellesfelter = vedtakFellesfelter)

            Vedtaksbrevtype.FORTSATT_INNVILGET -> ForsattInnvilget(vedtakFellesfelter = vedtakFellesfelter)

            Vedtaksbrevtype.AUTOVEDTAK_BARN6_ÅR,
            Vedtaksbrevtype.AUTOVEDTAK_BARN18_ÅR -> VedtakEndring(vedtakFellesfelter = vedtakFellesfelter,
                                                                  etterbetaling = null,
                                                                  erKlage = false,
                                                                  erFeilutbetalingPåBehandling = false)
        }
    }

    fun hentDødsfallbrevData(vedtak: Vedtak): Brev =
            hentSøkerOgSignaturData(vedtak).let {
                Dødsfall(
                        data = DødsfallData(
                                delmalData = DødsfallData.DelmalData(
                                        signaturVedtak = SignaturVedtak(enhet = it.enhet,
                                                                  saksbehandler = it.saksbehandler,
                                                                  beslutter = it.beslutter)),
                                flettefelter = DødsfallData.Flettefelter(
                                        navn = it.søker.navn,
                                        fodselsnummer = it.søker.personIdent.ident,
                                        navnSakspart = it.søker.navn,
                                        virkningstidspunkt = LocalDate.now().plusMonths(1).tilMånedÅr()
                                ))
                )
            }

    private fun verifiserVedtakHarBegrunnelse(vedtak: Vedtak) {
        if (vedtak.vedtakBegrunnelser.size == 0) {
            throw FunksjonellFeil(melding = "Vedtaket har ingen begrunnelser",
                                  frontendFeilmelding = "Vedtaket har ingen begrunnelser")
        }
    }

    fun lagVedtaksbrevFellesfelter(vedtak: Vedtak): VedtakFellesfelter {
        verifiserVedtakHarBegrunnelse(vedtak)
        val data = hentSøkerOgSignaturData(vedtak)
        return VedtakFellesfelter(
                enhet = data.enhet,
                saksbehandler = data.saksbehandler,
                beslutter = data.beslutter,
                hjemmeltekst = Hjemmeltekst(vedtak.hentHjemmelTekst()),
                søkerNavn = data.søker.navn,
                søkerFødselsnummer = data.søker.personIdent.ident,
                perioder = brevPeriodeService.hentBrevPerioder(vedtak),
        )
    }

    private fun hentEtterbetaling(vedtak: Vedtak): Etterbetaling? =
            hentEtterbetalingsbeløp(vedtak)?.let { Etterbetaling(it) }

    private fun hentEtterbetalingsbeløp(vedtak: Vedtak): String? =
            simuleringService.hentEtterbetaling(vedtak.behandling.id)
                    .takeIf { it > BigDecimal.ZERO }
                    ?.run { Utils.formaterBeløp(this.toInt()) }


    private fun erFeilutbetalingPåBehandling(behandlingId: Long): Boolean =
            simuleringService.hentFeilutbetaling(behandlingId) > BigDecimal.ZERO

    private fun hentSøkerOgSignaturData(vedtak: Vedtak): SøkerOgSignaturData {
        val personopplysningGrunnlag =
                persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)
                ?: throw Feil(message = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev",
                              frontendFeilmelding = "Finner ikke personopplysningsgrunnlag ved generering av vedtaksbrev")
        val (saksbehandler, beslutter) = hentSaksbehandlerOgBeslutter(
                behandling = vedtak.behandling,
                totrinnskontroll = totrinnskontrollService.hentAktivForBehandling(vedtak.behandling.id)
        )
        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id).behandlendeEnhetNavn
        return SøkerOgSignaturData(søker = personopplysningGrunnlag.søker,
                                   saksbehandler = saksbehandler,
                                   beslutter = beslutter,
                                   enhet = enhet
        )
    }

    private data class SøkerOgSignaturData(
            val søker: Person,
            val saksbehandler: String,
            val beslutter: String,
            val enhet: String
    )

}