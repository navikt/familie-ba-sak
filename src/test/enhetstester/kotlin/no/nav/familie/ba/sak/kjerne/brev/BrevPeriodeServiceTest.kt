package no.nav.familie.ba.sak.kjerne.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.Vedtaksperiodetype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

internal class BrevPeriodeServiceTest {

    @Test
    fun `finner person som har fått redusert sats nå, men ikke person med reduksjon fra tidligere`() {
        val alleredeRedusertPerson = lagPerson()
        val nåRedusertPerson = lagPerson()

        val service = settOppService(alleredeRedusertPerson, nåRedusertPerson)
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(LocalDate.of(2020, Month.JANUARY, 5))

        val identer = service.hentBarnsPersonIdentMedRedusertPeriode(
            vedtaksperiodeMedBegrunnelser,
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2015, 1),
                    tom = YearMonth.of(2030, 1),
                    person = alleredeRedusertPerson,
                    beløp = 500
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2020, 1),
                    tom = YearMonth.of(2030, 1),
                    person = nåRedusertPerson,
                    beløp = 400
                )
            )
        )

        assertEqualsUnordered(listOf(nåRedusertPerson.aktør.aktivFødselsnummer()), identer)
    }

    @Test
    fun `finner ikke person som ikke har endring fra fom-måneden`() {
        val nåRedusertPerson = lagPerson()

        val service = settOppService(lagPerson(), nåRedusertPerson)
        val vedtaksperiodeMedBegrunnelser = lagVedtaksperiodeMedBegrunnelser(LocalDate.of(2020, Month.JANUARY, 5))

        val identer = service.hentBarnsPersonIdentMedRedusertPeriode(
            vedtaksperiodeMedBegrunnelser,
            listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2021, 1),
                    tom = YearMonth.of(2030, 1),
                    person = nåRedusertPerson,
                    beløp = 1600
                )
            )
        )

        assertThat(identer).isEmpty()
    }

    private fun lagVedtaksperiodeMedBegrunnelser(fom: LocalDate) = VedtaksperiodeMedBegrunnelser(
        type = Vedtaksperiodetype.UTBETALING,
        vedtak = Vedtak(
            behandling = Behandling(
                fagsak = defaultFagsak(),
                kategori = BehandlingKategori.NASJONAL,
                opprettetÅrsak = BehandlingÅrsak.SØKNAD,
                type = BehandlingType.REVURDERING,
                underkategori = BehandlingUnderkategori.ORDINÆR
            )
        ),
        fom = fom
    )

    private fun settOppService(alleredeRedusertPerson: Person, nåRedusertPerson: Person) = BrevPeriodeService(
        mockk<BehandlingHentOgPersisterService>().also {
            every { it.hentForrigeBehandlingSomErIverksatt(any()) } returns lagBehandling(
                behandlingKategori = BehandlingKategori.NASJONAL,
                årsak = BehandlingÅrsak.SØKNAD,
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                underkategori = BehandlingUnderkategori.ORDINÆR
            )
        },
        mockk(),
        mockk<AndelTilkjentYtelseRepository>().also {
            every { it.finnAndelerTilkjentYtelseForBehandling(any()) } returns listOf(
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2010, 1),
                    tom = YearMonth.of(2014, 12),
                    person = alleredeRedusertPerson
                ),
                lagAndelTilkjentYtelse(
                    fom = YearMonth.of(2010, 1),
                    tom = YearMonth.of(2019, 12),
                    person = nåRedusertPerson
                )
            )
        },
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk<PersonidentService>().also { every { it.hentAktør(nåRedusertPerson.aktør.aktørId) } returns nåRedusertPerson.aktør },
        mockk(),
        mockk(),
        mockk()
    )
}
