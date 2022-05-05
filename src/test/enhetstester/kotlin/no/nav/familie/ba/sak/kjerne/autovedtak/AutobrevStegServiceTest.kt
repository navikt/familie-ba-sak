package no.nav.familie.ba.sak.kjerne.autovedtak

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.AutovedtakFødselshendelseService
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevBehandlingsdata
import no.nav.familie.ba.sak.kjerne.autovedtak.omregning.AutovedtakBrevService
import no.nav.familie.ba.sak.kjerne.autovedtak.småbarnstillegg.AutovedtakSmåbarnstilleggService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.prosessering.error.RekjørSenereException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class AutobrevStegServiceTest {
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val autovedtakFødselshendelseService = mockk<AutovedtakFødselshendelseService>()
    private val autovedtakBrevService = mockk<AutovedtakBrevService>()
    private val autovedtakSmåbarnstilleggService = mockk<AutovedtakSmåbarnstilleggService>()

    val autovedtakStegService = AutovedtakStegService(
        fagsakService = fagsakService,
        behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        oppgaveService = oppgaveService,
        autovedtakFødselshendelseService = autovedtakFødselshendelseService,
        autovedtakBrevService = autovedtakBrevService,
        autovedtakSmåbarnstilleggService = autovedtakSmåbarnstilleggService
    )

    @Test
    fun `Skal stoppe autovedtak og opprette oppgave ved åpen behandling som utredes`() {
        val aktør = randomAktørId()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.UTREDES
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(aktør) } returns true
        every { fagsakService.hent(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any()) } returns ""

        autovedtakStegService.kjørBehandling(
            mottakersAktør = aktør,
            autovedtaktype = Autovedtaktype.SMÅBARNSTILLEGG,
            behandlingsdata = aktør
        )

        verify(exactly = 1) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any()) }
    }

    @Test
    fun `Skal kun ta inn aktør som behandlingsdata for opphør av småbarnstillegg`() {
        val aktør = randomAktørId()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.AVSLUTTET
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(aktør) } returns true
        every { autovedtakSmåbarnstilleggService.kjørBehandling(aktør) } returns ""
        every { fagsakService.hent(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns null

        assertThrows<ClassCastException> {
            autovedtakStegService.kjørBehandling(
                mottakersAktør = aktør,
                autovedtaktype = Autovedtaktype.SMÅBARNSTILLEGG,
                behandlingsdata = AutovedtakBrevBehandlingsdata(
                    aktør = behandling.fagsak.aktør,
                    behandlingsårsak = BehandlingÅrsak.OMREGNING_SMÅBARNSTILLEGG,
                    standardbegrunnelse = Standardbegrunnelse.REDUKSJON_SMÅBARNSTILLEGG_IKKE_LENGER_FULL_OVERGANGSSTØNAD
                )
            )
        }
        assertDoesNotThrow {
            autovedtakStegService.kjørBehandling(
                mottakersAktør = aktør,
                autovedtaktype = Autovedtaktype.SMÅBARNSTILLEGG,
                behandlingsdata = aktør
            )
        }
    }

    @Test
    fun `Skal stoppe autovedtak og opprette oppgave ved åpen behandling med status Fatter vedtak`() {
        val aktør = randomAktørId()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.FATTER_VEDTAK
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(aktør) } returns true
        every { fagsakService.hent(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any()) } returns ""

        autovedtakStegService.kjørBehandling(
            mottakersAktør = aktør,
            autovedtaktype = Autovedtaktype.SMÅBARNSTILLEGG,
            behandlingsdata = aktør
        )

        verify(exactly = 1) { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any()) }
    }

    @Test
    fun `Skal stoppe autovedtak ved å kaste feil ved åpen behandling som iverksettes`() {
        val aktør = randomAktørId()
        val fagsak = defaultFagsak(aktør)
        val behandling = lagBehandling(fagsak).also {
            it.status = BehandlingStatus.IVERKSETTER_VEDTAK
        }

        every { autovedtakSmåbarnstilleggService.skalAutovedtakBehandles(aktør) } returns true
        every { fagsakService.hent(aktør) } returns fagsak
        every { behandlingHentOgPersisterService.hentAktivOgÅpenForFagsak(fagsakId = fagsak.id) } returns behandling
        every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any()) } returns ""

        assertThrows<RekjørSenereException> {
            autovedtakStegService.kjørBehandling(
                mottakersAktør = aktør,
                autovedtaktype = Autovedtaktype.SMÅBARNSTILLEGG,
                behandlingsdata = aktør
            )
        }
    }
}
