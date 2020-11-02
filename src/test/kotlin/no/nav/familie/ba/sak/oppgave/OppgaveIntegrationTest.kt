package no.nav.familie.ba.sak.oppgave

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.fagsak.FagsakService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.common.DbContainerInitializer
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.config.ClientMocks
import no.nav.familie.ba.sak.oppgave.domene.OppgaveRepository
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ContextConfiguration(initializers = [DbContainerInitializer::class])
@ActiveProfiles("postgres", "mock-pdl", "mock-arbeidsfordeling")
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

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(SØKER_FNR)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, SØKER_FNR, listOf(BARN_FNR))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        val godkjenneVedtakOppgaveId = oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        val opprettetOppgave = oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.GodkjenneVedtak, behandling)

        Assertions.assertNotNull(opprettetOppgave)
        Assertions.assertEquals(Oppgavetype.GodkjenneVedtak, opprettetOppgave!!.type)
        Assertions.assertEquals(behandling.id, opprettetOppgave.behandling.id)
        Assertions.assertEquals(behandling.status, opprettetOppgave.behandling.status)
        Assertions.assertEquals(behandling.behandlingStegTilstand.first().behandlingSteg,
                                opprettetOppgave.behandling.behandlingStegTilstand.first().behandlingSteg)
        Assertions.assertEquals(behandling.behandlingStegTilstand.first().behandlingStegStatus,
                                opprettetOppgave.behandling.behandlingStegTilstand.first().behandlingStegStatus)
        Assertions.assertFalse(opprettetOppgave.erFerdigstilt)
        Assertions.assertEquals(godkjenneVedtakOppgaveId, opprettetOppgave.gsakId)

        oppgaveService.ferdigstillOppgave(behandling.id, Oppgavetype.GodkjenneVedtak)

        Assertions.assertNull(oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.GodkjenneVedtak, behandling))
    }

    @Test
    fun `Skal logge feil ved opprettelse av oppgave på type som ikke er ferdigstilt`() {

        val logger: Logger = LoggerFactory.getLogger(OppgaveService::class.java) as Logger

        val listAppender: ListAppender<ILoggingEvent> = initLoggingEventListAppender()
        logger.addAppender(listAppender)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(SØKER_FNR)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, SØKER_FNR, listOf(BARN_FNR))
        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())
        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        val loggingEvents = listAppender.list

        assertThat(loggingEvents)
                .extracting<String, RuntimeException> { obj: ILoggingEvent -> obj.formattedMessage }
                .anyMatch { message -> message.contains("Fant eksisterende oppgave med samme oppgavetype") }
    }

    protected fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        return listAppender
    }



    companion object {
        private val SØKER_FNR = ClientMocks.søkerFnr[0]
        private val BARN_FNR = ClientMocks.barnFnr[0]
    }
}