package no.nav.familie.ba.sak.kjerne.behandlingsresultat

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.BehandlingUnderkategoriDTO
import no.nav.familie.ba.sak.ekstern.restDomene.SøknadDTO
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseMedEndreteUtbetalinger
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelerTilkjentYtelseOgEndreteUtbetalingerService
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Month
import java.time.YearMonth
import org.hamcrest.CoreMatchers.`is` as Is

@ExtendWith(MockKExtension::class)
internal class BehandlingsresultatServiceTest {

    @MockK
    private lateinit var behandlingHentOgPersisterService: BehandlingHentOgPersisterService

    @MockK
    private lateinit var søknadGrunnlagService: SøknadGrunnlagService

    @MockK
    private lateinit var personidentService: PersonidentService

    @MockK
    private lateinit var persongrunnlagService: PersongrunnlagService

    @MockK
    private lateinit var vilkårsvurderingService: VilkårsvurderingService

    @MockK
    private lateinit var andelerTilkjentYtelseOgEndreteUtbetalingerService: AndelerTilkjentYtelseOgEndreteUtbetalingerService

    @MockK
    private lateinit var featureToggleService: FeatureToggleService

    @InjectMockKs
    private lateinit var behandlingsresultatService: BehandlingsresultatService

    @Test
    fun `endra fom eller tom for utvida barnetrygd gir behandlingsresultat endret`() {
        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE, KravOpprinnelse.TIDLIGERE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.APRIL)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.TIDLIGERE),
            setOf(),
            YearMonth.of(2037, Month.MAY)
        )
        val andelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2022, Month.APRIL),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )
        val forrigeAndelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )
        val behandlingsresultat = behandlingsresultatService.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(andelMedEndring),
            forrigeAndelerMedEndringer = listOf(forrigeAndelMedEndring),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat, Is(Behandlingsresultat.ENDRET_UTBETALING))
    }

    @Test
    fun `samme fom og tom for utvida barnetrygd gir behandlingsresultat fortsatt innvilget`() {
        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE, KravOpprinnelse.TIDLIGERE),
            setOf(),
            YearMonth.of(2037, Month.MAY)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.TIDLIGERE),
            setOf(),
            YearMonth.of(2037, Month.MAY)
        )
        val andelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )
        val forrigeAndelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )

        val behandlingsresultat = behandlingsresultatService.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(andelMedEndring),
            forrigeAndelerMedEndringer = listOf(forrigeAndelMedEndring),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat, Is(Behandlingsresultat.FORTSATT_INNVILGET))
    }

    @Test
    fun `utvida barnetrygd nå, men ingenting før gir behandlingsresultat innvilget`() {
        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.INNVILGET),
            YearMonth.of(2022, Month.APRIL)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.INNVILGET),
            YearMonth.of(2037, Month.MAY)
        )
        val andelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )

        val behandlingsresultat = behandlingsresultatService.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(andelMedEndring),
            forrigeAndelerMedEndringer = listOf(),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat, Is(Behandlingsresultat.INNVILGET))
    }

    @Test
    fun `utvida barnetrygd før, men alt opphørt nå gir behandlingsresultat innvilget`() {
        val søkerAktør = Aktør("1234567890123")
        val ytelsePersonSøker = YtelsePerson(
            søkerAktør,
            YtelseType.UTVIDET_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.OCTOBER)
        )
        val ytelsePersonBarn = YtelsePerson(
            Aktør("1234567890124"),
            YtelseType.ORDINÆR_BARNETRYGD,
            listOf(KravOpprinnelse.INNEVÆRENDE),
            setOf(YtelsePersonResultat.OPPHØRT),
            YearMonth.of(2022, Month.OCTOBER)
        )
        val forrigeAndelMedEndring = AndelTilkjentYtelseMedEndreteUtbetalinger(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2021, Month.DECEMBER),
                tom = YearMonth.of(2037, Month.MAY),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søkerAktør,
                beløp = 1054,
                prosent = BigDecimal(50)
            )
        )

        val behandlingsresultat = behandlingsresultatService.utledBehandlingsresultat(
            ytelsePersonerMedResultat = listOf(ytelsePersonSøker, ytelsePersonBarn),
            andelerMedEndringer = listOf(),
            forrigeAndelerMedEndringer = listOf(forrigeAndelMedEndring),
            behandling = lagBehandling()
        )
        assertThat(behandlingsresultat, Is(Behandlingsresultat.OPPHØRT))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere tom liste dersom behandlingen ikke er søknad, fødselshendelse eller manuell migrering`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.DØDSFALL_BRUKER)

        val personerFramstiltForKrav = behandlingsresultatService.finnPersonerFremstiltKravFor(
            behandling = behandling,
            søknadDTO = null,
            forrigeBehandling = null
        )

        assertThat(personerFramstiltForKrav, Is(emptyList()))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere aktør som person framstilt krav for dersom det er søkt for utvidet barnetrygd`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val søknadDto = SøknadDTO(
            underkategori = BehandlingUnderkategoriDTO.UTVIDET,
            barnaMedOpplysninger = emptyList(),
            søkerMedOpplysninger = mockk(),
            endringAvOpplysningerBegrunnelse = ""
        )

        every { vilkårsvurderingService.hentAktivForBehandlingThrows(any()) } returns Vilkårsvurdering(behandling = behandling)

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDTO = søknadDto,
                forrigeBehandling = null
            )

        assertThat(personerFramstiltForKrav.single(), Is(behandling.fagsak.aktør))
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere barn som er folkeregistret og krysset av på søknad`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val barn1Fnr = randomFnr()
        val mocketAktør = mockk<Aktør>()

        val barnSomErKryssetAvFor = BarnMedOpplysninger(
            ident = barn1Fnr,
            navn = "barn1",
            inkludertISøknaden = true,
            erFolkeregistrert = true
        )

        val barnSomIkkeErKryssetAvFor = BarnMedOpplysninger(
            ident = randomFnr(),
            navn = "barn2",
            inkludertISøknaden = false,
            erFolkeregistrert = true
        )

        val barnSomErKryssetAvForMenIkkeFolkeregistrert = BarnMedOpplysninger(
            ident = randomFnr(),
            navn = "barn3",
            inkludertISøknaden = false,
            erFolkeregistrert = true
        )

        val søknadDto = SøknadDTO(
            underkategori = BehandlingUnderkategoriDTO.ORDINÆR,
            barnaMedOpplysninger = listOf(
                barnSomErKryssetAvFor,
                barnSomIkkeErKryssetAvFor,
                barnSomErKryssetAvForMenIkkeFolkeregistrert
            ),
            søkerMedOpplysninger = mockk(),
            endringAvOpplysningerBegrunnelse = ""
        )

        every { personidentService.hentAktør(barn1Fnr) } returns mocketAktør

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDTO = søknadDto,
                forrigeBehandling = null
            )

        assertThat(personerFramstiltForKrav.single(), Is(mocketAktør))

        verify(exactly = 1) { personidentService.hentAktør(barn1Fnr) }
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere nye barn dersom behandlingen har fødselshendelse som årsak`() {
        val forrigeBehandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val behandling = lagBehandling(årsak = BehandlingÅrsak.FØDSELSHENDELSE)
        val nyttBarn = lagPerson()

        every { persongrunnlagService.finnNyeBarn(behandling, forrigeBehandling) } returns listOf(nyttBarn)

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDTO = null,
                forrigeBehandling = forrigeBehandling
            )

        assertThat(personerFramstiltForKrav.single(), Is(nyttBarn.aktør))

        verify(exactly = 1) { persongrunnlagService.finnNyeBarn(behandling, forrigeBehandling) }
    }

    @Test
    fun `finnPersonerFremstiltKravFor skal returnere eksisterende personer fra persongrunnlaget dersom behandlingen er en manuell migrering`() {
        val behandling = lagBehandling(årsak = BehandlingÅrsak.HELMANUELL_MIGRERING, behandlingType = BehandlingType.MIGRERING_FRA_INFOTRYGD)
        val eksisterendeBarn = lagPerson()
        val eksisterendePersonpplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandling.id, personer = mutableSetOf(eksisterendeBarn))

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns eksisterendePersonpplysningGrunnlag

        val personerFramstiltForKrav =
            behandlingsresultatService.finnPersonerFremstiltKravFor(
                behandling = behandling,
                søknadDTO = null,
                forrigeBehandling = null
            )

        assertThat(personerFramstiltForKrav.single(), Is(eksisterendeBarn.aktør))

        verify(exactly = 1) { persongrunnlagService.hentAktivThrows(behandling.id) }
    }
}
