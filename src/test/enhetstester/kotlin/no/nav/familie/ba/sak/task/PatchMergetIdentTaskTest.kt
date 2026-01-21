package no.nav.familie.ba.sak.task

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.datagenerator.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.personident.PersonidentRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PatchMergetIdentTaskTest {
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val pdlIdentRestKlient = mockk<PdlIdentRestKlient>()
    private val personidentService = mockk<PersonidentService>(relaxed = true)
    private val aktørIdRepository = mockk<AktørIdRepository>(relaxed = true)
    private val aktørMergeLoggRepository = mockk<AktørMergeLoggRepository>(relaxed = true)
    private val personidentRepository = mockk<PersonidentRepository>()

    private val task =
        PatchMergetIdentTask(
            persongrunnlagService = persongrunnlagService,
            pdlIdentRestKlient = pdlIdentRestKlient,
            personidentService = personidentService,
            aktørIdRepository = aktørIdRepository,
            aktørMergeLoggRepository = aktørMergeLoggRepository,
            personidentRepository = personidentRepository,
        )

    private val gammelAktør = lagAktør(randomFnr())
    private val nyAktør = lagAktør(randomFnr())
    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val søkerAktør = fagsak.aktør
    private val personopplysningGrunnlag =
        lagTestPersonopplysningGrunnlag(
            behandlingId = behandling.id,
            søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
            barnasIdenter = listOf(gammelAktør.aktivFødselsnummer()),
        )

    @Test
    fun `Skal kunne patche barnets ident for en fagsak hvor gammel ident er en historisk ident av ny ident`() {
        val dto =
            PatchMergetIdentDto(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(gammelAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(nyAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestKlient.hentIdenter(nyAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(gammelAktør.aktørId, true, "AKTORID"),
                IdentInformasjon(gammelAktør.aktivFødselsnummer(), true, "FOLKEREGISTERIDENT"),
                IdentInformasjon(nyAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(nyAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentService.hentOgLagreAktør(nyAktør.aktivFødselsnummer(), true) } returns nyAktør
        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns null
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        task.doTask(Task(payload = jsonMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE))

        val aktørMergeLogg = aktørMergeLoggSlot.captured
        assertThat(aktørMergeLogg.nyAktørId).isEqualTo(nyAktør.aktørId)
        assertThat(aktørMergeLogg.fagsakId).isEqualTo(dto.fagsakId)
        assertThat(aktørMergeLogg.historiskAktørId).isEqualTo(gammelAktør.aktørId)
        assertThat(aktørMergeLogg.mergeTidspunkt).isCloseTo(
            LocalDateTime.now(),
            Assertions.within(10, ChronoUnit.SECONDS),
        )
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis ident på barnet ikke finnes på fagsaken`() {
        val dto =
            PatchMergetIdentDto(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(gammelAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(nyAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns emptySet()

        assertThrows<Feil> { task.doTask(Task(payload = jsonMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE)) }.also {
            assertThat(it.message).isEqualTo("Fant ikke ident som skal patches på fagsak=${fagsak.id} aktører=[]")
        }
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis gammel ident ikke er historisk av ny ident`() {
        val dto =
            PatchMergetIdentDto(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(gammelAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(nyAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestKlient.hentIdenter(nyAktør.aktivFødselsnummer(), true) } returns emptyList()

        assertThrows<Feil> { task.doTask(Task(payload = jsonMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE)) }.also {
            assertThat(it.message).isEqualTo("Ident som skal patches finnes ikke som historisk ident av ny ident")
        }
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis ny personident allerede eksisterer i personident`() {
        val dto =
            PatchMergetIdentDto(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(gammelAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(nyAktør.aktivFødselsnummer()),
                skalSjekkeAtGammelIdentErHistoriskAvNyIdent = false,
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestKlient.hentIdenter(nyAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(nyAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(nyAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns Personident(nyAktør.aktivFødselsnummer(), nyAktør)

        assertThrows<Feil> { task.doTask(Task(payload = jsonMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE)) }.also {
            assertThat(it.message).isEqualTo("Fant allerede en personident for nytt fødselsnummer")
        }
    }

    @Test
    fun `Skal kunne patche barnets ident for en fagsak hvor gammel ident ikke er en historisk ident av ny ident, men man har valgt å overstyre`() {
        val dto =
            PatchMergetIdentDto(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(gammelAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(nyAktør.aktivFødselsnummer()),
                skalSjekkeAtGammelIdentErHistoriskAvNyIdent = false,
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestKlient.hentIdenter(nyAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(nyAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(nyAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentService.hentOgLagreAktør(nyAktør.aktivFødselsnummer(), true) } returns nyAktør
        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns null
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        task.doTask(Task(payload = jsonMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE))

        val aktørMergeLogg = aktørMergeLoggSlot.captured
        assertThat(aktørMergeLogg.nyAktørId).isEqualTo(nyAktør.aktørId)
        assertThat(aktørMergeLogg.fagsakId).isEqualTo(dto.fagsakId)
        assertThat(aktørMergeLogg.historiskAktørId).isEqualTo(gammelAktør.aktørId)
        assertThat(aktørMergeLogg.mergeTidspunkt).isCloseTo(
            LocalDateTime.now(),
            Assertions.within(10, ChronoUnit.SECONDS),
        )
    }

    @Test
    fun `Skal kunne patche søkers ident for en fagsak hvor gammel ident er en historisk ident av ny ident`() {
        val dto =
            PatchMergetIdentDto(
                fagsakId = fagsak.id,
                gammelIdent = PersonIdent(søkerAktør.aktivFødselsnummer()),
                nyIdent = PersonIdent(nyAktør.aktivFødselsnummer()),
            )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestKlient.hentIdenter(nyAktør.aktivFødselsnummer(), true) } returns
            listOf(
                IdentInformasjon(søkerAktør.aktørId, true, "AKTORID"),
                IdentInformasjon(søkerAktør.aktivFødselsnummer(), true, "FOLKEREGISTERIDENT"),
                IdentInformasjon(nyAktør.aktørId, false, "AKTORID"),
                IdentInformasjon(nyAktør.aktivFødselsnummer(), false, "FOLKEREGISTERIDENT"),
            )

        every { personidentService.hentOgLagreAktør(nyAktør.aktivFødselsnummer(), true) } returns nyAktør
        every { personidentRepository.findByFødselsnummerOrNull(dto.nyIdent.ident) } returns null
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        task.doTask(Task(payload = jsonMapper.writeValueAsString(dto), type = PatchMergetIdentTask.TASK_STEP_TYPE))

        val aktørMergeLogg = aktørMergeLoggSlot.captured
        assertThat(aktørMergeLogg.nyAktørId).isEqualTo(nyAktør.aktørId)
        assertThat(aktørMergeLogg.fagsakId).isEqualTo(dto.fagsakId)
        assertThat(aktørMergeLogg.historiskAktørId).isEqualTo(søkerAktør.aktørId)
        assertThat(aktørMergeLogg.mergeTidspunkt).isCloseTo(
            LocalDateTime.now(),
            Assertions.within(10, ChronoUnit.SECONDS),
        )
    }
}
