package no.nav.familie.ba.sak.internal

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.isSameOrAfter
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagFagsak
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagVilkårsvurdering
import no.nav.familie.ba.sak.config.TaskRepositoryWrapper
import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdService
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiService
import no.nav.familie.ba.sak.kjerne.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.kjerne.autovedtak.AutovedtakService
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.beregning.TilkjentYtelseValideringService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.YearMonth
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
class ForvalterServiceTest {
    @MockK
    lateinit var persongrunnlagService: PersongrunnlagService

    @MockK
    lateinit var økonomiService: ØkonomiService

    @MockK
    lateinit var vedtakService: VedtakService

    @MockK
    lateinit var beregningService: BeregningService

    @MockK
    lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @MockK
    lateinit var stegService: StegService

    @MockK
    lateinit var fagsakService: FagsakService

    @MockK
    lateinit var behandlingService: BehandlingService

    @MockK
    lateinit var taskRepository: TaskRepositoryWrapper

    @MockK
    lateinit var autovedtakService: AutovedtakService

    @MockK
    lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    lateinit var fagsakRepository: FagsakRepository

    @MockK
    lateinit var behandlingRepository: BehandlingRepository

    @MockK
    lateinit var tilkjentYtelseValideringService: TilkjentYtelseValideringService

    @MockK
    lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @MockK
    lateinit var infotrygdService: InfotrygdService

    @MockK
    lateinit var andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository

    @InjectMockKs
    lateinit var forvalterService: ForvalterService

    @Test
    fun `Skal endre periodeFom på vilkårresultat når den er før fødselsdato på person`() {
        val behandling = lagBehandling(status = BehandlingStatus.AVSLUTTET)

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdering.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(), periodeTom = barn.fødselsdato.plusMonths(3), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.plusMonths(4), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, periodeTom = barnFødselsdato.plusMonths(2), vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.plusMonths(3), vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        val vilkårsvurderingEndret = forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        vilkårsvurderingEndret.personResultater
            .singleOrNull { !it.erSøkersResultater() }
            ?.vilkårResultater
            ?.forEach { vilkårResultat ->
                assertTrue(vilkårResultat.periodeFom?.isSameOrAfter(barn.fødselsdato) ?: false)
            }

        val vilkårResultaterMedFomEtterBarnsFødselsDato =
            vilkårsvurderingEndret.personResultater
                .singleOrNull { !it.erSøkersResultater() }
                ?.vilkårResultater
                ?.filter { it.periodeFom?.isAfter(barn.fødselsdato) ?: false }
        assertThat(vilkårResultaterMedFomEtterBarnsFødselsDato?.size)
            .isEqualTo(2)
            .`as`("Vilkårresultater med fom etter barns fødselsdato har også blitt endret: $vilkårResultaterMedFomEtterBarnsFødselsDato")
    }

