package no.nav.familie.ba.sak.kjerne.fagsaklåsing

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagPersonEnkel
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsfordelingsenhet
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.SaksstatistikkEventPublisher
import no.nav.familie.kontrakter.felles.BrukerIdType
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.dokarkiv.AvsluttSakRequest
import no.nav.familie.kontrakter.felles.dokarkiv.GjenåpneSakRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class FagsakLåsingServiceTest {
    private val fagsakRepository = mockk<FagsakRepository>()
    private val fagsakLåsingRepository = mockk<FagsakLåsingRepository>()
    private val integrasjonKlient = mockk<IntegrasjonKlient>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val saksstatistikkEventPublisher = mockk<SaksstatistikkEventPublisher>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    private val fagsakLåsingService =
        FagsakLåsingService(
            fagsakRepository = fagsakRepository,
            fagsakLåsingRepository = fagsakLåsingRepository,
            integrasjonKlient = integrasjonKlient,
            persongrunnlagService = persongrunnlagService,
            arbeidsfordelingService = arbeidsfordelingService,
            featureToggleService = featureToggleService,
            saksstatistikkEventPublisher = saksstatistikkEventPublisher,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
        )

    @Nested
    inner class LåsFagsak {
        private val fagsak = lagFagsak(status = FagsakStatus.AVSLUTTET)
        private val arbeidsfordelingsenhet = Arbeidsfordelingsenhet(enhetId = "1234", enhetNavn = "Enhet")
        private val yngsteBarnFødselsdato = LocalDate.now().minusYears(20)
        private val personer = setOf(lagPersonEnkel(personType = PersonType.BARN, fødselsdato = yngsteBarnFødselsdato))
        private var listAppender = ListAppender<ILoggingEvent>().apply { start() }

        @BeforeEach
        fun setup() {
            every { featureToggleService.isEnabled(FeatureToggle.KAN_LÅSE_FAGSAK) } returns true
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns null
            every { persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsak.id) } returns personer
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(fagsak.id) } returns false
            every { fagsakLåsingRepository.save(any()) } answers { firstArg() }
            every { fagsakRepository.save(fagsak) } returns fagsak
            every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just runs
            every { arbeidsfordelingService.hentArbeidsfordelingsenhetPåIdenter(fagsak, personer.map { it.aktør.aktivFødselsnummer() }, any()) } returns arbeidsfordelingsenhet
            every { integrasjonKlient.avsluttSak(any()) } just runs

            listAppender = ListAppender<ILoggingEvent>().apply { start() }
            (LoggerFactory.getLogger(FagsakLåsingService::class.java) as Logger).run {
                level = Level.INFO
                detachAndStopAllAppenders()
                addAppender(listAppender)
            }
        }

        @Test
        fun `skal låse avsluttet fagsak og sende melding til Joark`() {
            // Arrange
            val lagretLåsSlot = slot<FagsakLåsing>()
            val joarkRequestSlot = slot<AvsluttSakRequest>()

            // Mocker for å kunne sette `opprettetTidspunkt` til mer enn 30 dager siden
            val låstOppFagsakLåsing =
                mockk<FagsakLåsing>(relaxed = true) {
                    every { opprettetTidspunkt } returns LocalDateTime.now().minusDays(50)
                }

            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns låstOppFagsakLåsing
            every { fagsakLåsingRepository.save(capture(lagretLåsSlot)) } answers { firstArg() }
            every { integrasjonKlient.avsluttSak(capture(joarkRequestSlot)) } just runs

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(fagsak.status).isEqualTo(FagsakStatus.LÅST)
            assertThat(lagretLåsSlot.captured.hendelse).isEqualTo(FagsakLåsHendelse.LÅST)
            assertThat(lagretLåsSlot.captured.aktiv).isTrue()
            assertThat(lagretLåsSlot.captured.begrunnelse).isEqualTo("Automatisk låst iht. arkivloven fordi yngste barn fylte 18 år ${yngsteBarnFødselsdato.plusYears(18)}")
            assertThat(joarkRequestSlot.captured.fagsakId).isEqualTo(fagsak.id.toString())
            assertThat(joarkRequestSlot.captured.administrativEnhet).isEqualTo(arbeidsfordelingsenhet.enhetId)
            verify { saksstatistikkEventPublisher.publiserSaksstatistikk(fagsak.id) }
        }

        @Test
        fun `skal hoppe over låsing når toggle er av`() {
            // Arrange
            every { featureToggleService.isEnabled(FeatureToggle.KAN_LÅSE_FAGSAK) } returns false

            // Act
            fagsakLåsingService.låsFagsak(1)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Toggle for låsing av fagsak er av, hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.finnFagsak(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak som ikke er AVSLUTTET`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)

            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Status for fagsak ${fagsak.id} er LØPENDE, hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal hoppe over fagsak som har åpen behandling`() {
            // Arrange
            every { behandlingHentOgPersisterService.erÅpenBehandlingPåFagsak(fagsak.id) } returns true

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Fagsak ${fagsak.id} har åpen behandling, hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal kaste Feil hvis fagsak har FagsakLåsing med hendelse LÅST`() {
            // Arrange
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = LocalDateTime.now().minusDays(1),
                    hendelse = FagsakLåsHendelse.LÅST,
                    begrunnelse = "Allerede låst",
                    aktiv = true,
                )

            // Act
            val feil = assertThrows<Feil> { fagsakLåsingService.låsFagsak(fagsak.id) }

            // Assert
            assertThat(feil.message).isEqualTo("Fagsak ${fagsak.id} har allerede aktiv låsing")
        }

        @Test
        fun `skal hoppe over fagsak som ble låst opp for under 30 dager siden`() {
            // Arrange
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(fagsak.id) } returns
                FagsakLåsing(
                    fagsak = fagsak,
                    tidspunkt = LocalDateTime.now().minusDays(1),
                    hendelse = FagsakLåsHendelse.LÅST_OPP,
                    begrunnelse = "Låst opp",
                    aktiv = true,
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert
            assertThat(listAppender.list).anySatisfy {
                assertThat(it.level.toString()).isEqualTo("INFO")
                assertThat(it.formattedMessage).isEqualTo("Fagsak ${fagsak.id} ble låst opp for under 30 dager siden, hopper ut")
            }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal kaste Feil hvis fagsak ikke finnes`() {
            // Arrange
            every { fagsakRepository.finnFagsak(999) } returns null

            // Act & Assert
            assertThrows<Feil> { fagsakLåsingService.låsFagsak(999) }
        }

        @Test
        fun `skal kaste Feil hvis det ikke finnes barn på fagsak`() {
            // Arrange
            every { persongrunnlagService.hentSøkerOgBarnPåFagsak(any()) } returns null

            // Act & Assert
            assertThrows<Feil> { fagsakLåsingService.låsFagsak(fagsak.id) }
        }

        @Test
        fun `skal hoppe over fagsak der yngste barn ikke er gammelt nok`() {
            // Arrange
            every { persongrunnlagService.hentSøkerOgBarnPåFagsak(fagsak.id) } returns
                setOf(
                    lagPersonEnkel(
                        personType = PersonType.BARN,
                        fødselsdato = LocalDate.now().minusYears(17),
                    ),
                )

            // Act
            fagsakLåsingService.låsFagsak(fagsak.id)

            // Assert — ingen mutasjoner
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.avsluttSak(any()) }
        }

        @Test
        fun `skal propagere exception fra Joark slik at transaksjonen ruller tilbake`() {
            // Arrange
            every { integrasjonKlient.avsluttSak(any()) } throws RuntimeException("Joark er nede")

            // Act & Assert
            assertThrows<RuntimeException> { fagsakLåsingService.låsFagsak(fagsak.id) }
        }
    }

    @Nested
    inner class LåsOppFagsak {
        @BeforeEach
        fun setUp() {
            every { fagsakLåsingRepository.finnAktivLåsForFagsak(any()) } returns null
            every { fagsakLåsingRepository.save(any()) } answers { firstArg() }
            every { fagsakRepository.save(any()) } answers { firstArg() }
            every { integrasjonKlient.gjenåpneSakIDokarkiv(any()) } just runs
        }

        @Test
        fun `skal opprette FagsakLåsing med hendelse LÅST_OPP`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val låsingSlot = slot<FagsakLåsing>()
            every { fagsakLåsingRepository.save(capture(låsingSlot)) } answers { firstArg() }
            every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just runs

            // Act
            fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            // Assert
            assertThat(låsingSlot.captured.hendelse).isEqualTo(FagsakLåsHendelse.LÅST_OPP)
            assertThat(låsingSlot.captured.begrunnelse).isEqualTo("En god grunn")
            assertThat(låsingSlot.captured.fagsak.id).isEqualTo(fagsak.id)
            verify { saksstatistikkEventPublisher.publiserSaksstatistikk(fagsak.id) }
        }

        @Test
        fun `skal sette fagsak status til AVSLUTTET`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak
            every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just runs

            // Act
            val oppdatertFagsak = fagsakLåsingService.låsOppFagsak(fagsak.id, "En god grunn")

            // Assert
            assertThat(oppdatertFagsak.status).isEqualTo(FagsakStatus.AVSLUTTET)
            verify { fagsakRepository.save(match { it.status == FagsakStatus.AVSLUTTET }) }
            verify { saksstatistikkEventPublisher.publiserSaksstatistikk(fagsak.id) }
        }

        @Test
        fun `skal kalle gjenåpneSak på integrasjonsklienten med riktige verdier`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            val requestSlot = slot<GjenåpneSakRequest>()
            every { integrasjonKlient.gjenåpneSakIDokarkiv(capture(requestSlot)) } just runs
            every { saksstatistikkEventPublisher.publiserSaksstatistikk(any()) } just runs

            // Act
            fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")

            // Assert
            val request = requestSlot.captured
            assertThat(request.tema).isEqualTo(Tema.BAR)
            assertThat(request.fagsakId).isEqualTo(fagsak.id.toString())
            assertThat(request.fagsaksystem).isEqualTo(Fagsystem.BA)
            assertThat(request.bruker.idType).isEqualTo(BrukerIdType.FNR)
            assertThat(request.bruker.id).isEqualTo(fagsak.aktør.aktivFødselsnummer())
            verify { saksstatistikkEventPublisher.publiserSaksstatistikk(fagsak.id) }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis fagsak ikke har status LÅST`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LØPENDE)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            val feil =
                assertThrows<FunksjonellFeil> {
                    fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")
                }

            assertThat(feil.message).contains("LÅST")
            assertThat(feil.message).contains("LØPENDE")
            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal kaste FunksjonellFeil hvis begrunnelse er blank`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.LÅST)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "   ")
            }

            verify(exactly = 0) { fagsakLåsingRepository.save(any()) }
            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
        }

        @Test
        fun `skal ikke kalle integrasjonsklienten hvis fagsak har feil status`() {
            // Arrange
            val fagsak = lagFagsak(status = FagsakStatus.OPPRETTET)
            every { fagsakRepository.finnFagsak(fagsak.id) } returns fagsak

            // Act & Assert
            assertThrows<FunksjonellFeil> {
                fagsakLåsingService.låsOppFagsak(fagsak.id, "Begrunnelse")
            }

            verify(exactly = 0) { integrasjonKlient.gjenåpneSakIDokarkiv(any()) }
            verify(exactly = 0) { fagsakRepository.save(any()) }
        }
    }
}
