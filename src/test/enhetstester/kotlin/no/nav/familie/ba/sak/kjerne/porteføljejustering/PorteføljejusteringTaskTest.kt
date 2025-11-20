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
    fun `Skal kaste feil dersom oppgave ikke er tilknyttet noe folkeregistrert ident`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.SAMHANDLERNR),
                    ),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }
        assertThat(exception.message).isEqualTo("Oppgave med id 1 er ikke tilknyttet en ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi ikke får tilbake noen enheter på ident ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
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
    fun `Skal kaste feil dersom vi får tilbake flere enn 1 enhet på ident ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.OSLO.enhetsnummer, BarnetrygdEnhet.OSLO.enhetsnavn),
                Arbeidsfordelingsenhet(BarnetrygdEnhet.DRAMMEN.enhetsnummer, BarnetrygdEnhet.DRAMMEN.enhetsnavn),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Fant flere arbeidsfordelingsenheter for ident.")
    }

    @Test
    fun `Skal kaste feil dersom vi får tilbake Steinkjer som enhet på ident ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.STEINKJER.enhetsnummer, BarnetrygdEnhet.STEINKJER.enhetsnavn),
            )

        // Act && Assert
        val exception =
            assertThrows<Feil> {
                porteføljejusteringFlyttOppgaveTask.doTask(task)
            }

        assertThat(exception.message).isEqualTo("Oppgave med id 1 tildeles fortsatt Steinkjer som enhet")
    }

    @Test
    fun `Skal stoppe utføringen av task hvis det er midlertidig enhet vi får tilbake ved kall mot integrasjoner og videre til norg2`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                saksreferanse = "referanse",
                oppgavetype = "BEH_SAK",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnummer, BarnetrygdEnhet.MIDLERTIDIG_ENHET.enhetsnavn),
            )

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 0) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(any(), any(), any()) }
        verify { arbeidsfordelingService wasNot Called }
    }

    @Test
    fun `Skal oppdatere oppgaven med ny enhet og mappe og ikke mer dersom saksreferansen ikke er fylt ut`() {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                saksreferanse = null,
                oppgavetype = "BEH_SAK",
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-ba-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.OSLO.enhetsnummer, BarnetrygdEnhet.OSLO.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.EXCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe og ikke mer dersom saksreferanse er fylt ut, men det er ikke av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-ba-sak",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.OSLO.enhetsnummer, BarnetrygdEnhet.OSLO.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") } returns mockk()

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") }
        verify { arbeidsfordelingService wasNot Called }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i ba-sak dersom behandlesAvApplikasjon er familie-ba-sak og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-ba-sak",
                aktoerId = "1",
            )
        val aktørPåOppgave = lagAktør()

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.OSLO.enhetsnummer, BarnetrygdEnhet.OSLO.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") } returns mockk()

        every { personidentService.hentAktør("1") } returns aktørPåOppgave
        every { fagsakService.hentNormalFagsak(aktørPåOppgave) } returns lagFagsak(id = 1)

        val behandling = lagBehandling()
        every { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(1) } returns behandling

        every { arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, BarnetrygdEnhet.OSLO.enhetsnummer) } just Runs

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") }
        verify(exactly = 1) { personidentService.hentAktør("1") }
        verify(exactly = 1) { fagsakService.hentNormalFagsak(aktørPåOppgave) }
        verify(exactly = 1) { behandlingHentOgPersisterService.finnAktivOgÅpenForFagsak(1) }
        verify(exactly = 1) {
            arbeidsfordelingService.oppdaterBehandlendeEnhetPåBehandlingIForbindelseMedPorteføljejustering(behandling, BarnetrygdEnhet.OSLO.enhetsnummer)
        }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i klage dersom behandlesAvApplikasjon er familie-klage og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")
        val oppgaveId = 1L

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                id = oppgaveId,
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-klage",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.OSLO.enhetsnummer, BarnetrygdEnhet.OSLO.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") } returns mockk()
        every { klageKlient.oppdaterEnhetPåÅpenBehandling(oppgaveId, "4833") } returns "TODO"

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") }
        verify(exactly = 1) { klageKlient.oppdaterEnhetPåÅpenBehandling(oppgaveId, BarnetrygdEnhet.OSLO.enhetsnummer) }
    }

    @ParameterizedTest
    @EnumSource(Oppgavetype::class, names = ["BehandleSak", "GodkjenneVedtak", "BehandleUnderkjentVedtak"], mode = EnumSource.Mode.INCLUDE)
    fun `Skal oppdatere oppgaven med ny enhet og mappe, og oppdatere behandlingen i tilbakekreving dersom behandlesAvApplikasjon er familie-tilbake og oppgavetype er av type behandle sak, godkjenne vedtak eller behandle underkjent vedtak`(
        oppgavetype: Oppgavetype,
    ) {
        // Arrange
        val task = PorteføljejusteringFlyttOppgaveTask.opprettTask(1, "1", "1")

        every { integrasjonKlient.finnOppgaveMedId(1) } returns
            Oppgave(
                identer =
                    listOf(
                        OppgaveIdentV2("1234", IdentGruppe.FOLKEREGISTERIDENT),
                    ),
                tildeltEnhetsnr = BarnetrygdEnhet.STEINKJER.enhetsnummer,
                saksreferanse = "183421813",
                oppgavetype = oppgavetype.value,
                mappeId = 100027793,
                behandlesAvApplikasjon = "familie-tilbake",
                aktoerId = "1",
            )

        every { integrasjonKlient.hentBehandlendeEnhet("1234") } returns
            listOf(
                Arbeidsfordelingsenhet(BarnetrygdEnhet.OSLO.enhetsnummer, BarnetrygdEnhet.OSLO.enhetsnavn),
            )

        every { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") } returns mockk()
        every { tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(183421813, "4833") } returns "TODO"

        // Act
        porteføljejusteringFlyttOppgaveTask.doTask(task)

        // Assert
        verify(exactly = 1) { integrasjonKlient.tilordneEnhetOgMappeForOppgave(1, BarnetrygdEnhet.OSLO.enhetsnummer, "100012753") }
        verify(exactly = 1) { tilbakekrevingKlient.oppdaterEnhetPåÅpenBehandling(183421813, BarnetrygdEnhet.OSLO.enhetsnummer) }
    }
}
