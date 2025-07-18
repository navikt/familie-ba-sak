﻿package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.familie.ba.sak.TestClockProvider
import no.nav.familie.ba.sak.common.toLocalDate
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.featureToggle.UnleashNextMedContextService
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringMottaker
import no.nav.familie.ba.sak.datagenerator.lagØkonomiSimuleringPostering
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.eøs.differanseberegning.TilpassDifferanseberegningEtterValutakursService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.simulering.SimuleringService
import no.nav.familie.ba.sak.kjerne.tidslinje.util.feb
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.sep
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import no.nav.familie.kontrakter.felles.simulering.FagOmrådeKode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutomatiskOppdaterValutakursServiceTest {
    val dagensDato = LocalDate.of(2020, 9, 15)
    val clockProvider = TestClockProvider()

    val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs> = mockPeriodeBarnSkjemaRepository()
    val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp> = mockPeriodeBarnSkjemaRepository()
    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(valutakursRepository = valutakursRepository, utenlandskPeriodebeløpRepository, emptyList(), clockProvider)
    val tilpassDifferanseberegningEtterValutakursService = mockk<TilpassDifferanseberegningEtterValutakursService>()

    val valutakursService = ValutakursService(valutakursRepository, emptyList())
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val ecbService = mockk<ECBService>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()
    val simuleringService = mockk<SimuleringService>()
    val vurderingsstrategiForValutakurserRepository = mockk<VurderingsstrategiForValutakurserRepository>()
    val unleashNextMedContextService = mockk<UnleashNextMedContextService>()

    val automatiskOppdaterValutakursService =
        AutomatiskOppdaterValutakursService(
            valutakursService = valutakursService,
            vedtaksperiodeService = vedtaksperiodeService,
            clockProvider = TestClockProvider.lagClockProviderMedFastTidspunkt(dagensDato),
            ecbService = ecbService,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilpassValutakurserTilUtenlandskePeriodebeløpService = tilpassValutakurserTilUtenlandskePeriodebeløpService,
            simuleringService = simuleringService,
            vurderingsstrategiForValutakurserRepository = vurderingsstrategiForValutakurserRepository,
            unleashNextMedContextService = unleashNextMedContextService,
            tilpassDifferanseberegningEtterValutakursService = tilpassDifferanseberegningEtterValutakursService,
        )

    val forrigeBehandlingId = BehandlingId(9L)
    val behandlingId = BehandlingId(10L)

    val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).toLocalDate())
    val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).toLocalDate())
    val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).toLocalDate())

    @BeforeEach
    fun beforeEach() {
        every { simuleringService.oppdaterSimuleringPåBehandlingVedBehov(any()) } returns emptyList()
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers { lagBehandling(id = forrigeBehandlingId.id) }
        every { behandlingHentOgPersisterService.hent(any()) } answers {
            lagBehandling(id = firstArg())
        }
        every { ecbService.hentValutakurs(any(), any()) } answers {
            val dato = secondArg<LocalDate>()
            dato.month.value.toBigDecimal()
        }
        every { vurderingsstrategiForValutakurserRepository.findByBehandlingId(any()) } returns null
        valutakursRepository.deleteAll()
        utenlandskPeriodebeløpRepository.deleteAll()
        justRun { tilpassDifferanseberegningEtterValutakursService.skjemaerEndret(any(), any()) }
    }

    @Test
    fun `oppdaterValutakurserEtterEndringstidspunkt skal automatisk hente valutakurser hver måned etter endringstidspunktet`() {
        UtenlandskPeriodebeløpBuilder(jan(2023), behandlingId)
            .medBeløp("777777777", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(jan(2023), behandlingId)
            .medKurs("111111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2023, 5, 15)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(behandlingId)

        val forventetUberørteValutakurser =
            ValutakursBuilder(jan(2023), behandlingId)
                .medKurs("1111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)
                .bygg()

        val forventetOppdaterteValutakurser =
            ValutakursBuilder(jan(2023), behandlingId)
                .medKurs("    45678", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(forventetUberørteValutakurser + forventetOppdaterteValutakurser)
    }

    @Test
    fun `resettValutakurserOgLagValutakurserEtterEndringstidspunkt skal resette valutakursene før endringstidspunktet til forrige behandling`() {
        every { behandlingHentOgPersisterService.hent(any()) } answers { lagBehandling(id = firstArg()) }
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers { lagBehandling(id = forrigeBehandlingId.id) }

        UtenlandskPeriodebeløpBuilder(jan(2023), forrigeBehandlingId)
            .medBeløp("77777777", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        UtenlandskPeriodebeløpBuilder(jan(2023), behandlingId)
            .medBeløp("77778888", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(jan(2023), forrigeBehandlingId)
            .medKurs("11111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        ValutakursBuilder(jan(2023), behandlingId)
            .medKurs("01234567", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.AUTOMATISK)
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2023, 5, 15)

        automatiskOppdaterValutakursService.resettValutakurserOgLagValutakurserEtterEndringstidspunkt(behandlingId)

        val forventetUberørteValutakurser =
            ValutakursBuilder(jan(2023), behandlingId)
                .medKurs("1111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)
                .bygg()

        val forventetOppdaterteValutakurser =
            ValutakursBuilder(jan(2023), behandlingId)
                .medKurs("    4567", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(forventetUberørteValutakurser + forventetOppdaterteValutakurser)
    }

    @Test
    fun `oppdaterValutakurserEtterEndringstidspunkt skal ikke oppdatere valutakurser før praksisendringsdatoen juni 2024 for revurdering`() {
        every { behandlingHentOgPersisterService.hent(any()) } answers {
            lagBehandling(
                id = firstArg(),
                behandlingType = BehandlingType.REVURDERING,
            )
        }
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers { lagBehandling(id = forrigeBehandlingId.id) }
        every { ecbService.hentValutakurs(any(), any()) } answers {
            val dato = secondArg<LocalDate>()
            (dato.month.value % 10).toBigDecimal()
        }

        UtenlandskPeriodebeløpBuilder(feb(2024), behandlingId)
            .medBeløp("77778888", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(feb(2024), behandlingId)
            .medKurs("11111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2022, 5, 15)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(behandlingId)

        val forventetUberørteValutakurser =
            ValutakursBuilder(feb(2024), behandlingId)
                .medKurs("1111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)
                .bygg()

        val forventetOppdaterteValutakurser =
            ValutakursBuilder(feb(2024), behandlingId)
                .medKurs("    5678", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(forventetUberørteValutakurser + forventetOppdaterteValutakurser)
    }

    @Test
    fun `oppdaterValutakurserEtterEndringstidspunkt skal skal kun lage automatiske valutakurser etter siste manuelle postering`() {
        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2023, 2, 15)
        val tomDatoSisteManuellePostering = LocalDate.of(2023, 4, 30)
        every { simuleringService.oppdaterSimuleringPåBehandlingVedBehov(any()) } returns
            listOf(
                lagØkonomiSimuleringMottaker(
                    økonomiSimuleringPostering =
                        listOf(
                            lagØkonomiSimuleringPostering(
                                fagOmrådeKode = FagOmrådeKode.BARNETRYGD_INFOTRYGD_MANUELT,
                                fom = LocalDate.of(2023, 4, 1),
                                tom = tomDatoSisteManuellePostering,
                            ),
                        ),
                ),
            )

        UtenlandskPeriodebeløpBuilder(jan(2023), behandlingId)
            .medBeløp("777777777", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(jan(2023), behandlingId)
            .medKurs("111111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(behandlingId)

        val forventetUberørteValutakurser =
            ValutakursBuilder(jan(2023), behandlingId)
                .medKurs("1111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)
                .bygg()

        val forventetOppdaterteValutakurser =
            ValutakursBuilder(jan(2023), behandlingId)
                .medKurs("    45678", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        val faktiskeValutakurser = valutakursService.hentValutakurser(behandlingId)
        val førsteAutomatiskeValutakurs = faktiskeValutakurser.first { it.vurderingsform == Vurderingsform.AUTOMATISK }

        assertThat(førsteAutomatiskeValutakurs.fom).isEqualTo(tomDatoSisteManuellePostering.plusMonths(1).toYearMonth())

        assertThat(faktiskeValutakurser)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(forventetUberørteValutakurser + forventetOppdaterteValutakurser)
    }

    @Test
    fun `oppdaterValutakurserEtterEndringstidspunkt skal ikke automatisk hente valutakurser om vurderingsstrategien er satt til manuell`() {
        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("777777777", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        val manuelleValutakurserTidslinje =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("111111111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)

        manuelleValutakurserTidslinje
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2020, 5, 15)
        every { vurderingsstrategiForValutakurserRepository.findByBehandlingId(any()) } returns VurderingsstrategiForValutakurserDB(behandlingId = behandlingId.id, vurderingsstrategiForValutakurser = VurderingsstrategiForValutakurser.MANUELL)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(behandlingId)

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(manuelleValutakurserTidslinje.bygg())
    }

    @Test
    fun `oppdaterValutakurserEtterEndringstidspunkt skal kunne oppdatere valutakurser før praksisendringsdatoen januar 2023 for førstegangsbehandlinger`() {
        every { behandlingHentOgPersisterService.hent(any()) } answers {
            lagBehandling(
                id = firstArg(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
            )
        }
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers { lagBehandling(id = forrigeBehandlingId.id) }
        every { ecbService.hentValutakurs(any(), any()) } answers {
            val dato = secondArg<LocalDate>()
            (dato.month.value % 10).toBigDecimal()
        }

        UtenlandskPeriodebeløpBuilder(sep(2022), behandlingId)
            .medBeløp("77778888", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(sep(2022), behandlingId)
            .medKurs("11111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2022, 5, 15)

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringstidspunkt(behandlingId)

        val forventetOppdaterteValutakurser =
            ValutakursBuilder(sep(2022), behandlingId)
                .medKurs("89012123", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(forventetOppdaterteValutakurser)
    }
}
