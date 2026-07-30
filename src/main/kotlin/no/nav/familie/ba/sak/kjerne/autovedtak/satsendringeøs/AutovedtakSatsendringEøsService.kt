package no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs

import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakBehandlingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.SatsendringEøsData
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsSvar.SATSENDRING_EØS_ER_ALLEREDE_UTFØRT
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsSvar.SATSENDRING_EØS_INGEN_RELEVANTE_UTENLANDSK_PERIODEBELØP
import no.nav.familie.ba.sak.kjerne.autovedtak.satsendringeøs.SatsendringEøsSvar.SATSENDRING_EØS_KJØRT_OK
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.sats.EøsSatserRegister
import no.nav.familie.ba.sak.kjerne.eøs.sats.SatsendringEøsValidering.validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk
import no.nav.familie.ba.sak.kjerne.eøs.sats.filtrerErRelevantForSats
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløpService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.task.IverksettMotOppdragTask
import no.nav.familie.ba.sak.task.JournalførVedtaksbrevTask
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutovedtakSatsendringEøsService(
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val satsendringEøsKjøringService: SatsendringEøsKjøringService,
    private val utenlandskPeriodebeløpService: UtenlandskPeriodebeløpService,
    private val autovedtakService: AutovedtakService,
    private val taskRepository: TaskRepositoryWrapper,
) : AutovedtakBehandlingService<SatsendringEøsData> {
    override fun skalAutovedtakBehandles(behandlingsdata: SatsendringEøsData): Boolean {
        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(behandlingsdata.fagsakId)
                ?: return false

        return sisteVedtatteBehandling.kategori == BehandlingKategori.EØS &&
            sisteVedtatteBehandling.fagsak.status == FagsakStatus.LØPENDE
    }

    /**
     * Gjennomfører og committer revurderingsbehandling med årsak SATSENDRING_EØS.
     *
     * Kaster [AutovedtakMåBehandlesManueltFeil] dersom det er avvik i valuta,
     * intervall eller beløp og saksbehandler må opprette en manuell revurdering.
     */
    @Transactional
    override fun kjørBehandling(behandlingsdata: SatsendringEøsData): String {
        val fagsakId = behandlingsdata.fagsakId
        val utbetalingsland = behandlingsdata.utbetalingsland
        val satsTidspunkt = behandlingsdata.satsTidspunkt

        val sisteVedtatteBehandling =
            behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsakId)
                ?: throw Feil("Fant ikke siste vedtatte behandling for fagsak=$fagsakId")

        val nySats = EøsSatserRegister.hentSatsForLandIMåned(utbetalingsland, satsTidspunkt)
        val relevanteUtenlandskPeriodebeløp =
            utenlandskPeriodebeløpService
                .hentUtenlandskePeriodebeløp(BehandlingId(sisteVedtatteBehandling.id))
                .filtrerErRelevantForSats(nySats)

        if (relevanteUtenlandskPeriodebeløp.isEmpty()) {
            logger.info("${SATSENDRING_EØS_INGEN_RELEVANTE_UTENLANDSK_PERIODEBELØP.melding} for fagsak $fagsakId")
            return SATSENDRING_EØS_INGEN_RELEVANTE_UTENLANDSK_PERIODEBELØP.melding
        }

        if (relevanteUtenlandskPeriodebeløp.all { it.beløp == nySats.beløp }) {
            logger.info("${SATSENDRING_EØS_ER_ALLEREDE_UTFØRT.melding} for fagsak $fagsakId")
            return SATSENDRING_EØS_ER_ALLEREDE_UTFØRT.melding
        }

        val forrigeSats = EøsSatserRegister.hentSatsForLandIMåned(utbetalingsland, nySats.fom.minusMonths(1))

        relevanteUtenlandskPeriodebeløp.forEach { utenlandskPeriodebeløp ->
            validerAtUtenlandskPeriodebeløpKanOppdateresAutomatisk(
                utenlandskPeriodebeløp = utenlandskPeriodebeløp,
                forrigeSats = forrigeSats,
                nySats = nySats,
            )
        }

        val behandlingEtterBehandlingsresultat =
            autovedtakService.opprettAutomatiskBehandlingOgKjørTilBehandlingsresultat(
                behandlingType = BehandlingType.REVURDERING,
                behandlingÅrsak = BehandlingÅrsak.SATSENDRING_EØS,
                fagsakId = fagsakId,
                kjørFørVilkårsvurdering = { behandling ->
                    satsendringEøsKjøringService.settBehandlingId(fagsakId, utbetalingsland, satsTidspunkt, behandling.id)
                },
            )

        val opprettetVedtak =
            autovedtakService.opprettToTrinnskontrollOgVedtaksbrevForAutomatiskBehandling(
                behandlingEtterBehandlingsresultat,
            )

        val task =
            when (behandlingEtterBehandlingsresultat.steg) {
                StegType.IVERKSETT_MOT_OPPDRAG -> {
                    IverksettMotOppdragTask.opprettTask(
                        behandling = behandlingEtterBehandlingsresultat,
                        vedtak = opprettetVedtak,
                        saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                    )
                }

                StegType.JOURNALFØR_VEDTAKSBREV -> {
                    JournalførVedtaksbrevTask.opprettTaskJournalførVedtaksbrev(
                        personIdent = behandlingEtterBehandlingsresultat.fagsak.aktør.aktivFødselsnummer(),
                        behandlingId = behandlingEtterBehandlingsresultat.id,
                        vedtakId = opprettetVedtak.id,
                    )
                }

                else -> {
                    throw Feil("Ugyldig neste steg ${behandlingEtterBehandlingsresultat.steg} ved satsendring EØS for fagsak=$fagsakId")
                }
            }

        taskRepository.save(task)

        return SATSENDRING_EØS_KJØRT_OK.melding
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AutovedtakSatsendringEøsService::class.java)
    }
}

enum class SatsendringEøsSvar(
    val melding: String,
) {
    SATSENDRING_EØS_KJØRT_OK("Satsendring EØS kjørt OK"),
    SATSENDRING_EØS_ER_ALLEREDE_UTFØRT("Satsendring EØS allerede utført"),
    SATSENDRING_EØS_INGEN_RELEVANTE_UTENLANDSK_PERIODEBELØP("Ingen relevante utenlandsk periodebeløp for satsendring EØS"),
    SATSENDRING_EØS_MÅ_BEHANDLES_MANUELT("Satsendring EØS må behandles manuelt fordi utenlandsk periodebeløp har avvik i beløp, valuta eller intervall"),
}
