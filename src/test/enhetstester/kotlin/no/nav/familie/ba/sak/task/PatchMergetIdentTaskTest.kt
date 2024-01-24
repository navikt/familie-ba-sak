package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PatchMergetIdentTaskTest {
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val pdlIdentRestClient = mockk<PdlIdentRestClient>()
    private val personidentService = mockk<PersonidentService>(relaxed = true)
    private val aktørIdRepository = mockk<AktørIdRepository>(relaxed = true)
    private val aktørMergeLoggRepository = mockk<AktørMergeLoggRepository>(relaxed = true)
    private val personidentRepository = mockk<PersonidentRepository>()

    private val task =
        PatchMergetIdentTask(
            persongrunnlagService = persongrunnlagService,
            pdlIdentRestClient = pdlIdentRestClient,
            personidentService = personidentService,
            aktørIdRepository = aktørIdRepository,
            aktørMergeLoggRepository = aktørMergeLoggRepository,
            personidentRepository = personidentRepository,
        )

    private val barnetsGamleAktør = tilAktør(randomFnr())
    private val barnetsNyeAktør = tilAktør(randomFnr())
    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val søkerAktør = fagsak.aktør
    private val personopplysningGrunnlag =
        lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
            barnasIdenter = listOf(barnetsGamleAktør.aktivFødselsnummer()),
        )

    @Test
    fun `Skal kunne patche barnets ident for en fagsak hvor gammel ident er en historisk ident av ny ident`() {
        val dto =
            PatchIdentForBarnPåFagsak(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestClient.hentIdenter(barnetsNyeAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(barnetsGamleAktør.aktørId, true, "AKTORID"),
                IdentInformasjon(barnetsGamleAktør.aktivFødselsnummer(), true, "FOLKEREGISTERIDENT"),
                IdentInformasjon(barnetsNyeAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(barnetsNyeAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentService.hentOgLagreAktør(barnetsNyeAktør.aktivFødselsnummer(), true) } returns barnetsNyeAktør
        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns null
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        task.doTask(Task(payload = objectMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE))

        val aktørMergeLogg = aktørMergeLoggSlot.captured
        assertThat(aktørMergeLogg.nyAktørId).isEqualTo(barnetsNyeAktør.aktørId)
        assertThat(aktørMergeLogg.fagsakId).isEqualTo(dto.fagsakId)
        assertThat(aktørMergeLogg.historiskAktørId).isEqualTo(barnetsGamleAktør.aktørId)
        assertThat(aktørMergeLogg.mergeTidspunkt).isCloseTo(
            LocalDateTime.now(),
            Assertions.within(10, ChronoUnit.SECONDS),
        )
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis ident på barnet ikke finnes på fagsaken`() {
        val dto =
            PatchIdentForBarnPåFagsak(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns emptySet()

        assertThrows<IllegalStateException> { task.doTask(Task(payload = objectMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE)) }.also {
            assertThat(it.message).isEqualTo("Fant ikke ident som skal patches som barn på fagsak=${fagsak.id} aktører=[]")
        }
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis gammel ident ikke er historisk av ny ident`() {
        val dto =
            PatchIdentForBarnPåFagsak(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestClient.hentIdenter(barnetsNyeAktør.aktivFødselsnummer(), true) } returns emptyList()

        assertThrows<IllegalStateException> { task.doTask(Task(payload = objectMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE)) }.also {
            assertThat(it.message).isEqualTo("Ident som skal patches finnes ikke som historisk ident av ny ident")
        }
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis ny personident allerede eksisterer i personident`() {
        val dto =
            PatchIdentForBarnPåFagsak(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
                skalSjekkeAtGammelIdentErHistoriskAvNyIdent = false,
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestClient.hentIdenter(barnetsNyeAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(barnetsNyeAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(barnetsNyeAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns Personident(barnetsNyeAktør.aktivFødselsnummer(), barnetsNyeAktør)

        assertThrows<IllegalStateException> { task.doTask(Task(payload = objectMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE)) }.also {
            assertThat(it.message).isEqualTo("Fant allerede en personident for nytt fødselsnummer")
        }
    }

    @Test
    fun `Skal kunne patche barnets ident for en fagsak hvor gammel ident ikke er en historisk ident av ny ident, men man har valgt å overstyre`() {
        val dto =
            PatchIdentForBarnPåFagsak(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
                skalSjekkeAtGammelIdentErHistoriskAvNyIdent = false,
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestClient.hentIdenter(barnetsNyeAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(barnetsNyeAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(barnetsNyeAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentService.hentOgLagreAktør(barnetsNyeAktør.aktivFødselsnummer(), true) } returns barnetsNyeAktør
        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns null
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        task.doTask(Task(payload = objectMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE))

        val aktørMergeLogg = aktørMergeLoggSlot.captured
        assertThat(aktørMergeLogg.nyAktørId).isEqualTo(barnetsNyeAktør.aktørId)
        assertThat(aktørMergeLogg.fagsakId).isEqualTo(dto.fagsakId)
        assertThat(aktørMergeLogg.historiskAktørId).isEqualTo(barnetsGamleAktør.aktørId)
        assertThat(aktørMergeLogg.mergeTidspunkt).isCloseTo(
            LocalDateTime.now(),
            Assertions.within(10, ChronoUnit.SECONDS),
        )
    }
}
