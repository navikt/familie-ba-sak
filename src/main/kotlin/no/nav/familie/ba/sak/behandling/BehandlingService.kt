package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstand
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.BehandlingStegStatus
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatService
import no.nav.familie.ba.sak.behandling.vilkår.BehandlingResultatType
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.økonomi.OppdragIdForFagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val behandlingMetrikker: BehandlingMetrikker,
                        private val fagsakPersonRepository: FagsakPersonRepository,
                        private val persongrunnlagService: PersongrunnlagService,
                        private val beregningService: BeregningService,
                        private val fagsakService: FagsakService,
                        private val loggService: LoggService,
                        private val arbeidsfordelingService: ArbeidsfordelingService,
                        private val saksstatistikkEventPublisher: SaksstatistikkEventPublisher,
                        private val taskRepository: TaskRepository,
                        private val behandlingResultatService: BehandlingResultatService) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakPersonRepository.finnFagsak(setOf(PersonIdent(nyBehandling.søkersIdent)))
                     ?: throw FunksjonellFeil(melding = "Kan ikke lage behandling på person uten tilknyttet fagsak",
                                              frontendFeilmelding = "Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        return if (aktivBehandling == null || aktivBehandling.status == AVSLUTTET) {
            val behandling = Behandling(fagsak = fagsak,
                                        behandlingStegTilstand = mutableListOf(),
                                        opprettetÅrsak = nyBehandling.behandlingÅrsak,
                                        type = nyBehandling.behandlingType,
                                        kategori = nyBehandling.kategori,
                                        underkategori = nyBehandling.underkategori,
                                        skalBehandlesAutomatisk = nyBehandling.skalBehandlesAutomatisk,
                                        steg = initSteg(nyBehandling.behandlingType))

            behandling.behandlingStegTilstand.add(BehandlingStegTilstand(0, behandling, initSteg(nyBehandling.behandlingType)))

            lagreNyOgDeaktiverGammelBehandling(behandling)
            loggService.opprettBehandlingLogg(behandling)
            loggBehandlinghendelse(behandling)
            behandling
        } else if (aktivBehandling.steg < StegType.BESLUTTE_VEDTAK) {
            aktivBehandling.steg = initSteg(nyBehandling.behandlingType)
            aktivBehandling.status = initStatus()

            lagre(aktivBehandling)
        } else {
            throw FunksjonellFeil(melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
                                  frontendFeilmelding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }
    }

    @Transactional
    fun henleggBehandling(behandlingId: Long): Behandling {
        oppdaterAktivBehandlingsResultatPåBehandling(behandlingId)
        oppdaterStegPåBehandling(behandlingId, StegType.FERDIGSTILLE_BEHANDLING)

        //TODO: Trenger man hente personIdent når den ikke blir brukt?
        taskRepository.save(FerdigstillBehandlingTask.opprettTask(personIdent = "", behandlingsId = behandlingId))

        return hent(behandlingId)
    }

    private fun loggBehandlinghendelse(behandling: Behandling) {
        saksstatistikkEventPublisher.publish(behandling.id,
                                             hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
                                                     .takeIf { erRevurderingEllerKlage(behandling) }?.id)
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentGjeldendeBehandlingerForLøpendeFagsaker(): List<OppdragIdForFagsystem> {
        return fagsakService.hentLøpendeFagsaker()
                .flatMap { fagsak -> hentGjeldendeForFagsak(fagsak.id) }
                .map { behandling ->
                    OppdragIdForFagsystem(
                            persongrunnlagService.hentSøker(behandling)!!.personIdent.ident,
                            behandling.id)
                }
    }

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    private fun hentIverksatteBehandlinger(fagsakId: Long): List<Behandling> {
        return hentBehandlinger(fagsakId).filter {
            val tilkjentYtelsePåBehandling = beregningService.hentOptionalTilkjentYtelseForBehandling(it.id)
            tilkjentYtelsePåBehandling != null && tilkjentYtelsePåBehandling.erSendtTilIverksetting()
        }
    }

    fun hentSisteBehandlingSomErIverksatt(fagsakId: Long): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return Behandlingutils.hentSisteBehandlingSomErIverksatt(iverksatteBehandlinger)
    }

    fun hentForrigeBehandlingSomErIverksatt(fagsakId: Long, behandlingFørFølgende: Behandling): Behandling? {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)
        return Behandlingutils.hentForrigeIverksatteBehandling(iverksatteBehandlinger, behandlingFørFølgende)
    }

    fun lagre(behandling: Behandling): Behandling {
        return behandlingRepository.save(behandling)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling).also {
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
        return behandlingRepository.save(behandling).also { loggBehandlinghendelse(behandling) }
    }

    fun oppdaterStegPåBehandling(behandlingId: Long, steg: StegType): Behandling {
        val behandling = hent(behandlingId)
        val sisteBehandlingStegTilstand =
                behandling.behandlingStegTilstand.filter { it.behandlingStegStatus != BehandlingStegStatus.UTFØRT }.first()

        sisteBehandlingStegTilstand.behandlingStegStatus = BehandlingStegStatus.UTFØRT
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(behandling = behandling, behandlingSteg = steg))

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer siste steg på behandling $behandlingId fra ${sisteBehandlingStegTilstand.behandlingSteg} til $steg")
        // TODO: Oppdatreing av steg direkte på behandling skal fjernes når frontendkoden for å håntere behandlingsstegtilgang er klar,
        //       inkludert migrering av tidligere behandlinger som ikke har relaterte behandlingsstegtilgang.
        behandling.steg = steg
        behandling.behandlingStegTilstand.add(BehandlingStegTilstand(behandling = behandling, behandlingSteg = steg))
        return behandlingRepository.save(behandling)
    }

    private fun oppdaterAktivBehandlingsResultatPåBehandling(behandlingId: Long) {
        behandlingResultatService.hentAktivForBehandling(behandlingId)
                ?.also { it.samletResultat = BehandlingResultatType.HENLAGT }
                ?.let { behandlingResultatService.oppdater(it) }
    }

    fun oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsakId: Long, utbetalingsmåned: LocalDate): List<Behandling> {
        val iverksatteBehandlinger = hentIverksatteBehandlinger(fagsakId)

        val tilkjenteYtelser = iverksatteBehandlinger
                .sortedBy { it.opprettetTidspunkt }
                .map { beregningService.hentTilkjentYtelseForBehandling(it.id) }

        tilkjenteYtelser.forEach {
            if (it.erLøpende(utbetalingsmåned)) {
                behandlingRepository.saveAndFlush(it.behandling.apply { gjeldendeForFremtidigUtbetaling = true })
            } else if (it.erUtløpt(utbetalingsmåned)) {
                behandlingRepository.saveAndFlush(it.behandling.apply { gjeldendeForFremtidigUtbetaling = false })
            }

            if (it.harOpphørPåTidligereBehandling(utbetalingsmåned)) {
                val behandlingSomOpphører = hentBehandlingSomSkalOpphøres(it)
                behandlingRepository.saveAndFlush(behandlingSomOpphører.apply { gjeldendeForFremtidigUtbetaling = false })
            }
        }

        return hentGjeldendeForFagsak(fagsakId)
    }

    private fun hentBehandlingSomSkalOpphøres(tilkjentYtelse: TilkjentYtelse): Behandling {
        val utbetalingsOppdrag = objectMapper.readValue(tilkjentYtelse.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)
        val perioderMedOpphør = utbetalingsOppdrag.utbetalingsperiode.filter { it.opphør != null }
        val opphørsperiode = perioderMedOpphør.firstOrNull()
                             ?: throw IllegalArgumentException("Finner ikke opphør på tilkjent ytelse med id $tilkjentYtelse.id")
        if (perioderMedOpphør.any { it.behandlingId != opphørsperiode.behandlingId }) {
            throw IllegalArgumentException("Alle utbetalingsperioder med opphør må ha samme behandlingId")
        }
        return behandlingRepository.finnBehandling(opphørsperiode.behandlingId)
    }

    private fun hentGjeldendeForFagsak(fagsakId: Long): List<Behandling> {
        return behandlingRepository.findByFagsakAndGjeldendeForUtbetaling(fagsakId)
    }

    private fun erRevurderingEllerKlage(behandling: Behandling) =
            behandling.type == BehandlingType.REVURDERING || behandling.type == BehandlingType.KLAGE


    companion object {

        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
