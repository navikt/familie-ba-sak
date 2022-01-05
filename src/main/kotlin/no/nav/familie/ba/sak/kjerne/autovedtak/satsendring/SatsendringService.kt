package no.nav.familie.ba.sak.kjerne.autovedtak.satsendring

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.FerdigstillBehandlingTask
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Service
class SatsendringService(
    private val taskRepository: TaskRepositoryWrapper,
    private val behandlingRepository: BehandlingRepository,
    private val autovedtakService: AutovedtakService
) {

    /**
     * Finner behandlinger som trenger satsendring.
     * Se https://github.com/navikt/familie-ba-sak/pull/1361 for eksempel på scheduler.
     *
     * Obs! Denne utplukkingen tar også med inaktive behandlinger, siden den aktive behandlingen ikke nødvendigvis
     * iverksatte (f.eks. omregning eller henleggelse). Dette betyr at man potensielt får med fagsaker hvor
     * behovet for revurdering i ettertid har blitt fjernet. Dersom man ønsker å filtrere bort disse må
     * man sjekke om den inaktive behandlingen blir etterfulgt av revurdering som fjerner behovet.
     */
    fun finnBehandlingerForSatsendring(
        gammelSats: Int,
        satsendringMåned: YearMonth
    ): List<Long> {
        val behandlinger = behandlingRepository.finnBehadlingerForSatsendring(
            iverksatteLøpende = behandlingRepository.finnSisteIverksatteBehandlingFraLøpendeFagsaker(),
            gammelSats = gammelSats,
            månedÅrForEndring = satsendringMåned
        )

        return behandlinger.subList(0, minOf(100, behandlinger.size))
    }

    /**
     * Gjennomfører og commiter revurderingsbehandling
     * med årsak satsendring og uten endring i vilkår.
     *
     * Dersom man utfører dette på en behandling uten behov for satsendring eller ny sats ikke er lagt inn i systemet enda,
     * ferdigstilles behandlingen uendret (FORTSATT_INNVILGET). I og med at satsendring ikke trigger brev er det ikke kritisk.
     */
    @Transactional
    fun utførSatsendring(behandlingId: Long) {

        val behandling = behandlingRepository.finnBehandling(behandlingId = behandlingId)
        val søkerAktør = behandling.fagsak.aktør

        logger.info("Kjører satsendring på $behandling")
        secureLogger.info("Kjører satsendring på $behandling for ${søkerAktør.aktivFødselsnummer()}")
        if (behandling.fagsak.status != FagsakStatus.LØPENDE) throw Feil("Forsøker å utføre satsendring på ikke løpende fagsak ${behandling.fagsak.id}")
        if (behandling.status != BehandlingStatus.AVSLUTTET) throw Feil("Forsøker å utføre satsendring på behandling ${behandling.id} som ikke er avsluttet")

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                fagsak = behandling.fagsak,
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat
            )

        val task = if (behandlingEtterBehandlingsresultat.resultat == BehandlingResultat.ENDRET) {
            IverksettMotOppdragTask.opprettTask(
                behandlingEtterBehandlingsresultat,
                opprettetVedtak,
                SikkerhetContext.hentSaksbehandler()
            )
        } else {
            FerdigstillBehandlingTask.opprettTask(
                søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
                behandlingsId = behandlingEtterBehandlingsresultat.id
            )
        }
        taskRepository.save(task)
    }

    companion object {
        val logger = LoggerFactory.getLogger(SatsendringService::class.java)
        val secureLogger = LoggerFactory.getLogger("secureLogger")
    }
}