    @Test
    fun `Skal kaste feil når vi har etterfølgende perioder som begge begynner før fødselsdato`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.AVSLUTTET

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdring.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barnResultat = barnResultat,
                    periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(),
                    periodeTom = barn.fødselsdato.minusDays(5),
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                ),
                lagVilkårResultat(
                    barnResultat = barnResultat,
                    periodeFom = barn.fødselsdato.minusDays(4),
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                ),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        assertThrows<Feil> {
            forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        }
    }

    @Test
    fun `Skal kaste feil når vilkårResultat begynner tidligere måned enn fødselsdato`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.AVSLUTTET

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdring.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.minusMonths(2), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        assertThrows<Feil> {
            forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        }
    }

    @Test
    fun `Skal ikke endre på fom på under-18-vilkår`() {
        val behandling = lagBehandling()
        behandling.status = BehandlingStatus.AVSLUTTET

        val barnFødselsdato = LocalDate.now().minusYears(1).withDayOfMonth(15)
        val søker = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN, fødselsdato = barnFødselsdato)

        val vilkårsvurderingSomSkalFlushes = slot<Vilkårsvurdering>()
        every { vilkårsvurderingService.oppdater(capture(vilkårsvurderingSomSkalFlushes)) } answers {
            if (vilkårsvurderingSomSkalFlushes.isCaptured) {
                vilkårsvurderingSomSkalFlushes.captured
            } else {
                throw Feil("Noe gikk feil ved capturing av vilkårsvurdring.")
            }
        }

        every { persongrunnlagService.hentSøkerOgBarnPåBehandling(any()) } returns
            listOf(
                personTilPersonEnkel(barn),
                personTilPersonEnkel(søker),
            )

        every { behandlingHentOgPersisterService.hent(behandling.id) } returns behandling

        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                søkerAktør = søker.aktør,
                resultat = Resultat.OPPFYLT,
            )
        val barnResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = barn.aktør)
        barnResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(), vilkårType = Vilkår.BOSATT_I_RIKET),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato.førsteDagIInneværendeMåned(), vilkårType = Vilkår.UNDER_18_ÅR),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.BOR_MED_SØKER),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.LOVLIG_OPPHOLD),
                lagVilkårResultat(barnResultat = barnResultat, periodeFom = barn.fødselsdato, vilkårType = Vilkår.GIFT_PARTNERSKAP),
            ),
        )
        vilkårsvurdering.personResultater += barnResultat

        every { vilkårsvurderingService.hentAktivForBehandling(behandling.id) } returns vilkårsvurdering

        val vilkårsvurderingEndret = forvalterService.settFomPåVilkårTilPersonsFødselsdato(behandling.id)
        vilkårsvurderingEndret.personResultater
            .singleOrNull { !it.erSøkersResultater() }
            ?.vilkårResultater
            ?.forEach { vilkårResultat ->
                if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR) {
                    assertThat(vilkårResultat.periodeFom == barn.fødselsdato.førsteDagIInneværendeMåned())
                } else {
                    assertTrue(vilkårResultat.periodeFom?.isSameOrAfter(barn.fødselsdato) ?: false)
                }
            }
    }

    @Nested
    inner class KorrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlinger {
        @Test
        fun `skal splitte andel og oppdatere periodeOffset og forrigePeriodeOffset når andel overlapper med splittmåned`() {
            // Arrange
            val fagsak = lagFagsak()
            val mockedBehandling = mockk<Behandling>()

            every { mockedBehandling.opprettetTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()
            every { mockedBehandling.id } returns 3
            every { mockedBehandling.fagsak } returns fagsak
            every { mockedBehandling.aktivertTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()

            val mockedForrigeBehandling = mockk<Behandling>()
            val mockedForrigeBehandling2 = mockk<Behandling>()

            every { mockedForrigeBehandling.id } returns 2
            every { mockedForrigeBehandling.aktivertTidspunkt } returns LocalDate.of(2024, 8, 1).atStartOfDay()

            every { mockedForrigeBehandling2.id } returns 1
            every { mockedForrigeBehandling2.aktivertTidspunkt } returns LocalDate.of(2022, 8, 1).atStartOfDay()

            every { behandlingRepository.finnOppdaterUtvidetKlassekodeBehandlingerIFagsakerHvorDetKunFinnes1SlikBehandling() } returns listOf(mockedBehandling)
            every {
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(3)
            } returns
                listOf(
                    lagAndelTilkjentYtelse(id = 2, fom = YearMonth.of(2024, 7), tom = YearMonth.of(2035, 5), periodeIdOffset = 3, forrigeperiodeIdOffset = 2, ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                )
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(mockedForrigeBehandling, mockedForrigeBehandling2)

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(2) } returns
                listOf(
                    lagAndelTilkjentYtelse(id = 1, fom = YearMonth.of(2024, 7), tom = YearMonth.of(2035, 5), periodeIdOffset = 2, forrigeperiodeIdOffset = 1, ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                )

            every { andelTilkjentYtelseRepository.save(any()) } answers { firstArg() }

            // Act
            val korrigerteAndelerForBehandlinger = forvalterService.korrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlinger()

            // Assert
            verify(exactly = 2) { andelTilkjentYtelseRepository.save(any()) }
            assertThat(korrigerteAndelerForBehandlinger).hasSize(1)
            val korrigerteAndeler = korrigerteAndelerForBehandlinger.single().second
            assertThat(korrigerteAndeler).hasSize(2)
            val førsteAndel = korrigerteAndeler.minBy { it.stønadFom }
            val sisteAndel = korrigerteAndeler.maxBy { it.stønadFom }
            assertThat(førsteAndel.id).isEqualTo(2)
            assertThat(førsteAndel.stønadFom).isEqualTo(YearMonth.of(2024, 7))
            assertThat(førsteAndel.stønadTom).isEqualTo(YearMonth.of(2024, 12))
            assertThat(førsteAndel.periodeOffset).isEqualTo(2)
            assertThat(førsteAndel.forrigePeriodeOffset).isEqualTo(1)

            assertThat(sisteAndel.id).isEqualTo(0)
            assertThat(sisteAndel.stønadFom).isEqualTo(YearMonth.of(2025, 1))
            assertThat(sisteAndel.stønadTom).isEqualTo(YearMonth.of(2035, 5))
            assertThat(sisteAndel.periodeOffset).isEqualTo(3)
            assertThat(sisteAndel.forrigePeriodeOffset).isEqualTo(2)
        }

        @Test
        fun `skal ikke gjøre noe dersom ingen andel overlapper med splittmåned`() {
            // Arrange
            val fagsak = lagFagsak()
            val mockedBehandling = mockk<Behandling>()

            every { mockedBehandling.opprettetTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()
            every { mockedBehandling.id } returns 3
            every { mockedBehandling.fagsak } returns fagsak
            every { mockedBehandling.aktivertTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()

            every { behandlingRepository.finnOppdaterUtvidetKlassekodeBehandlingerIFagsakerHvorDetKunFinnes1SlikBehandling() } returns listOf(mockedBehandling)
            every {
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(3)
            } returns
                listOf(
                    lagAndelTilkjentYtelse(fom = YearMonth.of(2024, 7), tom = YearMonth.of(2024, 11), ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                    lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 2), tom = YearMonth.of(2035, 5), ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                )

            // Act
            val korrigerteAndelerForBehandlinger = forvalterService.korrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlinger()

            // Assert
            verify(exactly = 0) { andelTilkjentYtelseRepository.save(any()) }
            assertThat(korrigerteAndelerForBehandlinger).hasSize(1)
            val korrigerteAndeler = korrigerteAndelerForBehandlinger.single().second
            assertThat(korrigerteAndeler).isEmpty()
        }

        @Test
        fun `skal kaste feil dersom det finnes behandlinger i uttrekk som ikke ble opprettet 17 desember`() {
            // Arrange
            val fagsak = lagFagsak()
            val mockedBehandling = mockk<Behandling>()

            every { mockedBehandling.opprettetTidspunkt } returns LocalDate.of(2025, 1, 8).atStartOfDay()
            every { mockedBehandling.id } returns 3
            every { mockedBehandling.fagsak } returns fagsak
            every { mockedBehandling.aktivertTidspunkt } returns LocalDate.of(2025, 1, 8).atStartOfDay()

            every { behandlingRepository.finnOppdaterUtvidetKlassekodeBehandlingerIFagsakerHvorDetKunFinnes1SlikBehandling() } returns listOf(mockedBehandling)

            // Act && Assert
            val exception = assertThrows<IllegalStateException> { forvalterService.korrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlinger() }
            assertThat(exception.message).isEqualTo("Alle behandlinger må være opprettet 17.desember. Behandling 3 ble opprettet 2025-01-08")
        }
    }

    @Nested
    inner class KorrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlingerDryRun {
        @Test
        fun `skal splitte andel og oppdatere periodeOffset og forrigePeriodeOffset når andel overlapper med splittmåned`() {
            // Arrange
            val fagsak = lagFagsak()
            val mockedBehandling = mockk<Behandling>()

            every { mockedBehandling.opprettetTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()
            every { mockedBehandling.id } returns 3
            every { mockedBehandling.fagsak } returns fagsak
            every { mockedBehandling.aktivertTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()

            val mockedForrigeBehandling = mockk<Behandling>()
            val mockedForrigeBehandling2 = mockk<Behandling>()

            every { mockedForrigeBehandling.id } returns 2
            every { mockedForrigeBehandling.aktivertTidspunkt } returns LocalDate.of(2024, 8, 1).atStartOfDay()

            every { mockedForrigeBehandling2.id } returns 1
            every { mockedForrigeBehandling2.aktivertTidspunkt } returns LocalDate.of(2022, 8, 1).atStartOfDay()

            every { behandlingRepository.finnOppdaterUtvidetKlassekodeBehandlingerIFagsakerHvorDetKunFinnes1SlikBehandling() } returns listOf(mockedBehandling)
            every {
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(3)
            } returns
                listOf(
                    lagAndelTilkjentYtelse(id = 2, fom = YearMonth.of(2024, 7), tom = YearMonth.of(2035, 5), periodeIdOffset = 3, forrigeperiodeIdOffset = 2, ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                )
            every { behandlingRepository.finnBehandlinger(fagsak.id) } returns listOf(mockedForrigeBehandling, mockedForrigeBehandling2)

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(2) } returns
                listOf(
                    lagAndelTilkjentYtelse(id = 1, fom = YearMonth.of(2024, 7), tom = YearMonth.of(2035, 5), periodeIdOffset = 2, forrigeperiodeIdOffset = 1, ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                )

            // Act
            val korrigerteAndelerForBehandlinger = forvalterService.korrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlingerDryRun()

            // Assert
            assertThat(korrigerteAndelerForBehandlinger).hasSize(1)
            val korrigerteAndeler = korrigerteAndelerForBehandlinger.single().second
            assertThat(korrigerteAndeler).hasSize(2)
            val førsteAndel = korrigerteAndeler.minBy { it.stønadFom }
            val sisteAndel = korrigerteAndeler.maxBy { it.stønadFom }
            assertThat(førsteAndel.id).isEqualTo(2)
            assertThat(førsteAndel.stønadFom).isEqualTo(YearMonth.of(2024, 7))
            assertThat(førsteAndel.stønadTom).isEqualTo(YearMonth.of(2024, 12))
            assertThat(førsteAndel.periodeOffset).isEqualTo(2)
            assertThat(førsteAndel.forrigePeriodeOffset).isEqualTo(1)

            assertThat(sisteAndel.id).isEqualTo(0)
            assertThat(sisteAndel.stønadFom).isEqualTo(YearMonth.of(2025, 1))
            assertThat(sisteAndel.stønadTom).isEqualTo(YearMonth.of(2035, 5))
            assertThat(sisteAndel.periodeOffset).isEqualTo(3)
            assertThat(sisteAndel.forrigePeriodeOffset).isEqualTo(2)
        }

        @Test
        fun `skal ikke gjøre noe dersom ingen andel overlapper med splittmåned`() {
            // Arrange
            val fagsak = lagFagsak()
            val mockedBehandling = mockk<Behandling>()

            every { mockedBehandling.opprettetTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()
            every { mockedBehandling.id } returns 3
            every { mockedBehandling.fagsak } returns fagsak
            every { mockedBehandling.aktivertTidspunkt } returns LocalDate.of(2024, 12, 17).atStartOfDay()

            every { behandlingRepository.finnOppdaterUtvidetKlassekodeBehandlingerIFagsakerHvorDetKunFinnes1SlikBehandling() } returns listOf(mockedBehandling)
            every {
                andelTilkjentYtelseRepository
                    .finnAndelerTilkjentYtelseForBehandling(3)
            } returns
                listOf(
                    lagAndelTilkjentYtelse(fom = YearMonth.of(2024, 7), tom = YearMonth.of(2024, 11), ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                    lagAndelTilkjentYtelse(fom = YearMonth.of(2025, 2), tom = YearMonth.of(2035, 5), ytelseType = YtelseType.UTVIDET_BARNETRYGD),
                )

            // Act
            val korrigerteAndelerForBehandlinger = forvalterService.korrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlingerDryRun()

            // Assert
            verify(exactly = 0) { andelTilkjentYtelseRepository.save(any()) }
            assertThat(korrigerteAndelerForBehandlinger).hasSize(1)
            val korrigerteAndeler = korrigerteAndelerForBehandlinger.single().second
            assertThat(korrigerteAndeler).isEmpty()
        }

        @Test
        fun `skal kaste feil dersom det finnes behandlinger i uttrekk som ikke ble opprettet 17 desember`() {
            // Arrange
            val fagsak = lagFagsak()
            val mockedBehandling = mockk<Behandling>()

            every { mockedBehandling.opprettetTidspunkt } returns LocalDate.of(2025, 1, 8).atStartOfDay()
            every { mockedBehandling.id } returns 3
            every { mockedBehandling.fagsak } returns fagsak
            every { mockedBehandling.aktivertTidspunkt } returns LocalDate.of(2025, 1, 8).atStartOfDay()

            every { behandlingRepository.finnOppdaterUtvidetKlassekodeBehandlingerIFagsakerHvorDetKunFinnes1SlikBehandling() } returns listOf(mockedBehandling)

            // Act && Assert
            val exception = assertThrows<IllegalStateException> { forvalterService.korrigerUtvidetAndelerIOppdaterUtvidetKlassekodeBehandlingerDryRun() }
            assertThat(exception.message).isEqualTo("Alle behandlinger må være opprettet 17.desember. Behandling 3 ble opprettet 2025-01-08")
        }
    }

    private fun personTilPersonEnkel(barn: Person) =
        PersonEnkel(
            type = PersonType.BARN,
            aktør = barn.aktør,
            fødselsdato = barn.fødselsdato,
            dødsfallDato = null,
            målform = Målform.NB,
        )

    private fun lagVilkårResultat(
        vilkårType: Vilkår,
        barnResultat: PersonResultat,
        periodeFom: LocalDate,
        periodeTom: LocalDate? =
            if (vilkårType == Vilkår.UNDER_18_ÅR) {
                periodeFom.plusYears(18).minusMonths(1)
            } else {
                null
            },
    ): VilkårResultat =
        VilkårResultat(
            personResultat = barnResultat,
            vilkårType = vilkårType,
            resultat = Resultat.OPPFYLT,
            periodeFom = periodeFom,
            periodeTom = periodeTom,
            begrunnelse = "",
            sistEndretIBehandlingId = Random.nextLong(),
        )
}
