package no.nav.familie.ba.sak.kjerne.personident

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.ekstern.restDomene.FagsakDeltagerRolle
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.task.OpprettTaskService
import no.nav.familie.ba.sak.task.PatchMergetIdentTask
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.RekjørSenereException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class MergeIdentServiceTest {
    private val aktørIdRepository: AktørIdRepository = mockk()
    private val fagsakService: FagsakService = mockk()
    private val opprettTaskService: OpprettTaskService = mockk()
    private val persongrunnlagService: PersongrunnlagService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val behandlingRepository: BehandlingRepository = mockk()

    private val mergeIdentService =
        MergeIdentService(
            aktørIdRepository = aktørIdRepository,
            fagsakService = fagsakService,
            opprettTaskService = opprettTaskService,
            persongrunnlagService = persongrunnlagService,
            personopplysningerService = personopplysningerService,
            behandlingRepository = behandlingRepository,
        )

    val fnrGammel = randomFnr(LocalDate.of(2000, 1, 1))
    val aktørGammel = tilAktør(fnrGammel)

    val fnrNy = randomFnr(LocalDate.of(2000, 1, 1))
    val aktørNy = tilAktør(fnrGammel)

    val identInformasjonFraPdl =
        listOf(
            IdentInformasjon(aktørNy.aktørId, false, "AKTORID"),
            IdentInformasjon(fnrNy, false, "FOLKEREGISTERIDENT"),
            IdentInformasjon(aktørGammel.aktørId, true, "AKTORID"),
            IdentInformasjon(fnrGammel, true, "FOLKEREGISTERIDENT"),
        )

    @BeforeEach
    fun init() {
        every { aktørIdRepository.findByAktørIdOrNull(aktørGammel.aktørId) } returns aktørGammel
    }

    @Test
    fun `håndterNyIdent kaster Feil når det eksisterer flere fagsaker for identer`() {
        // arrange
        every { fagsakService.hentFagsakDeltager(any()) } returns
            listOf(
                RestFagsakDeltager(rolle = FagsakDeltagerRolle.BARN, fagsakId = 1),
                RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER, fagsakId = 2),
            )

        val feil =
            assertThrows<Feil> {
                // act
                mergeIdentService.mergeIdentOgRekjørSenere(identInformasjonFraPdl)
            }

        // assert
        assertThat(feil.message).startsWith("Det eksisterer flere fagsaker på identer som skal merges")
    }

    @Test
    fun `håndterNyIdent kaster Feil når det ikke eksisterer en fagsak for identer`() {
        // arrange
        every { fagsakService.hentFagsakDeltager(any()) } returns emptyList()

        // act
        val feil =
            assertThrows<Feil> {
                mergeIdentService.mergeIdentOgRekjørSenere(identInformasjonFraPdl)
            }

        // assert
        assertThat(feil.message).startsWith("Fant ingen fagsaker på identer som skal merges")
    }

    @Test
    fun `håndterNyIdent kaster Feil når det eksisterer en fagsak uten fagsakId for identer`() {
        // arrange
        every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER))

        // act
        val feil =
            assertThrows<Feil> {
                mergeIdentService.mergeIdentOgRekjørSenere(identInformasjonFraPdl)
            }

        // assert
        assertThat(feil.message).startsWith("Fant ingen fagsakId på fagsak for identer som skal merges")
    }

    @Test
    fun `håndterNyIdent kaster Feil når fødselsdato er endret for identer`() {
        // arrange
        every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER, fagsakId = 0))

        every { personopplysningerService.hentPersoninfoEnkel(any()) } returns
            PersonInfo(
                fødselsdato = LocalDate.of(2000, 2, 2),
            )
        val gammelBehandling = lagBehandling()
        every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns gammelBehandling

        every { persongrunnlagService.hentAktiv(any()) } returns
            PersonopplysningGrunnlag(
                behandlingId = gammelBehandling.id,
                personer =
                    mutableSetOf(
                        lagPerson(
                            aktør = aktørGammel,
                            fødselsdato = LocalDate.of(2000, 1, 1),
                        ),
                    ),
            )

        // act
        val feil =
            assertThrows<Feil> {
                mergeIdentService.mergeIdentOgRekjørSenere(identInformasjonFraPdl)
            }

        // assert
        assertThat(feil.message).startsWith("Fødselsdato er forskjellig fra forrige behandling. Må patche ny ident manuelt. Se")
    }

    @Test
    fun `håndterNyIdent lager en PatchMergetIdent task`() {
        // arrange
        every { fagsakService.hentFagsakDeltager(any()) } returns listOf(RestFagsakDeltager(rolle = FagsakDeltagerRolle.FORELDER, fagsakId = 0))
        every { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) } returns
            Task(
                type = PatchMergetIdentTask.TASK_STEP_TYPE,
                payload = "",
            )

        every { personopplysningerService.hentPersoninfoEnkel(any()) } returns
            PersonInfo(
                fødselsdato = LocalDate.of(2000, 1, 1),
            )
        val gammelBehandling = lagBehandling()
        every { behandlingRepository.finnSisteIverksatteBehandling(any()) } returns gammelBehandling

        every { persongrunnlagService.hentAktiv(any()) } returns
            PersonopplysningGrunnlag(
                behandlingId = gammelBehandling.id,
                personer =
                    mutableSetOf(
                        lagPerson(
                            aktør = aktørGammel,
                            fødselsdato = LocalDate.of(2000, 1, 1),
                        ),
                    ),
            )

        // act
        val feil =
            assertThrows<RekjørSenereException> {
                mergeIdentService.mergeIdentOgRekjørSenere(identInformasjonFraPdl)
            }

        // assert
        verify(exactly = 1) { opprettTaskService.opprettTaskForÅPatcheMergetIdent(any()) }
        assertThat(feil.årsak).startsWith("Mottok identhendelse som blir forsøkt patchet automatisk")
    }
}
