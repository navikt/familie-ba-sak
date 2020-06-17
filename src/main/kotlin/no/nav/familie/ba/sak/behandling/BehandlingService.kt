package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.fagsak.FagsakPersonRepository
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
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
                        private val fagsakPersonRepository: FagsakPersonRepository,
                        private val persongrunnlagService: PersongrunnlagService,
                        private val beregningService: BeregningService,
                        private val fagsakService: FagsakService) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakPersonRepository.finnFagsak(setOf(PersonIdent(nyBehandling.søkersIdent)))
                     ?: throw Feil(message = "Kan ikke lage behandling på person uten tilknyttet fagsak",
                                   frontendFeilmelding = "Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        // TODO: journalbehandling til å ha en liste av journalpostenr
        return if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.FERDIGSTILT) {
            lagreNyOgDeaktiverGammelBehandling(
                    Behandling(fagsak = fagsak,
                               opprinnelse = nyBehandling.behandlingOpprinnelse,
                               journalpostID = nyBehandling.journalpostID,
                               type = nyBehandling.behandlingType,
                               kategori = nyBehandling.kategori,
                               underkategori = nyBehandling.underkategori,
                               steg = initSteg(nyBehandling.behandlingType, null)))
        } else if (aktivBehandling.steg < StegType.BESLUTTE_VEDTAK) {
            aktivBehandling.steg = initSteg(nyBehandling.behandlingType, null)
            aktivBehandling.status = BehandlingStatus.OPPRETTET
            behandlingRepository.save(aktivBehandling)
        } else {
            throw Feil(message = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.",
                       frontendFeilmelding = "Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }
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

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.SENDT_TIL_BESLUTTER)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus): Behandling {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        return behandlingRepository.save(behandling)
    }

    fun oppdaterStegPåBehandling(behandlingId: Long, steg: StegType): Behandling {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer steg på behandling $behandlingId fra ${behandling.steg} til $steg")

        behandling.steg = steg
        return behandlingRepository.save(behandling)
    }

    fun oppdaterGjeldendeBehandlingForFremtidigUtbetaling(fagsakId: Long, utbetalingsMåned: LocalDate): List<Behandling> {
        val ferdigstilteBehandlinger = behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(fagsakId)

        val tilkjenteYtelser = ferdigstilteBehandlinger
                .sortedBy { it.opprettetTidspunkt }
                .map { beregningService.hentTilkjentYtelseForBehandling(it.id) }

        tilkjenteYtelser.forEach {
            if (it.stønadTom!! >= utbetalingsMåned && it.stønadFom != null) {
                behandlingRepository.saveAndFlush(it.behandling.apply { gjeldendeForUtbetaling = true })
            }
            if (it.opphørFom != null && it.opphørFom!! <= utbetalingsMåned) {
                val behandlingSomOpphører = hentBehandlingSomSkalOpphøres(it)
                behandlingRepository.saveAndFlush(behandlingSomOpphører.apply { gjeldendeForUtbetaling = false })
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

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
