package no.nav.familie.ba.sak.integrasjoner.infotrygd

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.familie.ba.sak.common.EnvService
import no.nav.familie.ba.sak.common.fødselsnummerGenerator
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigreringServiceTest() {
    lateinit var migreringServiceMock: MigreringService

    @BeforeEach
    fun init() {
        val envServiceMock = mockk<EnvService>()
        every { envServiceMock.erPreprod() } returns false
        every { envServiceMock.erDev() } returns false
        migreringServiceMock = MigreringService(
            mockk(),
            mockk(),
            env = envServiceMock,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true),
            mockk(),
            mockk(),
            mockk()
        ) // => env.erDev() = env.erE2E() = false
    }

    @Test
    fun `migrering skal feile på, og dagen etter, kjøredato i Infotrygd`() {
        val virkningsdatoUtleder =
            MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoUtleder.trySetAccessible()

        listOf<Long>(0, 1).forEach { antallDagerEtterKjøredato ->
            val kjøredato = LocalDate.now().minusDays(antallDagerEtterKjøredato)

            assertThatThrownBy { virkningsdatoUtleder.invoke(migreringServiceMock, kjøredato) }
                .cause.isInstanceOf(KanIkkeMigrereException::class.java)
                .hasMessageContaining("Kjøring pågår. Vent med migrering til etter")
                .extracting("feiltype").isEqualTo(MigreringsfeilType.IKKE_GYLDIG_KJØREDATO)
        }
    }

    @Test
    fun `virkningsdatoFra skal returnere første dag i inneværende måned - minus en måned - når den kalles før kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            val kjøredato = this.plusDays(1)
            assertThat(virkningsdatoFra.invoke(migreringServiceMock, kjøredato)).isEqualTo(
                this.førsteDagIInneværendeMåned().minusMonths(1)
            )
        }
    }

    @Test
    fun `virkningsdatoFra skal returnere første dag i inneværende måned, 2 dager eller mer etter kjøredato i Infotrygd`() {
        val virkningsdatoFra = MigreringService::class.java.getDeclaredMethod("virkningsdatoFra", LocalDate::class.java)
        virkningsdatoFra.trySetAccessible()

        LocalDate.now().run {
            listOf<Long>(2, 3).forEach { antallDagerEtterKjøredato ->
                val kjøredato = this.minusDays(antallDagerEtterKjøredato)
                assertThat(
                    virkningsdatoFra.invoke(
                        migreringServiceMock,
                        kjøredato
                    )
                ).isEqualTo(this.førsteDagIInneværendeMåned())
            }
        }
    }

    @Test
    fun `migrering skal feile med IDENT_IKKE_LENGER_AKTIV når input har ident som er historisk i PDL`() {
        val mockkPersonidentService = mockk<PersonidentService>()
        val s = MigreringService(
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockkPersonidentService,
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(),
            mockk(relaxed = true),
            mockk(),
            mockk(),
            mockk()
        )

        val aktivFnr = randomFnr()
        val historiskFnr = randomFnr()

        every { mockkPersonidentService.hentIdenter(historiskFnr, true) } returns listOf(
            IdentInformasjon(aktivFnr, false, "FOLKEREGISTERIDENT"),
            IdentInformasjon(historiskFnr, true, "FOLKEREGISTERIDENT"),
            IdentInformasjon("112244", false, "AKTOERID")
        )

        assertThatThrownBy {
            s.migrer(historiskFnr)
        }.isInstanceOf(KanIkkeMigrereException::class.java)
            .hasMessage(null)
            .extracting("feiltype").isEqualTo(MigreringsfeilType.IDENT_IKKE_LENGER_AKTIV)
    }

    @Test
    fun `Skal hente kjøredate hvis man har kjøredato eller så kastes kan ikke migrerere exception`() {
        val service = MigreringService(
            behandlingRepository = mockk(),
            behandlingService = mockk(),
            env = mockk(),
            fagsakService = mockk(),
            infotrygdBarnetrygdClient = mockk(),
            personidentService = mockk(),
            stegService = mockk(),
            taskRepository = mockk(),
            tilkjentYtelseRepository = mockk(),
            totrinnskontrollService = mockk(),
            vedtakService = mockk(),
            vilkårService = mockk(),
            vilkårsvurderingService = mockk(),
            migreringRestClient = mockk(relaxed = true),
            mockk(),
            mockk(),
            mockk()
        )
        assertThat(service.infotrygdKjøredato(YearMonth.of(2022, Month.NOVEMBER))).isEqualTo(
            LocalDate.of(
                2022,
                Month.NOVEMBER,
                17
            )
        )
        assertThat(service.infotrygdKjøredato(YearMonth.of(2023, Month.SEPTEMBER))).isEqualTo(
            LocalDate.of(
                2023,
                Month.SEPTEMBER,
                18
            )
        )
        assertThrows<KanIkkeMigrereException> { service.infotrygdKjøredato(YearMonth.now().plusYears(2)) }
    }

    @Test
    fun `Hvis denne testen feiler og man fortsatt migrererer, så må man ha ny kjøreplan, hvis man er ferdig med migrering, så kan man rydde opp kode`() {
        val service = MigreringService(
            behandlingRepository = mockk(),
            behandlingService = mockk(),
            env = mockk(),
            fagsakService = mockk(),
            infotrygdBarnetrygdClient = mockk(),
            personidentService = mockk(),
            stegService = mockk(),
            taskRepository = mockk(),
            tilkjentYtelseRepository = mockk(),
            totrinnskontrollService = mockk(),
            vedtakService = mockk(),
            vilkårService = mockk(),
            vilkårsvurderingService = mockk(),
            migreringRestClient = mockk(relaxed = true),
            mockk(),
            mockk(),
            mockk()
        )
        service.infotrygdKjøredato(YearMonth.now().plusMonths(1))
    }

    @Test
    fun `sammenlingBarnInfotrygdMedBarnBAsak - ikke kast feil hvis saken i infotrygd har barn med historisk ident, mens saken i ba-sak har barn med den aktive identen`() {
        val mockPersongrunnlagService = mockk<PersongrunnlagService>()
        val mockPersonidentService = mockk<PersonidentService>()
        val service = MigreringService(
            behandlingRepository = mockk(),
            behandlingService = mockk(),
            env = mockk(),
            fagsakService = mockk(),
            infotrygdBarnetrygdClient = mockk(),
            personidentService = mockPersonidentService,
            stegService = mockk(),
            taskRepository = mockk(),
            tilkjentYtelseRepository = mockk(),
            totrinnskontrollService = mockk(),
            vedtakService = mockk(),
            vilkårService = mockk(),
            vilkårsvurderingService = mockk(),
            migreringRestClient = mockk(relaxed = true),
            kompetanseService = mockk(),
            featureToggleService = mockk(),
            persongrunnlagService = mockPersongrunnlagService
        )

        val behandling = lagBehandling()

        val barnetsAktiveIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(1000)).asString
        val barnetsHistoriskeIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(1001)).asString

        every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(
            lagPerson(
                PersonIdent(
                    barnetsAktiveIdent
                )
            )
        )

        every { mockPersonidentService.hentIdenter(barnetsAktiveIdent, false) } returns listOf(
            IdentInformasjon(barnetsAktiveIdent, false, "FOLKEREGISTERIDENT")
        )
        every { mockPersonidentService.hentIdenter(barnetsHistoriskeIdent, true) } returns listOf(
            IdentInformasjon(barnetsAktiveIdent, false, "FOLKEREGISTERIDENT"),
            IdentInformasjon(barnetsHistoriskeIdent, true, "FOLKEREGISTERIDENT")
        )
        every { mockPersonidentService.hentIdenter(barnetsHistoriskeIdent, false) } returns listOf(
            IdentInformasjon(barnetsAktiveIdent, false, "FOLKEREGISTERIDENT")
        )

        service.sammenlingBarnInfotrygdMedBarnBAsak(behandling, listOf(barnetsHistoriskeIdent), "123")
    }

    @Test
    fun `sammenlingBarnInfotrygdMedBarnBAsak - ikke kast feil hvis saken i infotrygd har 1 av 2 barn med historisk ident, mens saken i ba-sak har barn med den aktive identen`() {
        val mockPersongrunnlagService = mockk<PersongrunnlagService>()
        val mockPersonidentService = mockk<PersonidentService>()
        val service = MigreringService(
            behandlingRepository = mockk(),
            behandlingService = mockk(),
            env = mockk(),
            fagsakService = mockk(),
            infotrygdBarnetrygdClient = mockk(),
            personidentService = mockPersonidentService,
            stegService = mockk(),
            taskRepository = mockk(),
            tilkjentYtelseRepository = mockk(),
            totrinnskontrollService = mockk(),
            vedtakService = mockk(),
            vilkårService = mockk(),
            vilkårsvurderingService = mockk(),
            migreringRestClient = mockk(relaxed = true),
            kompetanseService = mockk(),
            featureToggleService = mockk(),
            persongrunnlagService = mockPersongrunnlagService
        )

        val behandling = lagBehandling()
        val barn1AktivIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(1000)).asString
        val barn2AktivIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(1000)).asString
        val barn2historiskIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(100)).asString

        every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(
            lagPerson(
                PersonIdent(
                    barn1AktivIdent
                )
            ),
            lagPerson(
                PersonIdent(
                    barn2AktivIdent
                )
            )
        )

        every { mockPersonidentService.hentIdenter(barn1AktivIdent, true) } returns listOf(
            IdentInformasjon(barn1AktivIdent, false, "FOLKEREGISTERIDENT")
        )

        every { mockPersonidentService.hentIdenter(barn2historiskIdent, true) } returns listOf(
            IdentInformasjon(barn2AktivIdent, false, "FOLKEREGISTERIDENT"),
            IdentInformasjon(barn2historiskIdent, true, "FOLKEREGISTERIDENT")
        )
        every { mockPersonidentService.hentIdenter(barn2historiskIdent, false) } returns listOf(
            IdentInformasjon(barn2AktivIdent, false, "FOLKEREGISTERIDENT")
        )

        service.sammenlingBarnInfotrygdMedBarnBAsak(
            behandling,
            listOf(barn1AktivIdent, barn2historiskIdent),
            "123"
        )
    }

    @Test
    fun `sammenlingBarnInfotrygdMedBarnBAsak - kast DIFF_BARN_INFOTRYGD_OG_BA_SAK hvis saken i infotrygd har en historisk ident, mens saken i ba-sak har en aktiv ident som ikke tilhører den historiske identen`() {
        val mockPersongrunnlagService = mockk<PersongrunnlagService>()
        val mockPersonidentService = mockk<PersonidentService>()
        val service = MigreringService(
            behandlingRepository = mockk(),
            behandlingService = mockk(),
            env = mockk(),
            fagsakService = mockk(),
            infotrygdBarnetrygdClient = mockk(),
            personidentService = mockPersonidentService,
            stegService = mockk(),
            taskRepository = mockk(),
            tilkjentYtelseRepository = mockk(),
            totrinnskontrollService = mockk(),
            vedtakService = mockk(),
            vilkårService = mockk(),
            vilkårsvurderingService = mockk(),
            migreringRestClient = mockk(relaxed = true),
            kompetanseService = mockk(),
            featureToggleService = mockk(),
            persongrunnlagService = mockPersongrunnlagService
        )

        val behandling = lagBehandling()

        val barnetsAktiveIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(1000)).asString
        val barnetsHistoriskeIdent = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(1001)).asString
        val aktivIdentPåBaSak = fødselsnummerGenerator.foedselsnummer(LocalDate.now().minusDays(100)).asString

        every { mockPersongrunnlagService.hentBarna(behandling) } returns listOf(
            lagPerson(
                PersonIdent(
                    aktivIdentPåBaSak
                )
            )
        )

        every { mockPersonidentService.hentIdenter(barnetsHistoriskeIdent, false) } returns listOf(
            IdentInformasjon(barnetsAktiveIdent, false, "FOLKEREGISTERIDENT")
        )
        every { mockPersonidentService.hentIdenter(barnetsHistoriskeIdent, true) } returns listOf(
            IdentInformasjon(barnetsAktiveIdent, false, "FOLKEREGISTERIDENT"),
            IdentInformasjon(barnetsHistoriskeIdent, true, "FOLKEREGISTERIDENT")
        )

        assertThat(
            assertThrows<KanIkkeMigrereException> {
                service.sammenlingBarnInfotrygdMedBarnBAsak(
                    behandling,
                    listOf(barnetsHistoriskeIdent),
                    "123"
                )
            }.feiltype
        ).isEqualTo(MigreringsfeilType.DIFF_BARN_INFOTRYGD_OG_BA_SAK)
    }
}
