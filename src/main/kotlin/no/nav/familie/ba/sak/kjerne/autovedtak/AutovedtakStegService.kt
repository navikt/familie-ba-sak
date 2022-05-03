package no.nav.familie.ba.sak.kjerne.autovedtak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevBehandlingsdata
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

interface AutovedtakBehandlingService<Behandlingsdata> {
    fun skalAutovedtakBehandles(behandlingsdata: Behandlingsdata): Boolean = true

    fun kjørBehandling(behandlingsdata: Behandlingsdata): String
}

enum class Autovedtaktype(val displayName: String) {
    FØDSELSHENDELSE("Fødselshendelse"),
    SMÅBARNSTILLEGG("Småbarnstillegg"),
    OMREGNING_BREV("Omregning")
}

@Service
class AutovedtakStegService(
    private val fagsakService: FagsakService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    private val oppgaveService: OppgaveService,
    private val autovedtakFødselshendelseService: AutovedtakFødselshendelseService,
    private val autovedtakBrevService: AutovedtakBrevService,
    private val autovedtakSmåbarnstilleggService: AutovedtakSmåbarnstilleggService
) {

    private val antallAutovedtak: Map<Autovedtaktype, Counter> = Autovedtaktype.values().associateWith {
        Metrics.counter("behandling.saksbehandling.autovedtak", "type", it.name)
    }
    private val antallAutovedtakÅpenBehandling: Map<Autovedtaktype, Counter> = Autovedtaktype.values().associateWith {
        Metrics.counter("behandling.saksbehandling.autovedtak.aapen_behandling", "type", it.name)
    }

    fun <Behandlingsdata> kjørBehandling(
        mottakersAktør: Aktør,
        autovedtaktype: Autovedtaktype,
        behandlingsdata: Behandlingsdata
    ): String {
        secureLoggAutovedtakBehandling(autovedtaktype, mottakersAktør, BEHANDLING_STARTER)
        antallAutovedtak[autovedtaktype]?.increment()

        val skalAutovedtakBehandles = when (autovedtaktype) {
            Autovedtaktype.FØDSELSHENDELSE -> {
                autovedtakFødselshendelseService.skalAutovedtakBehandles(behandlingsdata as NyBehandlingHendelse)
            }
            Autovedtaktype.OMREGNING_BREV -> {
                autovedtakBrevService.skalAutovedtakBehandles(behandlingsdata as AutovedtakBrevBehandlingsdata)
            }
            Autovedtaktype.SMÅBARNSTILLEGG -> {
                autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(behandlingsdata as Aktør)
            }
        }

        if (!skalAutovedtakBehandles) {
            secureLoggAutovedtakBehandling(
                autovedtaktype,
                mottakersAktør,
                "Skal ikke behandles"
            )
            return "${autovedtaktype.displayName}: Skal ikke behandles"
        }

        if (håndterÅpenBehandlingOgAvbrytAutovedtak(
                aktør = mottakersAktør,
                autovedtaktype = autovedtaktype
            )
        ) {
            secureLoggAutovedtakBehandling(
                autovedtaktype,
                mottakersAktør,
                "Bruker har åpen behandling"
            )
            return "${autovedtaktype.displayName}: Bruker har åpen behandling"
        }

        val resultatAvKjøring = when (autovedtaktype) {
            Autovedtaktype.FØDSELSHENDELSE -> {
                autovedtakFødselshendelseService.kjørBehandling(behandlingsdata as NyBehandlingHendelse)
            }
            Autovedtaktype.OMREGNING_BREV -> {
                autovedtakBrevService.kjørBehandling(behandlingsdata as AutovedtakBrevBehandlingsdata)
            }
            Autovedtaktype.SMÅBARNSTILLEGG -> {
                autovedtakSmåbarnstilleggService.kjørBehandling(behandlingsdata as Aktør)
            }
        }

        secureLoggAutovedtakBehandling(
            autovedtaktype,
            mottakersAktør,
            resultatAvKjøring
        )

        return resultatAvKjøring
    }

    fun håndterÅpenBehandlingOgAvbrytAutovedtak(aktør: Aktør, autovedtaktype: Autovedtaktype): Boolean {
        val åpenBehandling = fagsakService.hent(aktør)?.let {
            behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(it.id)
        }

        return if (åpenBehandling == null) false
        else if (åpenBehandling.status == BehandlingStatus.UTREDES || åpenBehandling.status == BehandlingStatus.FATTER_VEDTAK) {
            antallAutovedtakÅpenBehandling[autovedtaktype]?.increment()

            oppgaveService.opprettOppgaveForManuellBehandling(
                behandling = åpenBehandling,
                begrunnelse = "${autovedtaktype.displayName}: Bruker har åpen behandling",
                oppgavetype = Oppgavetype.VurderLivshendelse
            )

            true
        } else if (åpenBehandling.status == BehandlingStatus.IVERKSETTER_VEDTAK) {
            throw RekjørSenereException(
                årsak = "Åpen behandling iverksettes, prøver igjen om 1 time",
                triggerTid = LocalDateTime.now().plusHours(1)
            )
        } else {
            throw Feil("Ikke håndtert feilsituasjon på $åpenBehandling")
        }
    }

    private fun secureLoggAutovedtakBehandling(
        autovedtaktype: Autovedtaktype,
        aktør: Aktør,
        melding: String
    ) {
        secureLogger.info("$autovedtaktype(${aktør.aktivFødselsnummer()}): $melding")
    }

    companion object {
        const val BEHANDLING_STARTER = "Behandling starter"
        const val BEHANDLING_FERDIG = "Behandling ferdig"
    }
}
