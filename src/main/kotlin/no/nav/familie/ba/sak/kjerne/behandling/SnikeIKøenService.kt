package no.nav.familie.ba.sak.kjerne.behandling

import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.settpåvent.SettPåVentService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.steg.TilbakestillBehandlingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SnikeIKøenService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val påVentService: SettPåVentService,
    private val loggService: LoggService,
    private val tilbakestillBehandlingService: TilbakestillBehandlingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun settAktivBehandlingPåMaskinellVent(
        behandlingId: Long,
        årsak: SettPåMaskinellVentÅrsak,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (!behandling.aktiv) {
            error("Behandling=$behandlingId er ikke aktiv")
        }
        val behandlingStatus = behandling.status
        if (behandlingStatus !== BehandlingStatus.UTREDES && behandlingStatus !== BehandlingStatus.SATT_PÅ_VENT) {
            error("Behandling=$behandlingId kan ikke settes på maskinell vent då status=$behandlingStatus")
        }
        behandling.status = BehandlingStatus.SATT_PÅ_MASKINELL_VENT
        behandling.aktiv = false
        behandlingHentOgPersisterService.lagreOgFlush(behandling)
        loggService.opprettSettPåMaskinellVent(behandling, årsak.årsak)
    }

    /**
     * @param behandlingSomFerdigstilles er behandlingen som ferdigstilles i [no.nav.familie.ba.sak.kjerne.steg.FerdigstillBehandling]
     *  Den er mest brukt for å logge hvilken behandling det er som ferdigstilles og hvilken som blir deaktivert
     *
     * @return boolean som tilsier om en behandling er reaktivert eller ikke
     */
    @Transactional
    fun reaktiverBehandlingPåMaskinellVent(behandlingSomFerdigstilles: Behandling): Boolean {
        val fagsakId = behandlingSomFerdigstilles.fagsak.id

        val behandlingPåVent = finnBehandlingPåMaskinellVent(fagsakId) ?: return false
        val aktivBehandling = behandlingHentOgPersisterService.finnAktivForFagsak(fagsakId)

        validerBehandlinger(aktivBehandling, behandlingPåVent)

        aktiverBehandlingPåVent(aktivBehandling, behandlingPåVent, behandlingSomFerdigstilles)
        if (behandlingPåVent.harVærtPåVilkårsvurderingSteg()) {
            tilbakestillBehandlingService.tilbakestillBehandlingTilVilkårsvurdering(behandlingPåVent)
        }
        loggService.opprettTattAvMaskinellVent(behandlingPåVent)
        return true
    }

    fun kanSnikeForbi(aktivOgÅpenBehandling: Behandling): Boolean {
        val behandlingId = aktivOgÅpenBehandling.id
        val loggSuffix = "endrer status på behandling til på vent"
        if (aktivOgÅpenBehandling.status == BehandlingStatus.SATT_PÅ_VENT) {
            logger.info("Behandling=$behandlingId er satt på vent av saksbehandler, $loggSuffix")
            return true
        }
        val tid4TimerSiden = LocalDateTime.now().minusHours(4)
        if (aktivOgÅpenBehandling.endretTidspunkt.isAfter(tid4TimerSiden)) {
            logger.info(
                "Behandling=$behandlingId har endretTid=${aktivOgÅpenBehandling.endretTidspunkt}. " +
                    "Det er altså mindre enn 4 timer siden behandlingen var endret, og vi ønsker derfor ikke å sette behandlingen på maskinell vent",
            )
            return false
        }
        val sisteLogghendelse = loggService.hentLoggForBehandling(behandlingId).maxBy { it.opprettetTidspunkt }
        if (sisteLogghendelse.opprettetTidspunkt.isAfter(tid4TimerSiden)) {
            logger.info(
                "Behandling=$behandlingId siste logginslag er " +
                    "type=${sisteLogghendelse.type} tid=${sisteLogghendelse.opprettetTidspunkt}, $loggSuffix. " +
                    "Det er altså mindre enn 4 timer siden siste logginslag, og vi ønsker derfor ikke å sette behandlingen på maskinell vent",
            )
            return false
        }
        return true
    }

    private fun finnBehandlingPåMaskinellVent(
        fagsakId: Long,
    ): Behandling? =
        behandlingHentOgPersisterService.hentBehandlinger(fagsakId, BehandlingStatus.SATT_PÅ_MASKINELL_VENT)
            .takeIf { it.isNotEmpty() }
            ?.let { it.singleOrNull() ?: error("Forventer kun en behandling på vent for fagsak=$fagsakId") }

    private fun aktiverBehandlingPåVent(
        aktivBehandling: Behandling?,
        behandlingPåVent: Behandling,
        behandlingSomFerdigstilles: Behandling,
    ) {
        logger.info(
            "Deaktiverer aktivBehandling=${aktivBehandling?.id}" +
                " aktiverer behandlingPåVent=${behandlingPåVent.id}" +
                " behandlingSomFerdigstilles=${behandlingSomFerdigstilles.id}",
        )

        if (aktivBehandling != null) {
            aktivBehandling.aktiv = false
            behandlingHentOgPersisterService.lagreOgFlush(aktivBehandling)
        }

        behandlingPåVent.aktiv = true
        behandlingPåVent.aktivertTidspunkt = LocalDateTime.now()
        behandlingPåVent.status = utledStatusForBehandlingPåVent(behandlingPåVent)

        behandlingHentOgPersisterService.lagreOgFlush(behandlingPåVent)
    }

    private fun validerBehandlinger(
        aktivBehandling: Behandling?,
        behandlingPåVent: Behandling,
    ) {
        if (behandlingPåVent.aktiv) {
            error("Åpen behandling har feil tilstand $behandlingPåVent")
        }
        if (aktivBehandling != null && aktivBehandling.status != BehandlingStatus.AVSLUTTET) {
            throw BehandlingErIkkeAvsluttetException(aktivBehandling)
        }
    }

    /**
     * Hvis behandlingen er satt på vent av saksbehandler så skal statusen settes tilbake til SATT_PÅ_VENT
     * Ellers settes UTREDES
     */
    private fun utledStatusForBehandlingPåVent(behandlingPåVent: Behandling) =
        påVentService.finnAktivSettPåVentPåBehandling(behandlingPåVent.id)
            ?.let { BehandlingStatus.SATT_PÅ_VENT }
            ?: BehandlingStatus.UTREDES
}

private fun Behandling.harVærtPåVilkårsvurderingSteg() =
    behandlingStegTilstand.any { it.behandlingSteg == StegType.VILKÅRSVURDERING }

enum class SettPåMaskinellVentÅrsak(val årsak: String) {
    SATSENDRING("Satsendring"),
    OMREGNING_6_ELLER_18_ÅR("Omregning 6 eller 18 år"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    FØDSELSHENDELSE("Fødselshendelse"),
    MÅNEDLIG_VALUTAJUSTERING("Månedlig valutajustering"),
}

class BehandlingErIkkeAvsluttetException(val behandling: Behandling) :
    RuntimeException("Behandling=${behandling.id} har status=${behandling.status} og er ikke avsluttet")
