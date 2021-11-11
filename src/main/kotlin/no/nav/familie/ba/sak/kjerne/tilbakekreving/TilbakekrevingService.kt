package no.nav.familie.ba.sak.kjerne.tilbakekreving

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestTilbakekreving
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.steg.BehandlerRolle
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.Tilbakekreving
import no.nav.familie.ba.sak.kjerne.tilbakekreving.domene.TilbakekrevingRepository
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingstype
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettManueltTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.OpprettTilbakekrevingRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TilbakekrevingService(
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val vedtakRepository: VedtakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val totrinnskontrollRepository: TotrinnskontrollRepository,
    private val simuleringService: SimuleringService,
    private val tilgangService: TilgangService,
    private val persongrunnlagService: PersongrunnlagService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val tilbakekrevingKlient: TilbakekrevingKlient,
) {

    fun validerRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, behandlingId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
            handling = "opprette tilbakekreving"
        )

        val feilutbetaling = simuleringService.hentFeilutbetaling(behandlingId)
        validerVerdierPåRestTilbakekreving(restTilbakekreving, feilutbetaling)
    }

    @Transactional
    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving, behandlingId: Long): Tilbakekreving? {
        val behandling = behandlingRepository.finnBehandling(behandlingId)

        val tilbakekreving = Tilbakekreving(
            begrunnelse = restTilbakekreving.begrunnelse,
            behandling = behandling,
            valg = restTilbakekreving.valg,
            varsel = restTilbakekreving.varsel,
            tilbakekrevingsbehandlingId = tilbakekrevingRepository
                .findByBehandlingId(behandling.id)?.tilbakekrevingsbehandlingId,
        )

        tilbakekrevingRepository.deleteByBehandlingId(behandlingId)
        return tilbakekrevingRepository.save(tilbakekreving)
    }

    fun hentTilbakekrevingsvalg(behandlingId: Long): Tilbakekrevingsvalg? {
        return tilbakekrevingRepository.findByBehandlingId(behandlingId)?.valg
    }

    fun slettTilbakekrevingPåBehandling(behandlingId: Long) =
        tilbakekrevingRepository.findByBehandlingId(behandlingId)?.let { tilbakekrevingRepository.delete(it) }

    fun hentForhåndsvisningVarselbrev(
        behandlingId: Long,
        forhåndsvisTilbakekrevingsvarselbrevRequest: ForhåndsvisTilbakekrevingsvarselbrevRequest,
    ): ByteArray {
        tilgangService.verifiserHarTilgangTilHandling(
            minimumBehandlerRolle = BehandlerRolle.VEILEDER,
            handling = "hent forhåndsvisning av varselbrev for tilbakekreving"
        )

        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)
            ?: throw Feil(
                "Fant ikke vedtak for behandling $behandlingId ved forhåndsvisning av varselbrev" +
                    " for tilbakekreving."
            )

        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId)
            ?: throw Feil(
                "Fant ikke aktivt persongrunnlag ved forhåndsvisning av varselbrev" +
                    " for tilbakekreving."
            )
        val arbeidsfordeling = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandlingId)

        return tilbakekrevingKlient.hentForhåndsvisningVarselbrev(
            forhåndsvisVarselbrevRequest = ForhåndsvisVarselbrevRequest(
                varseltekst = forhåndsvisTilbakekrevingsvarselbrevRequest.fritekst,
                ytelsestype = Ytelsestype.BARNETRYGD,
                behandlendeEnhetId = arbeidsfordeling.behandlendeEnhetId,
                behandlendeEnhetsNavn = arbeidsfordeling.behandlendeEnhetNavn,
                språkkode = persongrunnlag.søker.målform.tilSpråkkode(),
                feilutbetaltePerioderDto = FeilutbetaltePerioderDto(
                    sumFeilutbetaling = simuleringService.hentFeilutbetaling(behandlingId).toLong(),
                    perioder = hentTilbakekrevingsperioderISimulering(
                        simuleringService.hentSimuleringPåBehandling(behandlingId)
                    )
                ),
                fagsystem = Fagsystem.BA,
                eksternFagsakId = vedtak.behandling.fagsak.id.toString(),
                ident = persongrunnlag.søker.personIdent.ident,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerNavn()
            )
        )
    }

    fun søkerHarÅpenTilbakekreving(fagsakId: Long): Boolean =
        tilbakekrevingKlient.harÅpenTilbakekrevingsbehandling(fagsakId)

    fun opprettTilbakekreving(behandling: Behandling): TilbakekrevingId =
        tilbakekrevingKlient.opprettTilbakekrevingBehandling(lagOpprettTilbakekrevingRequest(behandling))

    fun lagOpprettTilbakekrevingRequest(behandling: Behandling): OpprettTilbakekrevingRequest {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id) ?: throw Feil(
            message = "Finner ikke personopplysningsgrunnlag på vedtak ${behandling.id} " +
                "ved iverksetting av tilbakekreving mot familie-tilbake",
        )

        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(behandling.id)

        val aktivtVedtak = vedtakRepository.findByBehandlingAndAktiv(behandling.id)
            ?: throw Feil("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        val totrinnskontroll = totrinnskontrollRepository.findByBehandlingAndAktiv(behandling.id)

        val revurderingsvedtaksdato = aktivtVedtak.vedtaksdato?.toLocalDate() ?: throw Feil(
            message = "Finner ikke revurderingsvedtaksdato på vedtak ${aktivtVedtak.id} " +
                "ved iverksetting av tilbakekreving mot familie-tilbake"
        )

        val tilbakekreving = tilbakekrevingRepository.findByBehandlingId(behandling.id)
            ?: throw Feil("Fant ikke tilbakekreving på behandling ${behandling.id}")

        return OpprettTilbakekrevingRequest(
            fagsystem = Fagsystem.BA,
            ytelsestype = Ytelsestype.BARNETRYGD,
            eksternFagsakId = behandling.fagsak.id.toString(),
            personIdent = personopplysningGrunnlag.søker.personIdent.ident,
            eksternId = behandling.id.toString(),
            behandlingstype = Behandlingstype.TILBAKEKREVING,
            // Manuelt opprettet er per nå ikke håndtert i familie-tilbake.
            manueltOpprettet = false,
            språkkode = personopplysningGrunnlag.søker.målform.tilSpråkkode(),
            enhetId = enhet.behandlendeEnhetId,
            enhetsnavn = enhet.behandlendeEnhetNavn,
            saksbehandlerIdent = totrinnskontroll?.saksbehandlerId ?: SikkerhetContext.hentSaksbehandler(),
            varsel = opprettVarsel(tilbakekreving, simuleringService.hentSimuleringPåBehandling(behandling.id)),
            revurderingsvedtaksdato = revurderingsvedtaksdato,
            // Verge er per nå ikke støttet i familie-ba-sak.
            verge = null,
            faktainfo = hentFaktainfoForTilbakekreving(behandling, tilbakekreving),
        )
    }

    fun opprettTilbakekrevingsbehandlingManuelt(fagsakId: Long): Ressurs<String> {
        val kanOpprettesRespons = tilbakekrevingKlient.kanTilbakekrevingsbehandlingOpprettesManuelt(fagsakId)
        if (!kanOpprettesRespons.kanBehandlingOpprettes) {
            return Ressurs.funksjonellFeil(
                frontendFeilmelding = kanOpprettesRespons.melding,
                melding = "familie-tilbake svarte nei på om tilbakekreving kunne opprettes"
            )
        }

        val behandling = kanOpprettesRespons.kravgrunnlagsreferanse?.toLong()
            ?.let { behandlingRepository.findByFagsakAndAvsluttet(fagsakId).find { beh -> beh.id == it } }
        return if (behandling != null) {
            tilbakekrevingKlient.opprettTilbakekrevingsbehandlingManuelt(
                OpprettManueltTilbakekrevingRequest(
                    eksternFagsakId = fagsakId.toString(),
                    ytelsestype = Ytelsestype.BARNETRYGD,
                    eksternId = kanOpprettesRespons.kravgrunnlagsreferanse!!
                )
            )

            Ressurs.success("Tilbakekreving opprettet")
        } else {
            logger.error("Kan ikke opprette tilbakekrevingsbehandling. Respons inneholder referanse til en ukjent behandling")
            Ressurs.funksjonellFeil(
                melding = "Kan ikke opprette tilbakekrevingsbehandling. Respons inneholder referanse til en ukjent behandling",
                frontendFeilmelding = "Av tekniske årsaker så kan ikke behandling opprettes. Kontakt brukerstøtte for å rapportere feilen."
            )
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(TilbakekrevingService::class.java)
    }
}
