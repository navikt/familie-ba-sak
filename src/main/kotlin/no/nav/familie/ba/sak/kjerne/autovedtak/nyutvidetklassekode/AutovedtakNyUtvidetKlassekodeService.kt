package no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.NyUtvidetKlassekodeFeil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.nyutvidetklassekode.domene.NyUtvidetKlasskodeKjøringRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakNyUtvidetKlassekodeService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val nyUtvidetKlasskodeKjøringRepository: NyUtvidetKlasskodeKjøringRepository,
    private val snikeIKøenService: SnikeIKøenService,
    private val autovedtakService: AutovedtakService,
    private val taskRepository: TaskRepositoryWrapper,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun utførMigreringTilNyUtvidetKlassekode(fagsakId: Long) {
        logger.info("Utfører migrering til ny klassekode for utvidet barnetrygd for fagsak=$fagsakId")

        if (tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(fagsakId)) {
            logger.info("Hopper ut av behandling fordi fagsak $fagsakId allerede bruker ny klassekode for utvidet barnetrygd.")
            nyUtvidetKlasskodeKjøringRepository.settBrukerNyKlassekodeTilTrue(fagsakId)
            return
        }

        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId = fagsakId)
                ?: error("Fant ikke siste vedtatte behandling for fagsak $fagsakId")

        if (!sisteVedtatteBehandling.harLøpendeUtvidetBarnetrygd()) {
            logger.info("Hopper ut av behandling fordi fagsak $fagsakId ikke har løpende utvidet barnetrygd.")
            nyUtvidetKlasskodeKjøringRepository.deleteByFagsakId(fagsakId)
            return
        }

        val aktivOgÅpenBehandling = behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(fagsakId = fagsakId)

        if (aktivOgÅpenBehandling != null) {
            if (snikeIKøenService.kanSnikeForbi(aktivOgÅpenBehandling)) {
                snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                    aktivOgÅpenBehandling.id,
                    SettPåMaskinellVentÅrsak.NY_UTVIDET_KLASSEKODE,
                )
            } else {
                throw NyUtvidetKlassekodeFeil(
                    melding =
                        "Kan ikke utføre migrering til ny klassekode for utvidet barnetrygd for fagsak=$fagsakId " +
                            "fordi det er en åpen behandling vi ikke klarer å snike forbi",
                )
            }
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                aktør = sisteVedtatteBehandling.fagsak.aktør,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.NY_UTVIDET_KLASSEKODE,
                fagsakId = fagsakId,
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandlingEtterBehandlingsresultat,
                        opprettetVedtak,
                        SikkerhetContext.hentSaksbehandler(),
                    )
                }

                else -> throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved migrering til ny klassekode for utvidet barnetrygd for fagsak=$fagsakId")
            }

        taskRepository.save(task)
    }

    private fun Behandling.harLøpendeUtvidetBarnetrygd(): Boolean =
        tilkjentYtelseRepository
            .findByBehandling(id)
            .andelerTilkjentYtelse
            .any { it.erUtvidet() && it.erLøpende() }
}
