package no.nav.familie.ba.sak.oppgave

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.lang.IllegalStateException
import java.time.LocalDate

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres")
@Tag("integration")
class OppgaveIntegrationTest {

    @Autowired
    private lateinit var fagsakService: FagsakService

    @Autowired
    private lateinit var behandlingService: BehandlingService

    @Autowired
    private lateinit var oppgaveService: OppgaveService

    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository

    @Test
    fun `Skal opprette oppgave og ferdigstille oppgave for behandling`() {
        val søkerFnr = "12345678910"
        val barnFnr = "01101800033"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val godkjenneVedtakOppgaveId = oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        val opprettetOppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.GodkjenneVedtak, behandling)

        Assertions.assertNotNull(opprettetOppgave)
        Assertions.assertEquals(Oppgavetype.GodkjenneVedtak, opprettetOppgave!!.type)
        Assertions.assertEquals(behandling, opprettetOppgave.behandling)
        Assertions.assertFalse(opprettetOppgave.erFerdigstilt)
        Assertions.assertEquals(godkjenneVedtakOppgaveId, opprettetOppgave.gsakId)

        oppgaveService.ferdigstillOppgave(behandling.id, Oppgavetype.GodkjenneVedtak)

        Assertions.assertNull(oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.GodkjenneVedtak, behandling))
    }

    @Test
    fun `Skal kaste feil ved opprettelse av oppgave på type som ikke er ferdigstilt`() {
        val søkerFnr = "12345678910"
        val barnFnr = "01101800033"

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barnFnr))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        assertThatExceptionOfType(IllegalStateException::class.java)
                .isThrownBy { oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now()) }
                .withMessageStartingWith("Det finnes allerede en oppgave av typen ${Oppgavetype.GodkjenneVedtak} på behandling")
    }
}