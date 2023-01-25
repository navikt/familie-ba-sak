package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendring.domene.SatskjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.SatsType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AutovedtakSatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
    private val satskjøringRepository: SatskjøringRepository
) : AutovedtakBehandlingService<Long> {

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     */
    @Transactional
    override fun kjørBehandling(fagsakId: Long): String {
        val satskjøringForFagsak =
            satskjøringRepository.findByFagsakId(fagsakId) ?: error("Fant ingen satskjøringsrad for fagsak=$fagsakId")

        val behandling = behandlingRepository.finnSisteIverksatteBehandling(fagsakId = fagsakId)
            ?: error("Fant ikke siste iverksette behandling for $fagsakId")

        if (harAlleredeNySats(behandling.id)) {
            satskjøringForFagsak.ferdigTidspunkt
            satskjøringRepository.save(satskjøringForFagsak)
            logger.info("Satsendring allerede utført fagsak=$fagsakId")
            return "Satsendring allerede utført fagsak=$fagsakId"
        }

        val aktivOgÅpenBehandling = behandlingRepository.findByFagsakAndAktivAndOpen(fagsakId = behandling.fagsak.id)
        val søkerAktør = behandling.fagsak.aktør

        logger.info("Kjører satsendring på $behandling")
        secureLogger.info("Kjører satsendring på $behandling for ${søkerAktør.aktivFødselsnummer()}")
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")

        if (aktivOgÅpenBehandling != null) {
            val brukerHarÅpenBehandlingMelding = if (harAlleredeNySats(behandlingId = aktivOgÅpenBehandling.id)) {
                "Åpen behandling har allerede siste sats og vi lar den ligge."
            } else if (aktivOgÅpenBehandling.status.erLåstMenIkkeAvsluttet()) {
                "Behandling $aktivOgÅpenBehandling er låst for endringer og satsendring vil bli trigget neste virkedag."
            } else if (aktivOgÅpenBehandling.steg.rekkefølge > StegType.VILKÅRSVURDERING.rekkefølge) {
                tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(aktivOgÅpenBehandling)
                "Tilbakestiller behandling $aktivOgÅpenBehandling til vilkårsvurderingen"
            } else {
                "Behandling $aktivOgÅpenBehandling er under utredning, men er allerede i riktig tilstand."
            }

            logger.info(brukerHarÅpenBehandlingMelding)
            return brukerHarÅpenBehandlingMelding
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = søkerAktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING,
                fagsakId = behandling.fagsak.id
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        val task = IverksettMotOppdragTask.opprettTask(
            behandlingEtterBehandlingsresultat,
            opprettetVedtak,
            SikkerhetContext.hentSaksbehandler()
        )

        satskjøringForFagsak.ferdigTidspunkt = LocalDateTime.now()
        satskjøringRepository.save(satskjøringForFagsak)
        taskRepository.save(task)

        return "Satsendring kjørt OK"
    }

    private fun harAlleredeNySats(behandlingId: Long): Boolean {
        val andeler =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandlingId)

        return andeler.any {
            it.sats == SatsService.finnSisteSatsFor(SatsType.ORBA).beløp
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AutovedtakSatsendringService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
