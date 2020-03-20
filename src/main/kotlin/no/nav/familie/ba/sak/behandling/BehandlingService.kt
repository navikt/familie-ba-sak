package no.nav.familie.ba.sak.behandling

import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.steg.initSteg
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.domene.BeregningResultat
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.økonomi.OppdragId
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException
import java.time.LocalDate

@Service
class BehandlingService(private val behandlingRepository: BehandlingRepository,
                        private val persongrunnlagService: PersongrunnlagService,
                        private val beregningService: BeregningService,
                        private val fagsakService: FagsakService) {

    @Transactional
    fun opprettBehandling(nyBehandling: NyBehandling): Behandling {
        val fagsak = fagsakService.hent(personIdent = PersonIdent(nyBehandling.søkersIdent))
                     ?: error("Kan ikke lage behandling på person uten tilknyttet fagsak")

        val aktivBehandling = hentAktivForFagsak(fagsak.id)

        return if (aktivBehandling == null || aktivBehandling.status == BehandlingStatus.FERDIGSTILT) {
            lagreNyOgDeaktiverGammelBehandling(
                    Behandling(fagsak = fagsak,
                               journalpostID = nyBehandling.journalpostID,
                               type = nyBehandling.behandlingType,
                               kategori = nyBehandling.kategori,
                               underkategori = nyBehandling.underkategori,
                               steg = initSteg(nyBehandling.behandlingType)))
        } else if (aktivBehandling.steg < StegType.GODKJENNE_VEDTAK) {
            aktivBehandling.steg = initSteg(nyBehandling.behandlingType)
            aktivBehandling.status = BehandlingStatus.OPPRETTET
            behandlingRepository.save(aktivBehandling)
        } else {
            error("Kan ikke lage ny behandling. Fagsaken har en aktiv behandling som ikke er ferdigstilt.")
        }
    }

    fun settVilkårsvurdering(behandling: Behandling, resultat: BehandlingResultat, begrunnelse: String): Behandling {
        behandling.begrunnelse = begrunnelse
        behandling.resultat = resultat
        return lagre(behandling)
    }

    fun hentAktivForFagsak(fagsakId: Long): Behandling? {
        return behandlingRepository.findByFagsakAndAktiv(fagsakId)
    }

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.finnBehandling(behandlingId)
    }

    fun hentGjeldendeBehandlingerForLøpendeFagsaker(): List<OppdragId> {
        return fagsakService.hentLøpendeFagsaker()
                .flatMap { fagsak -> hentGjeldendeForFagsak(fagsak.id) }
                .map { behandling ->
                    OppdragId(
                            persongrunnlagService.hentSøker(behandling)!!.personIdent.ident,
                            behandling.id)
                }
    }

    fun hentBehandlinger(fagsakId: Long): List<Behandling> {
        return behandlingRepository.finnBehandlinger(fagsakId)
    }

    fun lagre(behandling: Behandling): Behandling {
        return behandlingRepository.save(behandling)
    }

    fun lagreNyOgDeaktiverGammelBehandling(behandling: Behandling): Behandling {
        val aktivBehandling = hentAktivForFagsak(behandling.fagsak.id)

        if (aktivBehandling != null) {
            behandlingRepository.saveAndFlush(aktivBehandling.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandler()} oppretter behandling $behandling")
        return behandlingRepository.save(behandling)
    }

    fun sendBehandlingTilBeslutter(behandling: Behandling) {
        oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.SENDT_TIL_BESLUTTER)
    }

    fun oppdaterStatusPåBehandling(behandlingId: Long, status: BehandlingStatus): Behandling {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        return behandlingRepository.save(behandling)
    }

    fun oppdaterStegPåBehandling(behandlingId: Long, steg: StegType) {
        val behandling = hent(behandlingId)
        LOG.info("${SikkerhetContext.hentSaksbehandler()} endrer steg på behandling $behandlingId fra ${behandling.steg} til $steg")

        behandling.steg = steg
        behandlingRepository.save(behandling)
    }

    fun oppdaterGjeldendeBehandlingForNesteUtbetaling(fagsakId: Long, utbetalingsMåned: LocalDate): List<Behandling> {
        val ferdigstilteBehandlinger = behandlingRepository.findByFagsakAndFerdigstiltOrIverksatt(fagsakId)

        val beregningResultater = ferdigstilteBehandlinger
                .sortedBy { it.opprettetTidspunkt }
                .map { beregningService.hentBeregningsresultatForBehandling(it.id) }

        beregningResultater.forEach {
            if (it.stønadTom >= utbetalingsMåned && it.stønadFom != null) {
                behandlingRepository.saveAndFlush(it.behandling.apply { gjeldendeForNesteUtbetaling = true })
            }
            if (it.opphørFom != null && it.opphørFom <= utbetalingsMåned) {
                val behandlingSomOpphører = hentBehandlingSomSkalOpphøres(it)
                behandlingRepository.saveAndFlush(behandlingSomOpphører.apply { gjeldendeForNesteUtbetaling = false })
            }
        }

        return hentGjeldendeForFagsak(fagsakId)
    }

    private fun hentBehandlingSomSkalOpphøres(beregningResultat: BeregningResultat): Behandling {
        val utbetalingsOppdrag = objectMapper.readValue(beregningResultat.utbetalingsoppdrag, Utbetalingsoppdrag::class.java)
        val opphørsPeriode = utbetalingsOppdrag.utbetalingsperiode.find { it.opphør != null } ?: throw IllegalArgumentException("Finner ikke opphør på beregningsresultat med id $beregningResultat.id")
        return behandlingRepository.finnBehandling(opphørsPeriode.behandlingId)
    }

    private fun hentGjeldendeForFagsak(fagsakId: Long): List<Behandling> {
        return behandlingRepository.findByFagsakAndGjeldendeForNesteUtbetaling(fagsakId)
    }

    companion object {
        val LOG: Logger = LoggerFactory.getLogger(BehandlingService::class.java)
    }
}
