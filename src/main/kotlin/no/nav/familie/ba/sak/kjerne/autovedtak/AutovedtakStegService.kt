package no.nav.familie.ba.sak.kjerne.autovedtak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevBehandlingsdata
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.error.RekjørSenereException
import org.springframework.stereotype.Service
import java.time.LocalDateTime

interface AutovedtakBehandlingService<Behandlingsdata> {
    fun kanAutovedtakBehandles(behandlingsdata: Behandlingsdata): Boolean = true

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
    private val behandlingService: BehandlingService,
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
        antallAutovedtak[autovedtaktype]?.increment()

        val kanAutovedtakBehandles = when (autovedtaktype) {
            Autovedtaktype.FØDSELSHENDELSE -> {
                autovedtakFødselshendelseService.kanAutovedtakBehandles(behandlingsdata as NyBehandlingHendelse)
            }
            Autovedtaktype.OMREGNING_BREV -> {
                autovedtakBrevService.kanAutovedtakBehandles(behandlingsdata as AutovedtakBrevBehandlingsdata)
            }
            Autovedtaktype.SMÅBARNSTILLEGG -> {
                autovedtakSmåbarnstilleggService.kanAutovedtakBehandles(behandlingsdata as Aktør)
            }
        }

        if (!kanAutovedtakBehandles) return "Behandling stoppet i prekjøringssteget"

        if (håndterÅpenBehandlingOgAvbrytAutovedtak(
                aktør = mottakersAktør,
                autovedtaktype = autovedtaktype
            )
        ) return "Bruker har åpen behandling"

        return when (autovedtaktype) {
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
    }

    fun håndterÅpenBehandlingOgAvbrytAutovedtak(aktør: Aktør, autovedtaktype: Autovedtaktype): Boolean {
        val åpenBehandling = fagsakService.hent(aktør)?.let {
            behandlingService.hentAktivOgÅpenForFagsak(it.id)
        }

        return if (åpenBehandling == null) false
        else if (åpenBehandling.status == BehandlingStatus.UTREDES) {
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
}
