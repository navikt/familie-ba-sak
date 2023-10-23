package no.nav.familie.ba.sak.internal

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.common.tilPersonEnkelSøkerOgBarn
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlIdentRestClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLogg
import no.nav.familie.ba.sak.kjerne.personident.AktørMergeLoggRepository
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ForvalterServiceTest {

    private val økonomiService = mockk<ØkonomiService>()
    private val vedtakService = mockk<VedtakService>()
    private val beregningService = mockk<BeregningService>()
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val stegService = mockk<StegService>()
    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()
    private val taskRepository = mockk<TaskRepositoryWrapper>()
    private val autovedtakService = mockk<AutovedtakService>()
    private val fagsakRepository = mockk<FagsakRepository>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val tilkjentYtelseValideringService = mockk<TilkjentYtelseValideringService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val infotrygdService = mockk<InfotrygdService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val pdlIdentRestClient = mockk<PdlIdentRestClient>()
    private val personidentService = mockk<PersonidentService>(relaxed = true)
    private val aktørIdRepository = mockk<AktørIdRepository>(relaxed = true)
    private val vilkårsvurderingService = mockk<VilkårsvurderingService>()
    private val aktørMergeLoggRepository = mockk<AktørMergeLoggRepository>(relaxed = true)

    private val service = ForvalterService(
        økonomiService,
        vedtakService,
        beregningService,
        behandlingHentOgPersisterService,
        stegService,
        fagsakService,
        behandlingService,
        taskRepository,
        autovedtakService,
        fagsakRepository,
        behandlingRepository,
        tilkjentYtelseValideringService,
        arbeidsfordelingService,
        infotrygdService,
        persongrunnlagService,
        pdlIdentRestClient,
        personidentService,
        aktørIdRepository,
        vilkårsvurderingService,
        aktørMergeLoggRepository,
    )

    private val barnetsGamleAktør = tilAktør(randomFnr())
    private val barnetsNyeAktør = tilAktør(randomFnr())
    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak)
    private val søkerAktør = fagsak.aktør
    private val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(
        behandlingId = behandling.id,
        søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
        barnasIdenter = listOf(barnetsGamleAktør.aktivFødselsnummer()),
    )

    @Test
    fun `Skal kunne patche barnets ident for en fagsak hvor gammel ident er en historisk ident av ny ident`() {
        val dto = PatchIdentForBarnPåFagsak(
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
        every { behandlingHentOgPersisterService.finnAktivForFagsak(fagsak.id) } returns behandling
        every { persongrunnlagService.hentBarna(behandling) } returns listOf(
            lagPerson(
                personIdent = no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                type = PersonType.BARN,
            ),
        )
        every { personidentService.hentOgLagreAktør(barnetsNyeAktør.aktivFødselsnummer(), true) } returns barnetsNyeAktør
        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns lagVilkårsvurdering(barnetsGamleAktør, behandling, Resultat.IKKE_VURDERT)
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        service.patchIdentForBarnPåFagsak(dto)

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
        val dto = PatchIdentForBarnPåFagsak(
            fagsakId = fagsak.id,
            gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
            nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
        )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns emptySet()

        assertThrows<IllegalStateException> { service.patchIdentForBarnPåFagsak(dto) }.also {
            assertThat(it.message).isEqualTo("Fant ikke ident som skal patches som barn på fagsak=${fagsak.id}")
        }
    }

    @Test
    fun `Skal kaste feil ved patching av ident hvis gammel ident ikke er historisk av ny ident`() {
        val dto = PatchIdentForBarnPåFagsak(
            fagsakId = fagsak.id,
            gammelIdent = PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
            nyIdent = PersonIdent(barnetsNyeAktør.aktivFødselsnummer()),
        )

        every { persongrunnlagService.hentSøkerOgBarnPåFagsak(dto.fagsakId) } returns personopplysningGrunnlag.tilPersonEnkelSøkerOgBarn().toSet()
        every { pdlIdentRestClient.hentIdenter(barnetsNyeAktør.aktivFødselsnummer(), true) } returns emptyList()

        assertThrows<IllegalStateException> { service.patchIdentForBarnPåFagsak(dto) }.also {
            assertThat(it.message).isEqualTo("Ident som skal patches finnes ikke som historisk ident av ny ident")
        }
    }

    @Test
    fun `Skal kunne patche barnets ident for en fagsak hvor gammel ident ikke er en historisk ident av ny ident, men man har valgt å overstyre`() {
        val dto = PatchIdentForBarnPåFagsak(
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
        every { behandlingHentOgPersisterService.finnAktivForFagsak(fagsak.id) } returns behandling
        every { persongrunnlagService.hentBarna(behandling) } returns listOf(
            lagPerson(
                personIdent = no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent(barnetsGamleAktør.aktivFødselsnummer()),
                type = PersonType.BARN,
            ),
        )
        every { personidentService.hentOgLagreAktør(barnetsNyeAktør.aktivFødselsnummer(), true) } returns barnetsNyeAktør
        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns lagVilkårsvurdering(barnetsGamleAktør, behandling, Resultat.IKKE_VURDERT)
        val aktørMergeLoggSlot = slot<AktørMergeLogg>()
        every { aktørMergeLoggRepository.save(capture(aktørMergeLoggSlot)) } answers { aktørMergeLoggSlot.captured }

        service.patchIdentForBarnPåFagsak(dto)

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
