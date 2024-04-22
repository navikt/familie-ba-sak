package no.nav.familie.ba.sak.kjerne.eøs.valutakurs

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.MockedDateProvider
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.integrasjoner.ecb.ECBService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.endringsabonnement.TilpassValutakurserTilUtenlandskePeriodebeløpService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.eøs.utenlandskperiodebeløp.UtenlandskPeriodebeløp
import no.nav.familie.ba.sak.kjerne.eøs.util.UtenlandskPeriodebeløpBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.ValutakursBuilder
import no.nav.familie.ba.sak.kjerne.eøs.util.mockPeriodeBarnSkjemaRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.tidspunkt.tilLocalDate
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.jupiter.api.BeforeAll
import java.time.LocalDate

class AutomatiskOppdaterValutakursServiceTest {
    val dagensDato = LocalDate.of(2020, 9, 15)

    val valutakursRepository: PeriodeOgBarnSkjemaRepository<Valutakurs> = mockPeriodeBarnSkjemaRepository()
    val utenlandskPeriodebeløpRepository: PeriodeOgBarnSkjemaRepository<UtenlandskPeriodebeløp> = mockPeriodeBarnSkjemaRepository()
    val tilpassValutakurserTilUtenlandskePeriodebeløpService = TilpassValutakurserTilUtenlandskePeriodebeløpService(valutakursRepository = valutakursRepository, utenlandskPeriodebeløpRepository, emptyList())

    val valutakursService = ValutakursService(valutakursRepository, emptyList())
    val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    val ecbService = mockk<ECBService>()
    val behandlingHentOgPersisterService = mockk<BehandlingHentOgPersisterService>()

    val automatiskOppdaterValutakursService =
        AutomatiskOppdaterValutakursService(
            valutakursService = valutakursService,
            vedtaksperiodeService = vedtaksperiodeService,
            localDateProvider = MockedDateProvider(dagensDato),
            ecbService = ecbService,
            utenlandskPeriodebeløpRepository = utenlandskPeriodebeløpRepository,
            behandlingHentOgPersisterService = behandlingHentOgPersisterService,
            tilpassValutakurserTilUtenlandskePeriodebeløpService = tilpassValutakurserTilUtenlandskePeriodebeløpService,
        )

    val forrigeBehandlingId = BehandlingId(9L)
    val behandlingId = BehandlingId(10L)

    val barn1 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
    val barn2 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())
    val barn3 = tilfeldigPerson(personType = PersonType.BARN, fødselsdato = jan(2020).tilLocalDate())

    @BeforeAll
    fun init() {
    }

    @Test
    fun `oppdaterValutakurserEtterEndringsmåned skal automatisk hente valutakurser hver måned etter endringstidspunktet`() {
        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("777777777", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("111111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandling(behandlingId.id) } returns LocalDate.of(2020, 5, 15)
        every { ecbService.hentValutakurs(any(), any()) } answers {
            val dato = secondArg<LocalDate>()
            dato.month.value.toBigDecimal()
        }

        automatiskOppdaterValutakursService.oppdaterValutakurserEtterEndringsmåned(behandlingId)

        val uberørteValutakurser =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("1111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)
                .bygg()

        val oppdaterteValutakurser =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("    45678", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(uberørteValutakurser + oppdaterteValutakurser)
    }

    @Test
    fun `resettValutakurserOgLagValutakurserEtterEndringsmåned skal resette valutakursene før endringstidspunktet til forrige behandling`() {
        every { behandlingHentOgPersisterService.hent(any()) } answers { lagBehandling(id = firstArg()) }
        every { behandlingHentOgPersisterService.hentForrigeBehandlingSomErVedtatt(any()) } answers { lagBehandling(id = forrigeBehandlingId.id) }

        UtenlandskPeriodebeløpBuilder(jan(2020), forrigeBehandlingId)
            .medBeløp("77777777", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        UtenlandskPeriodebeløpBuilder(jan(2020), behandlingId)
            .medBeløp("77778888", "EUR", "N", barn1, barn2, barn3)
            .lagreTil(utenlandskPeriodebeløpRepository)

        ValutakursBuilder(jan(2020), forrigeBehandlingId)
            .medKurs("11111111", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.MANUELL)
            .lagreTil(valutakursRepository)

        ValutakursBuilder(jan(2020), behandlingId)
            .medKurs("01234567", "EUR", barn1, barn2, barn3)
            .medVurderingsform(Vurderingsform.AUTOMATISK)
            .lagreTil(valutakursRepository)

        every { vedtaksperiodeService.finnEndringstidspunktForBehandlingFørValutakurser(behandlingId.id) } returns LocalDate.of(2020, 5, 15)
        every { ecbService.hentValutakurs(any(), any()) } answers {
            val dato = secondArg<LocalDate>()
            dato.month.value.toBigDecimal()
        }

        automatiskOppdaterValutakursService.resettValutakurserOgLagValutakurserEtterEndringsmåned(behandlingId)

        val uberørteValutakurser =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("1111", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.MANUELL)
                .bygg()

        val oppdaterteValutakurser =
            ValutakursBuilder(jan(2020), behandlingId)
                .medKurs("    4567", "EUR", barn1, barn2, barn3)
                .medVurderingsform(Vurderingsform.AUTOMATISK)
                .bygg()

        assertThat(valutakursService.hentValutakurser(behandlingId))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .ignoringFields("valutakursdato")
            .ignoringFields("endretTidspunkt")
            .ignoringFields("opprettetTidspunkt")
            .isEqualTo(uberørteValutakurser + oppdaterteValutakurser)
    }
}
