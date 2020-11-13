package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus.AVSLUTTET
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ba.sak.behandling.domene.tilstand.BehandlingStegTilstandRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.økonomi.OppdragIdForFagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
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
                        private val behandlingStegTilstandRepository: BehandlingStegTilstandRepository) {

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

            if (behandling.type == BehandlingType.TEKNISK_OPPHØR || behandling.opprettetÅrsak == BehandlingÅrsak.TEKNISK_OPPHØR) behandling.erTekniskOpphør()

            lagreNyOgDeaktiverGammelBehandling(behandling)
            loggService.opprettBehandlingLogg(behandling)
            loggBehandlinghendelse(behandling)
            behandling
        } else if (aktivBehandling.steg < StegType.BESLUTTE_VEDTAK) {
            aktivBehandling.leggTilBehandlingStegTilstand(initSteg(nyBehandling.behandlingType))
            aktivBehandling.status = initStatus()

            lagre(aktivBehandling)
        } else {
            throw FunksjonellFeil(melding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
                                  frontendFeilmelding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }
    }

    private fun loggBehandlinghendelse(behandling: Behandling) {
        saksstatistikkEventPublisher.publiserBehandlingsstatistikk(behandling.id,
                                                                   hentSisteBehandlingSomErIverksatt(behandling.fagsak.id)
                                                     .takeIf { erRevurderingKlageTekniskOpphør(behandling) }?.id)
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
            val henleggsteg = behandlingStegTilstandRepository.finnBehandlingStegTilstand(it.id)
                    .firstOrNull { behandlingStegTilstand ->
                        behandlingStegTilstand.behandlingSteg == StegType.HENLEGG_SØKNAD
                    }
            val tilkjentYtelsePåBehandling = beregningService.hentOptionalTilkjentYtelseForBehandling(it.id)
            henleggsteg == null && tilkjentYtelsePåBehandling != null && tilkjentYtelsePåBehandling.erSendtTilIverksetting()
        }
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

    fun leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId: Long, steg: StegType): Behandling {
        val behandling = hent(behandlingId)
        behandling.leggTilBehandlingStegTilstand(steg)

        return behandlingRepository.save(behandling)
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

    private fun erRevurderingKlageTekniskOpphør(behandling: Behandling) =
            behandling.type == BehandlingType.REVURDERING || behandling.type == BehandlingType.KLAGE || behandling.type == BehandlingType.TEKNISK_OPPHØR


    companion object {

        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
