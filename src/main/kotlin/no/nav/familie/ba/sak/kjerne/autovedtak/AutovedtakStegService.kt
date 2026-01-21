package no.nav.familie.ba.sak.kjerne.autovedtak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg.AutovedtakFinnmarkstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.autovedtak.svalbardtillegg.AutovedtakSvalbardtilleggService
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
import no.nav.familie.util.VirkedagerProvider
import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

interface AutovedtakBehandlingService<Behandlingsdata : AutomatiskBehandlingData> {
    fun skalAutovedtakBehandles(behandlingsdata: Behandlingsdata): Boolean

    fun kjørBehandling(behandlingsdata: Behandlingsdata): String
}

enum class Autovedtaktype(
    val displayName: String,
) {
    FØDSELSHENDELSE("Fødselshendelse"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    OMREGNING_BREV("Omregning"),
    FINNMARKSTILLEGG("Finnmarkstillegg"),
    SVALBARDTILLEGG("Svalbardtillegg"),
}

sealed interface AutomatiskBehandlingData {
    val type: Autovedtaktype
}

data class FødselshendelseData(
    val nyBehandlingHendelse: NyBehandlingHendelse,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.FØDSELSHENDELSE
}

data class SmåbarnstilleggData(
    val aktør: Aktør,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.SMÅBARNSTILLEGG
}

data class OmregningBrevData(
    val aktør: Aktør,
    val behandlingsårsak: BehandlingÅrsak,
    val standardbegrunnelse: Standardbegrunnelse,
    val fagsakId: Long,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.OMREGNING_BREV
}

data class FinnmarkstilleggData(
    val fagsakId: Long,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.FINNMARKSTILLEGG
}

data class SvalbardtilleggData(
    val fagsakId: Long,
) : AutomatiskBehandlingData {
    override val type = Autovedtaktype.SVALBARDTILLEGG
}

@Service
class AutovedtakStegService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val oppgaveService: OppgaveService,
    private val autovedtakFødselshendelseService: AutovedtakFødselshendelseService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService,
    private val autovedtakFinnmarkstilleggService: AutovedtakFinnmarkstilleggService,
    private val autovedtakSvalbardtilleggService: AutovedtakSvalbardtilleggService,
    private val snikeIKøenService: SnikeIKøenService,
    private val featureToggleService: FeatureToggleService,
) {
    private val antallAutovedtak: Map<Autovedtaktype, Counter> =
        Autovedtaktype.entries.associateWith {
            Metrics.counter("behandling.saksbehandling.autovedtak", "type", it.name)
        }
    private val antallAutovedtakÅpenBehandling: Map<Autovedtaktype, Counter> =
        Autovedtaktype.entries.associateWith {
            Metrics.counter("behandling.saksbehandling.autovedtak.aapen_behandling", "type", it.name)
        }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørBehandlingFødselshendelse(
        mottakersAktør: Aktør,
        nyBehandlingHendelse: NyBehandlingHendelse,
        førstegangKjørt: LocalDateTime = LocalDateTime.now(),
    ): String =
        kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = FødselshendelseData(nyBehandlingHendelse),
            førstegangKjørt = førstegangKjørt,
        )

    fun kjørBehandlingOmregning(
        mottakersAktør: Aktør,
        behandlingsdata: OmregningBrevData,
        førstegangKjørt: LocalDateTime = LocalDateTime.now(),
    ): String =
        kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = behandlingsdata,
            førstegangKjørt = førstegangKjørt,
        )

    fun kjørBehandlingSmåbarnstillegg(
        mottakersAktør: Aktør,
        aktør: Aktør,
        førstegangKjørt: LocalDateTime = LocalDateTime.now(),
    ): String =
        kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = SmåbarnstilleggData(aktør),
            førstegangKjørt = førstegangKjørt,
        )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørBehandlingFinnmarkstillegg(
        mottakersAktør: Aktør,
        fagsakId: Long,
        førstegangKjørt: LocalDateTime = LocalDateTime.now(),
    ): String =
        kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = FinnmarkstilleggData(fagsakId),
            førstegangKjørt = førstegangKjørt,
        )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun kjørBehandlingSvalbardtillegg(
        mottakersAktør: Aktør,
        fagsakId: Long,
        førstegangKjørt: LocalDateTime = LocalDateTime.now(),
    ): String =
        kjørBehandling(
            mottakersAktør = mottakersAktør,
            automatiskBehandlingData = SvalbardtilleggData(fagsakId),
            førstegangKjørt = førstegangKjørt,
        )

    private fun kjørBehandling(
        automatiskBehandlingData: AutomatiskBehandlingData,
        mottakersAktør: Aktør,
        førstegangKjørt: LocalDateTime,
    ): String {
        secureLoggAutovedtakBehandling(automatiskBehandlingData.type, mottakersAktør, BEHANDLING_STARTER)
        antallAutovedtak[automatiskBehandlingData.type]?.increment()

        val skalAutovedtakBehandles =
            when (automatiskBehandlingData) {
                is FødselshendelseData -> autovedtakFødselshendelseService.skalAutovedtakBehandles(automatiskBehandlingData)
                is OmregningBrevData -> autovedtakBrevService.skalAutovedtakBehandles(automatiskBehandlingData)
                is SmåbarnstilleggData -> autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(automatiskBehandlingData)
                is FinnmarkstilleggData -> autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(automatiskBehandlingData)
                is SvalbardtilleggData -> autovedtakSvalbardtilleggService.skalAutovedtakBehandles(automatiskBehandlingData)
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
                førstegangKjørt = førstegangKjørt,
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
                is FinnmarkstilleggData -> autovedtakFinnmarkstilleggService.kjørBehandling(automatiskBehandlingData)
                is SvalbardtilleggData -> autovedtakSvalbardtilleggService.kjørBehandling(automatiskBehandlingData)
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

            is FinnmarkstilleggData -> behandlingsdata.fagsakId

            is SvalbardtilleggData -> behandlingsdata.fagsakId

            is FødselshendelseData,
            is SmåbarnstilleggData,
            -> null
        }

    private fun håndterÅpenBehandlingOgAvbrytAutovedtak(
        aktør: Aktør,
        autovedtaktype: Autovedtaktype,
        fagsakId: Long?,
        førstegangKjørt: LocalDateTime,
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
                    snikeIKøenService.settAktivBehandlingPåMaskinellVent(
                        åpenBehandling.id,
                        årsak = autovedtaktype.tilMaskinellVentÅrsak(),
                    )
                    return false
                } else if (autovedtaktype == Autovedtaktype.FINNMARKSTILLEGG) {
                    throw RekjørSenereException(
                        årsak = "Åpen behandling med status ${åpenBehandling.status} ble endret for under fire timer siden. Prøver igjen klokken 06.00 neste virkedag",
                        triggerTid = nesteVirkedag(LocalDate.now()).atTime(6, 0),
                    )
                }
            }

            BehandlingStatus.FATTER_VEDTAK -> {
                if (førstegangKjørt.until(LocalDateTime.now(), ChronoUnit.DAYS) < 7) {
                    throw RekjørSenereException(
                        årsak = "Åpen behandling med status ${åpenBehandling.status}, prøver igjen neste virkedag",
                        triggerTid = VirkedagerProvider.nesteVirkedag(LocalDate.now()).atTime(12, 0),
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
            behandlingId = åpenBehandling.id,
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

private fun Autovedtaktype.tilMaskinellVentÅrsak() =
    when (this) {
        Autovedtaktype.FØDSELSHENDELSE -> SettPåMaskinellVentÅrsak.FØDSELSHENDELSE
        Autovedtaktype.OMREGNING_BREV -> SettPåMaskinellVentÅrsak.OMREGNING_6_ELLER_18_ÅR
        Autovedtaktype.SMÅBARNSTILLEGG -> SettPåMaskinellVentÅrsak.SMÅBARNSTILLEGG
        Autovedtaktype.FINNMARKSTILLEGG -> SettPåMaskinellVentÅrsak.FINNMARKSTILLEGG
        Autovedtaktype.SVALBARDTILLEGG -> SettPåMaskinellVentÅrsak.SVALBARDTILLEGG
    }
