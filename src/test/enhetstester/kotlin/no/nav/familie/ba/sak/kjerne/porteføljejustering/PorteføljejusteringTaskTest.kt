package no.nav.familie.ba.sak.kjerne.porteføljejustering

import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.DRAMMEN
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.OSLO
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.STEINKJER
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.BarnetrygdEnhet.STORD
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.klage.KlageKlient
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingKlient
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import java.util.UUID

class PorteføljejusteringTaskTest {
    private val integrasjonKlient: IntegrasjonKlient = mockk()
    private val tilbakekrevingKlient: TilbakekrevingKlient = mockk()
    private val klageKlient: KlageKlient = mockk()
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService = mockk()
    private val personidentService: PersonidentService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()

    private val porteføljejusteringFlyttOppgaveTask =
        PorteføljejusteringFlyttOppgaveTask(
            integrasjonKlient = integrasjonKlient,
            tilbakekrevingKlient = tilbakekrevingKlient,
            klageKlient = klageKlient,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            personidentService = personidentService,
            fagsakService = fagsakService,
            arbeidsfordelingService = arbeidsfordelingService,
        )

    @Test
    fun `Skal ikke flytte dersom oppgave ikke er tildelt Steinkjer`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = OSLO.enhetsnummer,
            )

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.finnOppgaveMedId(1) }
        verify(exactly = 0) { integrasjonKlient.hentBehandlendeEnhet(any()) }
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
    }

    @Test
    fun `Skal kaste feil dersom oppgave ikke er tilknyttet en folkeregistrert ident`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Oppgave med id 1 er ikke tilknyttet en ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi ikke får tilbake noen enheter på ident fra norg`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns emptyList()

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Fant ingen arbeidsfordelingsenhet for ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi får tilbake flere enehter på ident fra norg`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(OSLO.enhetsnummer, OSLO.enhetsnavn),
                Arbeidsfordelingsenhet(DRAMMEN.enhetsnummer, DRAMMEN.enhetsnavn),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Fant flere arbeidsfordelingsenheter for ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi får tilbake Steinkjer som enhet på ident fra norg`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(STEINKJER.enhetsnummer, STEINKJER.enhetsnavn))

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Oppgave med id 1 tildeles fortsatt Steinkjer som enhet")
    }

    @ParameterizedTest
    @EnumSource(value = BarnetrygdEnhet::class, names = ["OSLO", "VADSØ", "STEINKJER"], mode = EXCLUDE)
    fun `Skal ikke flytte hvis ny enhet ikke er Oslo eller Vadsø`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(STORD.enhetsnummer, STORD.enhetsnavn))

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
        verify { arbeidsfordelingService wasNot Called }
    }

    @Test
    fun `Skal oppdatere oppgaven med ny enhet og mappe, men ikke behandlingen, dersom saksreferansen ikke er fylt ut`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                saksreferanse = null,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-ba-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(OSLO.enhetsnummer, OSLO.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EXCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, men ikke behandlingen, dersom saksreferanse er fylt ut, men det er ikke av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-ba-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(OSLO.enhetsnummer, OSLO.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i ba-sak dersom behandlesAvApplikasjon er familie-ba-sak og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")
        val aktørPåOppgave = lagAktør()
        val behandling = lagBehandling()

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-ba-sak",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(OSLO.enhetsnummer, OSLO.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1L, OSLO.enhetsnummer, "100012753") } returns mockk()
        every { personidentService.hentAktør("1") } returns aktørPåOppgave
        every { fagsakService.hentNormalFagsak(aktørPåOppgave) } returns lagFagsak(id = 1)
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(1) } returns behandling
        every { arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, OSLO.enhetsnummer) } just Runs

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") }
        verify(exactly = 1) { personidentService.hentAktør("1") }
        verify(exactly = 1) { fagsakService.hentNormalFagsak(aktørPåOppgave) }
        verify(exactly = 1) { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(1) }
        verify(exactly = 1) {
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, OSLO.enhetsnummer)
        }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i klage dersom behandlesAvApplikasjon er familie-klage og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-klage",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(OSLO.enhetsnummer, OSLO.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") } returns mockk()
        every { klageKlient.oppdaterEnhetPåÅpenBehandling(1, OSLO.enhetsnummer) } returns "TODO"

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") }
        verify(exactly = 1) { klageKlient.oppdaterEnhetPåÅpenBehandling(1L, OSLO.enhetsnummer) }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i tilbakekreving dersom behandlesAvApplikasjon er familie-tilbake og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")
        val behandlingEksternBrukId = UUID.randomUUID()

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = 1,
                identer = listOf(OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT)),
                tildeltEnhetsnr = STEINKJER.enhetsnummer,
                saksreferanse = behandlingEksternBrukId.toString(),
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-tilbake",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns listOf(Arbeidsfordelingsenhet(OSLO.enhetsnummer, OSLO.enhetsnavn))
        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1L, OSLO.enhetsnummer, "100012753") } returns mockk()
        every { tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(behandlingEksternBrukId, OSLO.enhetsnummer) } returns "TODO"

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, OSLO.enhetsnummer, "100012753") }
        verify(exactly = 1) { tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(behandlingEksternBrukId, OSLO.enhetsnummer) }
    }
}
