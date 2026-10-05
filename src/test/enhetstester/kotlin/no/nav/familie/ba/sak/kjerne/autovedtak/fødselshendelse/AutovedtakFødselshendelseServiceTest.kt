package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagForelderBarnRelasjon
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonInfo
import no.nav.familie.ba.sak.integrasjoner.oppgave.OppgaveService
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.FødselshendelseData
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.filtreringsregler.FiltreringsreglerFødselshendelseService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.HenleggÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandlingHendelse
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.FiltrerAutomatiskBehandlingData
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import no.nav.familie.ba.sak.task.dto.ManuellOppgaveType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AutovedtakFødselshendelseServiceTest {
    private val filtreringsreglerFødselshendelseService = mockk<FiltreringsreglerFødselshendelseService>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val personidentService = mockk<PersonidentService>()
    private val stegService = mockk<StegService>()
    private val autovedtakService = mockk<AutovedtakService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val oppgaveService = mockk<OppgaveService>()
    private val autovedtakFødselshendelseBegrunnelseService = mockk<AutovedtakFødselshendelseBegrunnelseService>()

    private val mor = lagPerson()
    private val barn = lagPerson()
    private val fagsak = defaultFagsak(aktør = mor.aktør)
    private val behandling = lagBehandling(fagsak = fagsak)
    private val hendelse = NyBehandlingHendelse(mor.aktør.aktivFødselsnummer(), listOf(barn.aktør.aktivFødselsnummer()))
    private val data = FiltrerAutomatiskBehandlingData(hendelse.morsIdent, hendelse.barnasIdenter)

    private val service =
        AutovedtakFødselshendelseService(
            fagsakService,
            behandlingHentOgPersisterService,
            filtreringsreglerFødselshendelseService,
            taskRepository,
            vilkårsvurderingRepository,
            persongrunnlagService,
            personidentService,
            stegService,
            autovedtakService,
            personopplysningerService,
            oppgaveService,
            autovedtakFødselshendelseBegrunnelseService,
        )

    @Nested
    inner class KjørBehandling {
        @BeforeEach
        fun setup() {
            every { personidentService.hentAktør(hendelse.morsIdent) } returns mor.aktør
            every { fagsakService.hentNormalFagsak(mor.aktør) } returns fagsak
            every { behandlingHentOgPersisterService.hentBehandlinger(fagsak.id) } returns emptyList()
            every { stegService.opprettNyBehandlingOgRegistrerPersongrunnlagForFødselhendelse(hendelse) } returns behandling

            every {
                personopplysningerService.hentPersoninfoMedRelasjonerOgRegisterinformasjon(mor.aktør)
            } returns
                lagPersonInfo(
                    fødselsdato = mor.fødselsdato,
                    navn = "Mor",
                    kjønn = Kjønn.KVINNE,
                    forelderBarnRelasjon = setOf(lagForelderBarnRelasjon(aktør = barn.aktør)),
                )
        }

        @Test
        fun `skal bruke sammenslått begrunnelse ved henleggelse når flere filtreringsregler ikke er oppfylt`() {
            // Arrange
            every {
                filtreringsreglerFødselshendelseService.finnBegrunnelseForHenleggingAvBehandling(behandling.id)
            } returns "1) Mor har registrert dødsdato. 2) Barnet har registrert dødsdato."

            every {
                stegService.håndterFiltreringsreglerForAutomatiskeBehandlinger(behandling, data)
            } returns behandling.leggTilBehandlingStegTilstand(StegType.HENLEGG_BEHANDLING)

            every { stegService.håndterHenleggBehandling(any(), any()) } returns behandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) } returns ""

            // Act
            service.kjørBehandling(FødselshendelseData(hendelse))

            // Assert
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    begrunnelse = "Fødselshendelse: 1) Mor har registrert dødsdato. 2) Barnet har registrert dødsdato.",
                    manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                )
            }
            verify(exactly = 1) {
                stegService.håndterHenleggBehandling(
                    behandling = behandling,
                    henleggBehandlingInfo =
                        match {
                            it.årsak == HenleggÅrsak.AUTOMATISK_HENLAGT &&
                                it.begrunnelse == "1) Mor har registrert dødsdato. 2) Barnet har registrert dødsdato."
                        },
                )
            }
        }

        @Test
        fun `skal bruke begrunnelsen ved henleggelse når kun en filtreringsregel ikke er oppfylt`() {
            // Arrange
            every {
                filtreringsreglerFødselshendelseService.finnBegrunnelseForHenleggingAvBehandling(behandling.id)
            } returns "Mor har registrert dødsdato."

            every {
                stegService.håndterFiltreringsreglerForAutomatiskeBehandlinger(behandling, data)
            } returns behandling.leggTilBehandlingStegTilstand(StegType.HENLEGG_BEHANDLING)

            every { stegService.håndterHenleggBehandling(any(), any()) } returns behandling
            every { oppgaveService.opprettOppgaveForManuellBehandling(any(), any(), any(), any(), any()) } returns ""

            // Act
            service.kjørBehandling(FødselshendelseData(hendelse))

            // Assert
            verify(exactly = 1) {
                oppgaveService.opprettOppgaveForManuellBehandling(
                    behandlingId = behandling.id,
                    begrunnelse = "Fødselshendelse: Mor har registrert dødsdato.",
                    manuellOppgaveType = ManuellOppgaveType.FØDSELSHENDELSE,
                    oppgavetype = Oppgavetype.VurderLivshendelse,
                )
            }
            verify(exactly = 1) {
                stegService.håndterHenleggBehandling(
                    behandling = behandling,
                    henleggBehandlingInfo =
                        match {
                            it.årsak == HenleggÅrsak.AUTOMATISK_HENLAGT &&
                                it.begrunnelse == "Mor har registrert dødsdato."
                        },
                )
            }
        }
    }
}
