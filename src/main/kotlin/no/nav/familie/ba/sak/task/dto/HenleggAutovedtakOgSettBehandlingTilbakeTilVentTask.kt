package no.nav.familie.ba.sak.task.dto

import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task

class HenleggAutovedtakOgSettBehandlingTilbakeTilVentTask(
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
    private val påVentService: SettPåVentService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val opprettTaskService: OpprettTaskService,
    private val loggService: LoggService,
) {
    fun doTask(task: Task) {
        val meldingIOppgave = "Småbarnstillegg: endring i overgangsstønad må behandles manuelt"
        val behandlingId = objectMapper.readValue(task.payload, Long::class.java)
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)

        val behandlingPåMaskinellVent =
            behandlingHentOgPersisterService.hentBehandlinger(behandling.fagsak.id, BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
                .singleOrNull()

        if (behandlingPåMaskinellVent != null && påVentService.finnAktivSettPåVentPåBehandling(behandlingPåMaskinellVent.id) != null) {
            behandlingPåMaskinellVent.status = BehandlingStatus.SATT_PÅ_VENT
            behandlingHentOgPersisterService.lagreEllerOppdater(behandlingPåMaskinellVent)

            henleggBehandlingOgOpprettOppgaveForBehandlingPåMaskinellVent(behandlingPåMaskinellVent, behandling, meldingIOppgave)
        }
    }

    private fun henleggBehandlingOgOpprettOppgaveForBehandlingPåMaskinellVent(
        behandlingPåMaskinellVent: Behandling,
        behandling: Behandling,
        meldingIOppgave: String,
    ): String {
        opprettTaskService.opprettHenleggBehandlingTask(
            behandlingId = behandling.id,
            årsak = HenleggÅrsak.TEKNISK_VEDLIKEHOLD,
            begrunnelse = meldingIOppgave,
        )

        AutovedtakSmåbarnstilleggService.logger.info("Sender autovedtak til manuell behandling, se secureLogger for mer detaljer.")
        secureLogger.info("Sender autovedtak til manuell behandling. Begrunnelse: $meldingIOppgave")
        opprettTaskService.opprettOppgaveForManuellBehandlingTask(
            behandlingId = behandlingPåMaskinellVent.id,
            beskrivelse = meldingIOppgave,
            manuellOppgaveType = ManuellOppgaveType.SMÅBARNSTILLEGG,
        )

        loggService.opprettAutovedtakTilManuellBehandling(
            behandling = behandling,
            tekst = meldingIOppgave,
        )

        return meldingIOppgave
    }

    companion object {
        const val TASK_STEP_TYPE = "HenleggAutovedtakOgSettBehandlingTilbakeTilVentTask"
    }
}
