package no.nav.familie.ba.sak.tilbakekreving

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlerRolle
import no.nav.familie.ba.sak.behandling.vedtak.Vedtak
import no.nav.familie.ba.sak.behandling.vedtak.VedtakRepository
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import no.nav.familie.ba.sak.simulering.SimuleringService
import no.nav.familie.ba.sak.simulering.vedtakSimuleringMottakereTilRestSimulering
import no.nav.familie.kontrakter.felles.tilbakekreving.*
import org.springframework.stereotype.Service

@Service
class TilbakekrevingService(
        private val tilbakekrevingRepository: TilbakekrevingRepository,
        private val vedtakRepository: VedtakRepository,
        private val simuleringService: SimuleringService,
        private val tilgangService: TilgangService,
        private val tilbakeRestClient: TilbakeRestClient,
        private val persongrunnlagService: PersongrunnlagService,
        private val arbeidsfordelingService: ArbeidsfordelingService,
) {

    fun validerRestTilbakekreving(restTilbakekreving: RestTilbakekreving?, vedtakId: Long) {
        tilgangService.verifiserHarTilgangTilHandling(minimumBehandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                                      handling = "opprette tilbakekreving")

        val simulering = simuleringService.hentSimuleringPåVedtak(vedtakId)
        val feilutbetaling = vedtakSimuleringMottakereTilRestSimulering(simulering).feilutbetaling
        validerVerdierPåRestTilbakekreving(restTilbakekreving, feilutbetaling)
    }

    fun lagreTilbakekreving(restTilbakekreving: RestTilbakekreving): Tilbakekreving? {
        val vedtak = vedtakRepository.finnVedtak(restTilbakekreving.vedtakId)
        vedtak.tilbakekreving = Tilbakekreving(
                begrunnelse = restTilbakekreving.begrunnelse,
                vedtak = vedtak,
                valg = restTilbakekreving.valg,
                varsel = restTilbakekreving.varsel,
                tilbakekrevingsbehandlingId = tilbakekrevingRepository.findByVedtakId(vedtak.id)?.tilbakekrevingsbehandlingId,
        )
        return vedtakRepository.save(vedtak).tilbakekreving
    }

    fun slettTilbakekrevingPåAktivtVedtak(behandlingId: Long) {
        val vedtak = vedtakRepository.findByBehandlingAndAktiv(behandlingId)
                     ?: throw Feil("Fant ikke vedtak for behandling $behandlingId ved sletting av tilbakekreving")
        vedtak.tilbakekreving = null
        vedtakRepository.save(vedtak)
    }

    fun søkerHarÅpenTilbakekreving(vedtakId: Long): Boolean = tilbakeRestClient.harÅpenTilbakekreingBehandling(vedtakId)

    fun opprettTilbakekreving(vedtak: Vedtak): TilbakekrevingId = tilbakeRestClient.opprettTilbakekrevingBehandling(
            lagOpprettTilbakekrevingRequest(vedtak))

    fun lagOpprettTilbakekrevingRequest(vedtak: Vedtak): OpprettTilbakekrevingRequest {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id) ?: throw Feil(
                message = "Finner ikke personopplysningsgrunnlag på vedtak ${vedtak.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake",
        )

        val språkkode = when (personopplysningGrunnlag.søker.målform) {
            Målform.NB -> Språkkode.NB
            Målform.NN -> Språkkode.NN
        }

        val enhet = arbeidsfordelingService.hentAbeidsfordelingPåBehandling(vedtak.behandling.id)

        val revurderingsvedtaksdato = vedtak.vedtaksdato?.toLocalDate() ?: throw Feil(
                message = "Finner ikke revurderingsvedtaksdato på vedtak ${vedtak.id} " +
                          "ved iverksetting av tilbakekreving mot familie-tilbake"
        )

        return OpprettTilbakekrevingRequest(
                fagsystem = Fagsystem.BA,
                ytelsestype = Ytelsestype.BARNETRYGD,
                eksternFagsakId = vedtak.behandling.fagsak.id.toString(),
                personIdent = personopplysningGrunnlag.søker.personIdent.ident,
                eksternId = vedtak.behandling.id.toString(),
                behandlingstype = Behandlingstype.TILBAKEKREVING,
                // Manuelt opprettet er per nå ikke håndtert i familie-tilbake.
                manueltOpprettet = false,
                språkkode = språkkode,
                enhetId = enhet.behandlendeEnhetId,
                enhetsnavn = enhet.behandlendeEnhetNavn,
                varsel = opprettVarsel(vedtak),
                revurderingsvedtaksdato = revurderingsvedtaksdato,
                // Verge er per nå ikke støttet i familie-ba-sak.
                verge = null,
                faktainfo = hentFaktainfoForTilbakekreving(vedtak),
        )
    }

    private fun opprettVarsel(vedtak: Vedtak): Varsel? {
        if (vedtak.tilbakekreving?.valg == Tilbakekrevingsvalg.OPPRETT_TILBAKEKREVING_MED_VARSEL) {
            val varseltekst = vedtak.tilbakekreving?.varsel ?: throw FunksjonellFeil("Varseltekst er ikke satt")
            val restSimulering =
                    vedtakSimuleringMottakereTilRestSimulering(simuleringService.hentSimuleringPåVedtak(vedtakId = vedtak.id))


            return Varsel(varseltekst = varseltekst,
                          sumFeilutbetaling = restSimulering.feilutbetaling,
                          perioder = slåsammenNærliggendeFeilutbtalingPerioder(restSimulering.perioder))
        }
        return null
    }

    fun hentFaktainfoForTilbakekreving(vedtak: Vedtak): Faktainfo =
            Faktainfo(
                    revurderingsårsak = vedtak.behandling.opprettetÅrsak.name,
                    revurderingsresultat = vedtak.behandling.resultat.name,
                    tilbakekrevingsvalg = vedtak.tilbakekreving?.valg,
                    konsekvensForYtelser = emptySet(),
            )
}