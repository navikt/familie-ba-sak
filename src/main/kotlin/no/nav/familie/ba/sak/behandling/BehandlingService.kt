package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.domene.initStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.steg.FØRSTE_STEG
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.oppgave.OppgaveService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val behandlingMetrikker: BehandlingMetrikker,
                        private val fagsakPersonRepository: FagsakPersonRepository,
                        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                        private val loggService: LoggService,
                        private val arbeidsfordelingService: ArbeidsfordelingService,
                        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
                        private val oppgaveService: OppgaveService) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakPersonRepository.finnFagsak(setOf(PersonIdent(nyBehandling.søkersIdent)))
                     ?: throw FunksjonellFeil(melding = "Kan ikke lage behandling på person uten tilknyttet fagsak",
                                              frontendFeilmelding = "Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        return if (aktivBehandling == null || aktivBehandling.status == AVSLUTTET) {
            val behandling = Behandling(fagsak = fagsak,
                                        opprettetÅrsak = nyBehandling.behandlingÅrsak,
                                        type = nyBehandling.behandlingType,
                                        kategori = nyBehandling.kategori,
                                        underkategori = nyBehandling.underkategori,
                                        skalBehandlesAutomatisk = nyBehandling.skalBehandlesAutomatisk)
                    .initBehandlingStegTilstand()

            behandling.erTekniskOpphør() // Sjekker om teknisk opphør og kaster feil dersom BehandlingType og BehandlingÅrsak ikke samsvarer på eventuelt teknisk opphør

            val lagretBehandling = lagreNyOgDeaktiverGammelBehandling(behandling)
            loggService.opprettBehandlingLogg(lagretBehandling)
            loggBehandlinghendelse(lagretBehandling)

            if (lagretBehandling.opprettBehandleSakOppgave()) {
                oppgaveService.opprettOppgave(behandlingId = lagretBehandling.id,
                                              oppgavetype = Oppgavetype.BehandleSak,
                                              fristForFerdigstillelse = LocalDate.now(),
                                              tilordnetNavIdent = nyBehandling.navIdent)
            }

            lagretBehandling
        } else if (aktivBehandling.steg < StegType.BESLUTTE_VEDTAK) {
            aktivBehandling.leggTilBehandlingStegTilstand(FØRSTE_STEG)
            aktivBehandling.status = initStatus()

            lagreEllerOppdater(aktivBehandling)
        } else {
            throw FunksjonellFeil(melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
                                  frontendFeilmelding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }
    }

    private fun loggBehandlinghendelse(behandling: Behandling) {
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id,
                                                                   hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
                                                                           .takeIf { erRevurderingEllerTekniskOpphør(behandling) }?.id)
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentSisteIverksatteBehandlingerFraLøpendeFagsaker(): List<Long> = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker()

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    private fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> {
        return Behandlingutils.hentIverksatteBehandlinger(hentBehandlinger(fagsakId), tilkjentYtelseRepository)
    }

    /**
     * Henter siste iverksatte behandling på fagsak
     */
    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
    }

    /**
     * Henter siste iverksatte behandling FØR en gitt behandling
     */
    fun hentForrigeBehandlingSomErIverksatt(behandling: Behandling): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(behandling.fagsak.id)
        return Behandlingutils.hentForrigeIverksatteBehandling(iverksatteBehandlinger, behandling)
    }

    fun lagreEllerOppdater(behandling: Behandling): Behandling {
        return behandlingRepository.save(behandling)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return lagreEllerOppdater(behandling).also {
            arbeidsfordelingService.fastsettBehandlendeEnhet(it)

            if (it.versjon == 0L) {
                behandlingMetrikker.tellNøkkelTallVedOpprettelseAvBehandling(it)
            }
        }
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = FATTER_VEDTAK)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus): Behandling {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        return lagreEllerOppdater(behandling).also { loggBehandlinghendelse(behandling) }
    }

    fun oppdaterResultatPåBehandling(behandlingId: Long, resultat: BehandlingResultat): Behandling {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer resultat på behandling $behandlingId fra ${behandling.resultat} til $resultat")

        if (!behandling.skalBehandlesAutomatisk && !resultat.erStøttetIManuellBehandling) {
            throw FunksjonellFeil(frontendFeilmelding = "Behandlingsresultatet ${resultat.displayName.toLowerCase()} er ikke støttet i løsningen enda. Ta kontakt med Team familie om du er uenig i resultatet.",
                                  melding = "Behandlingsresultatet ${resultat.displayName.toLowerCase()} er ikke støttet i løsningen, se securelogger for resultatene som ble utledet.")
        }

        loggService.opprettVilkårsvurderingLogg(behandling = behandling,
                                                forrigeBehandlingResultat = behandling.resultat,
                                                nyttBehandlingResultat = resultat)

        behandling.resultat = resultat
        return lagreEllerOppdater(behandling)
    }

    fun leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId: Long, steg: StegType): Behandling {
        val behandling = hent(behandlingId)
        behandling.leggTilBehandlingStegTilstand(steg)

        return lagreEllerOppdater(behandling)
    }

    private fun erRevurderingEllerTekniskOpphør(behandling: Behandling) =
            behandling.type == BehandlingType.REVURDERING || behandling.type == BehandlingType.TEKNISK_OPPHØR

    companion object {

        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
