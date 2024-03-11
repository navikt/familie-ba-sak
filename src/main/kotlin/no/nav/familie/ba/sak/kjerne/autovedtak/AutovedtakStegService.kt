package no.nav.familie.ba.sak.kjerne.autovedtak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.SettPåMaskinellVentÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.SnikeIKøenService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

interface AutovedtakBehandlingService<Behandlingsdata : AutomatiskBehandlingData> {
    fun skalAutovedtakBehandles(behandlingsdata: Behandlingsdata): Boolean

    fun kjørBehandling(behandlingsdata: Behandlingsdata): String
}

enum class Autovedtaktype(val displayName: String) {
    FØDSELSHENDELSE("Fødselshendelse"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    OMREGNING_BREV("Omregning"),
}

sealed interface AutomatiskBehandlingData {
    val type: Autovedtaktype
    val taskOpprettetTid: LocalDateTime
}

data class FødselshendelseData(
    val nyBehandlingHendelse: NyBehandlingHendelse,
    override val taskOpprettetTid: LocalDateTime = LocalDateTime.now(),
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.FØDSELSHENDELSE
}

data class SmåbarnstilleggData(
    val aktør: Aktør,
    override val taskOpprettetTid: LocalDateTime = LocalDateTime.now(),
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.SMÅBARNSTILLEGG
}

data class OmregningBrevData(
    val aktør: Aktør,
    val behandlingsårsak: BehandlingÅrsak,
    val standardbegrunnelse: Standardbegrunnelse,
    val fagsakId: Long,
    override val taskOpprettetTid: LocalDateTime = LocalDateTime.now(),
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.OMREGNING_BREV
}

@Service
class AutovedtakStegService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val oppgaveService: OppgaveService,
    private val autovedtakFødselshendelseService: AutovedtakFødselshendelseService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
    private val snikeIKøenService: SnikeIKøenService,
) {
    private val antallAutovedtak: Map<Autovedtaktype, Counter> =
        Autovedtaktype.values().associateWith {
            Metrics.counter("behandling.saksbehandling.autovedtak", "type", it.name)
        }
    private val antallAutovedtakÅpenBehandling: Map<Autovedtaktype, Counter> =
        Autovedtaktype.values().associateWith {
            Metrics.counter("behandling.saksbehandling.autovedtak.aapen_behandling", "type", it.name)
        }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørBehandlingFødselshendelse(
        mottakersAktør: Aktør,
        nyBehandlingHendelse: NyBehandlingHendelse,
        taskOpprettetTid: LocalDateTime = LocalDateTime.now(),
    ): String {
        return kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = FødselshendelseData(nyBehandlingHendelse, taskOpprettetTid),
        )
    }

    fun kjørBehandlingOmregning(
        mottakersAktør: Aktør,
        behandlingsdata: OmregningBrevData,
    ): String {
        return kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = behandlingsdata,
        )
    }

    fun kjørBehandlingSmåbarnstillegg(
        mottakersAktør: Aktør,
        aktør: Aktør,
        taskOpprettetTid: LocalDateTime = LocalDateTime.now(),
    ): String {
        return kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = SmåbarnstilleggData(aktør, taskOpprettetTid),
        )
    }

    private fun kjørBehandling(
        automatiskBehandlingData: AutomatiskBehandlingData,
        mottakersAktør: Aktør,
    ): String {
        secureLoggAutovedtakBehandling(automatiskBehandlingData.type, mottakersAktør, BEHANDLING_STARTER)
        antallAutovedtak[automatiskBehandlingData.type]?.increment()

        val skalAutovedtakBehandles =
            when (automatiskBehandlingData) {
                is FødselshendelseData -> autovedtakFødselshendelseService.skalAutovedtakBehandles(automatiskBehandlingData)
                is OmregningBrevData -> autovedtakBrevService.skalAutovedtakBehandles(automatiskBehandlingData)
                is SmåbarnstilleggData -> autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(automatiskBehandlingData)
            }

        if (!skalAutovedtakBehandles) {
            secureLoggAutovedtakBehandling(
                automatiskBehandlingData.type,
                mottakersAktør,
                "Skal ikke behandles",
            )
            return "${automatiskBehandlingData.type.displayName}: Skal ikke behandles"
        }

        if (håndterÅpenBehandlingOgAvbrytAutovedtak(
                aktør = mottakersAktør,
                autovedtaktype = automatiskBehandlingData.type,
                fagsakId = hentFagsakIdFraBehandlingsdata(automatiskBehandlingData),
                taskOpprettetTid = automatiskBehandlingData.taskOpprettetTid,
            )
        ) {
            secureLoggAutovedtakBehandling(
                automatiskBehandlingData.type,
                mottakersAktør,
                "Bruker har åpen behandling",
            )
            return "${automatiskBehandlingData.type.displayName}: Bruker har åpen behandling"
        }

        val resultatAvKjøring =
            when (automatiskBehandlingData) {
                is FødselshendelseData -> autovedtakFødselshendelseService.kjørBehandling(automatiskBehandlingData)
                is OmregningBrevData -> autovedtakBrevService.kjørBehandling(automatiskBehandlingData)
                is SmåbarnstilleggData -> autovedtakSmåbarnstilleggService.kjørBehandling(automatiskBehandlingData)
            }

        secureLoggAutovedtakBehandling(
            automatiskBehandlingData.type,
            mottakersAktør,
            resultatAvKjøring,
        )

        return resultatAvKjøring
    }

    private fun hentFagsakIdFraBehandlingsdata(
        behandlingsdata: AutomatiskBehandlingData,
    ): Long? =
        when (behandlingsdata) {
            is OmregningBrevData -> behandlingsdata.fagsakId
            is FødselshendelseData,
            is SmåbarnstilleggData,
            -> null
        }

    private fun håndterÅpenBehandlingOgAvbrytAutovedtak(
        aktør: Aktør,
        autovedtaktype: Autovedtaktype,
        fagsakId: Long?,
        taskOpprettetTid: LocalDateTime,
    ): Boolean {
        val fagsak =
            if (fagsakId != null) {
                fagsakService.hentPåFagsakId(fagsakId)
            } else {
                fagsakService.hentNormalFagsak(aktør = aktør)
            }
        val åpenBehandling =
            fagsak?.let {
                behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(it.id)
            } ?: return false

        when (åpenBehandling.status) {
            BehandlingStatus.UTREDES,
            BehandlingStatus.SATT_PÅ_VENT,
            -> {
                if (snikeIKøenService.kanSnikeForbi(åpenBehandling)) {
                    snikeIKøenService.settAktivBehandlingTilPåMaskinellVent(
                        åpenBehandling.id,
                        årsak = SettPåMaskinellVentÅrsak.OMREGNING_6_ELLER_18_ÅR,
                    )
                    return false
                }
            }
            BehandlingStatus.FATTER_VEDTAK -> {
                if (taskOpprettetTid.until(LocalDateTime.now(), ChronoUnit.HOURS) < 72) {
                    throw RekjørSenereException(
                        årsak = "Åpen behandling med status ${åpenBehandling.status}, prøver igjen om 24 timer",
                        triggerTid = LocalDateTime.now().plusHours(24),
                    )
                }
            }
            BehandlingStatus.IVERKSETTER_VEDTAK,
            BehandlingStatus.SATT_PÅ_MASKINELL_VENT,
            -> {
                throw RekjørSenereException(
                    årsak = "Åpen behandling med status ${åpenBehandling.status}, prøver igjen om 1 time",
                    triggerTid = LocalDateTime.now().plusHours(1),
                )
            }
            else -> {
                throw Feil("Ikke håndtert feilsituasjon på $åpenBehandling")
            }
        }

        antallAutovedtakÅpenBehandling[autovedtaktype]?.increment()
        oppgaveService.opprettOppgaveForManuellBehandling(
            behandling = åpenBehandling,
            begrunnelse = "${autovedtaktype.displayName}: Bruker har åpen behandling",
            manuellOppgaveType = ManuellOppgaveType.ÅPEN_BEHANDLING,
        )
        return true
    }

    private fun secureLoggAutovedtakBehandling(
        autovedtaktype: Autovedtaktype,
        aktør: Aktør,
        melding: String,
    ) {
        secureLogger.info("$autovedtaktype(${aktør.aktivFødselsnummer()}): $melding")
    }

    companion object {
        const val BEHANDLING_STARTER = "Behandling starter"
        const val BEHANDLING_FERDIG = "Behandling ferdig"
    }
}
