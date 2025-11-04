package no.nav.familie.ba.sak.kjerne.autovedtak.finnmarkstillegg

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.AutovedtakMåBehandlesManueltFeil
import no.nav.familie.ba.sak.datagenerator.defaultFagsak
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagFagsak
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.FinnmarkstilleggData
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus.LØPENDE
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType.NORMAL
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class AutovedtakFinnmarkstilleggServiceTest {
    private val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val beregningService = mockk<BeregningService>()
    private val fagsakService = mockk<FagsakService>()
    private val pdlRestKlient = mockk<SystemOnlyPdlRestKlient>()
    private val simuleringService = mockk<SimuleringService>()
    private val autovedtakFinnmarkstilleggBegrunnelseService = mockk<AutovedtakFinnmarkstilleggBegrunnelseService>()

    private val autovedtakFinnmarkstilleggService =
        AutovedtakFinnmarkstilleggService(
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            persongrunnlagService = persongrunnlagService,
            beregningService = beregningService,
            fagsakService = fagsakService,
            pdlRestKlient = pdlRestKlient,
            simuleringService = simuleringService,
            autovedtakFinnmarkstilleggBegrunnelseService = autovedtakFinnmarkstilleggBegrunnelseService,
            autovedtakService = mockk(),
            behandlingService = mockk(),
            taskService = mockk(),
        )

    private val fagsak = defaultFagsak()
    private val behandling = lagBehandling(fagsak = fagsak)
    private val søkerIdent = "12345678910"
    private val barnIdent = "12345678911"
    private val persongrunnlag =
        lagTestPersonopplysningGrunnlag(
            behandling.id,
            søkerIdent,
            listOf(barnIdent),
        )

    private val adresse = Vegadresse(null, null, null, null, null, null, null, null)
    private val bostedsadresseUtenforFinnmark =
        Bostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 9, 15), vegadresse = adresse.copy(kommunenummer = "0301"))

    private val bostedsadresseIFinnmark =
        Bostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 9, 15), vegadresse = adresse.copy(kommunenummer = "5601"))

    @BeforeEach
    fun setUp() {
        every { fagsakService.hentPåFagsakId(fagsak.id) } returns lagFagsak(status = LØPENDE, type = NORMAL)
        every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns lagTilkjentYtelse { emptySet() }
        every { simuleringService.hentFeilutbetaling(behandling.id) } returns BigDecimal.ZERO
        every { simuleringService.oppdaterSimuleringPåBehandling(behandling) } returns emptyList()
    }

    @Nested
    inner class SkalAutovedtakBehandles {
        @ParameterizedTest
        @EnumSource(FagsakStatus::class, names = ["LØPENDE"], mode = EXCLUDE)
        fun `skal returnere false når fagsak ikke har løpende barnetrygd`(
            fagsakStatus: FagsakStatus,
        ) {
            // Arrange
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns lagFagsak(status = fagsakStatus)

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @ParameterizedTest
        @EnumSource(FagsakType::class, names = ["SKJERMET_BARN", "INSTITUSJON"], mode = INCLUDE)
        fun `skal returnere false når fagsaktype ikke kan behandles`(
            fagsakType: FagsakType,
        ) {
            // Arrange
            every { fagsakService.hentPåFagsakId(fagsak.id) } returns lagFagsak(status = LØPENDE, type = fagsakType)

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @Test
        fun `skal returnere false når det ikke finnes noen siste iverksatte behandling`() {
            // Arrange
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns null

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @Test
        fun `skal returnere true når forrige behandling hadde andeler med ytelsetype FINNMARKSTILLEGG`() {
            // Arrange
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns
                lagTilkjentYtelse {
                    setOf(
                        lagAndelTilkjentYtelse(
                            fom = YearMonth.of(2025, 10),
                            tom = YearMonth.of(2025, 10),
                            ytelseType = YtelseType.FINNMARKSTILLEGG,
                        ),
                    )
                }

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isTrue()
        }

        @Test
        fun `skal returnere false når ingen av personene bor i tilleggssone`() {
            // Arrange
            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isFalse()
        }

        @Test
        fun `skal returnere true når minst en person bor i tilleggssone`() {
            // Arrange
            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseIFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val skalAutovedtakBehandles = autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))

            // Assert
            assertThat(skalAutovedtakBehandles).isTrue()
        }

        @Test
        fun `skal kaste feil når minst en person bor i tilleggssone, men siste vedtatte behandling er av kategori EØS`() {
            // Arrange
            val behandling = lagBehandling(fagsak = fagsak, behandlingKategori = BehandlingKategori.EØS)
            every { behandlingHentOgPersisterService.hentSisteBehandlingSomErVedtatt(fagsak.id) } returns behandling
            every { beregningService.hentTilkjentYtelseForBehandling(behandling.id) } returns lagTilkjentYtelse { emptySet() }
            every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(listOf(søkerIdent, barnIdent)) } returns
                mapOf(
                    søkerIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseIFinnmark), deltBosted = emptyList()),
                    barnIdent to PdlAdresserPerson(bostedsadresse = listOf(bostedsadresseUtenforFinnmark), deltBosted = emptyList()),
                )

            // Act
            val feil =
                assertThrows<AutovedtakMåBehandlesManueltFeil> {
                    autovedtakFinnmarkstilleggService.skalAutovedtakBehandles(FinnmarkstilleggData(fagsakId = fagsak.id))
                }

            // Assert
            assertThat(feil.message).isEqualTo("Automatisk behandling av finnmarkstillegg kan ikke gjennomføres for EØS-saker.")
        }
    }
}
