package no.nav.familie.ba.sak.integrasjoner.oppgave

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomBarnFnr
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.oppgave.domene.OppgaveRepository
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class OppgaveIntegrationTest(
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val oppgaveService: OppgaveService,
    @Autowired private val oppgaveRepository: OppgaveRepository,
    @Autowired private val personidentService: PersonidentService,
    @Autowired private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal opprette oppgave og ferdigstille oppgave for behandling`() {
        // Arrange
        val søkerFnr = randomFnr()
        val barnFnr = randomBarnFnr()
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barnFnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        // Act
        val godkjenneVedtakOppgaveId =
            oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        // Assert
        val opprettetOppgave =
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(Oppgavetype.GodkjenneVedtak, behandling)

        assertThat(opprettetOppgave).isNotNull
        assertThat(opprettetOppgave!!.type).isEqualTo(Oppgavetype.GodkjenneVedtak)
        assertThat(opprettetOppgave.behandling.id).isEqualTo(behandling.id)
        assertThat(opprettetOppgave.behandling.status).isEqualTo(behandling.status)
        assertThat(
            opprettetOppgave.behandling.behandlingStegTilstand
                .first()
                .behandlingSteg,
        ).isEqualTo(behandling.behandlingStegTilstand.first().behandlingSteg)
        assertThat(
            opprettetOppgave.behandling.behandlingStegTilstand
                .first()
                .behandlingStegStatus,
        ).isEqualTo(behandling.behandlingStegTilstand.first().behandlingStegStatus)
        assertThat(opprettetOppgave.erFerdigstilt).isFalse
        assertThat(opprettetOppgave.gsakId).isEqualTo(godkjenneVedtakOppgaveId)

        // Act
        oppgaveService.ferdigstillOppgaver(behandling.id, Oppgavetype.GodkjenneVedtak)

        // Assert
        assertThat(
            oppgaveRepository.findByOppgavetypeAndBehandlingAndIkkeFerdigstilt(
                Oppgavetype.GodkjenneVedtak,
                behandling,
            ),
        ).isNull()
    }

    @Test
    fun `Skal logge feil ved opprettelse av oppgave på type som ikke er ferdigstilt`() {
        // Arrange
        val søkerFnr = randomFnr()
        val barnFnr = randomBarnFnr()

        val logger: Logger = LoggerFactory.getLogger(OppgaveService::class.java) as Logger

        val listAppender: ListAppender<ILoggingEvent> = initLoggingEventListAppender()
        logger.addAppender(listAppender)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søkerFnr)
        val behandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))
        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barnFnr),
                søkerAktør = fagsak.aktør,
                barnAktør = barnAktør,
            )

        personopplysningGrunnlagRepository.save(personopplysningGrunnlag)

        // Act
        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())
        oppgaveService.opprettOppgave(behandling.id, Oppgavetype.GodkjenneVedtak, LocalDate.now())

        // Assert
        val loggingEvents = listAppender.list

        assertThat(loggingEvents)
            .extracting<String, RuntimeException> { obj: ILoggingEvent -> obj.formattedMessage }
            .anyMatch { message -> message.contains("Fant eksisterende oppgave med samme oppgavetype") }
    }

    private fun initLoggingEventListAppender(): ListAppender<ILoggingEvent> {
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        return listAppender
    }
}
