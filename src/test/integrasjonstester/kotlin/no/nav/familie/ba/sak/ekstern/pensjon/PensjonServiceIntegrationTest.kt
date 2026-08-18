package no.nav.familie.ba.sak.ekstern.pensjon

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.årMnd
import no.nav.familie.ba.sak.fake.FakeEnvService
import no.nav.familie.ba.sak.fake.FakeInfotrygdBarnetrygdKlient
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

class PensjonServiceIntegrationTest(
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val pensjonService: PensjonService,
    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    @Autowired
    private val fakeInfotrygdBarnetrygdKlient: FakeInfotrygdBarnetrygdKlient,
    @Autowired
    private val envService: FakeEnvService,
    @Autowired
    private val personopplysningGrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired
    private val vilkårsvurderingService: VilkårsvurderingService,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun setUp() {
        // FakeEnvService er en singleton med preprod=true som default. Uten dette avhenger testene av
        // rekkefølgen, og hentBarnetrygd ville tatt testdata-grenen med tilfeldige uttrekk fra infotrygd_baq.
        envService.setErPreprod(false)
    }

    @AfterEach
    fun tearDown() {
        // FakeEnvService er en delt singleton, så defaultverdien må settes tilbake for andre testklasser
        envService.setErPreprod(true)
    }

    @Test
    fun `skal finne en relaterte fagsaker per barn`() {
        // Arrange
        val søker = tilfeldigPerson()
        val annenForelder = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør)

        val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(annenForelder.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak2, barn1, barnAktør)

        // Act
        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))

        // Assert
        assertThat(barnetrygdTilPensjon).hasSize(2)
    }

    @Test
    fun `skal inkludere periode fra Infotrygd sammen med perioden fra BA-sak på den samme identen`() {
        // Arrange
        val søker = tilfeldigPerson()
        val annenForelder = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør)

        val fagsak2 = fagsakService.hentEllerOpprettFagsakForPersonIdent(annenForelder.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak2, barn1, barnAktør)

        mockInfotrygdBarnetrygdResponse(søkerAktør)

        // Act
        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))

        // Assert
        assertThat(barnetrygdTilPensjon).hasSize(2)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.any { it.kildesystem == "Infotrygd" } }).hasSize(1)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.all { it.kildesystem == "Infotrygd" } }).hasSize(0)
    }

    @Test
    fun `skal fjerne overlapp ved å kutte perioden fra Infotrygd til før perioden for den samme personen starter i BA-sak`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(
            fagsak,
            barn1,
            barnAktør,
            fom = årMnd("2021-04"),
            tom = årMnd("2023-11"),
        )
        val infotrygdStønadFom = årMnd("2019-03")
        val infotrygdStønadTom = årMnd("2022-09")

        mockInfotrygdBarnetrygdResponse(
            søkerAktør,
            barnAktør,
            stønadFom = infotrygdStønadFom,
            stønadTom = infotrygdStønadTom,
        )

        // Act
        val (basakPeriode, infotrygdperiode) =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder
                .partition { it.kildesystem == "BA" }
                .run { first.single() to second.single() }

        // Assert
        assertThat(infotrygdperiode.stønadFom).isEqualTo(infotrygdStønadFom)
        assertThat(infotrygdperiode.stønadTom).isEqualTo(basakPeriode.stønadFom.minusMonths(1))
    }

    @Test
    fun `skal finne og returnere perioder fra Infotrygd som har infotrygd sin definisjon på uendelighet`() {
        // Arrange
        val søker = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)

        mockInfotrygdBarnetrygdResponse(søker = søkerAktør, stønadFom = YearMonth.now(), stønadTom = YearMonth.of(999999999, 12))

        // Act
        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))

        // Assert
        assertThat(barnetrygdTilPensjon).hasSize(1)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.all { it.kildesystem == "Infotrygd" } }).hasSize(1)
    }

    @Test
    fun `skal finne og returnere perioder fra Infotrygd`() {
        // Arrange
        val søker = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)

        mockInfotrygdBarnetrygdResponse(søkerAktør)

        // Act
        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))

        // Assert
        assertThat(barnetrygdTilPensjon).hasSize(1)
        assertThat(barnetrygdTilPensjon.filter { it.barnetrygdPerioder.all { it.kildesystem == "Infotrygd" } }).hasSize(1)
    }

    @Test
    fun `skal sette søkerHarSelvstendigRett til false når annen forelder ikke er omfattet av norsk lovgivning`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør)

        // Act
        val barnetrygdPeriode =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder
                .single()

        // Assert
        assertThat(barnetrygdPeriode.stønadFom).isEqualTo(årMnd("2019-04"))
        assertThat(barnetrygdPeriode.stønadTom).isEqualTo(årMnd("2023-03"))
        assertThat(barnetrygdPeriode.søkerHarSelvstendigRett).isFalse()
    }

    @Test
    fun `skal sette søkerHarSelvstendigRett til true når annen forelder er omfattet av norsk lovgivning i hele perioden`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør, selvstendigRettFom = årMnd("2019-04"))

        // Act
        val barnetrygdPeriode =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder
                .single()

        // Assert
        assertThat(barnetrygdPeriode.stønadFom).isEqualTo(årMnd("2019-04"))
        assertThat(barnetrygdPeriode.stønadTom).isEqualTo(årMnd("2023-03"))
        assertThat(barnetrygdPeriode.søkerHarSelvstendigRett).isTrue()
    }

    @Test
    fun `skal splitte barnetrygdperioden når annen forelder kun er omfattet av norsk lovgivning i deler av perioden`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør, selvstendigRettFom = årMnd("2022-01"))

        // Act
        // fraDato midt i andelen: hele andelen var løpende på fraDato og skal returneres i sin helhet, også delen før fraDato
        val barnetrygdPerioder =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder
                .sortedBy { it.stønadFom }

        // Assert
        assertThat(barnetrygdPerioder).hasSize(2)
        assertThat(barnetrygdPerioder[0].stønadFom).isEqualTo(årMnd("2019-04"))
        assertThat(barnetrygdPerioder[0].stønadTom).isEqualTo(årMnd("2021-12"))
        assertThat(barnetrygdPerioder[0].søkerHarSelvstendigRett).isFalse()
        assertThat(barnetrygdPerioder[1].stønadFom).isEqualTo(årMnd("2022-01"))
        assertThat(barnetrygdPerioder[1].stønadTom).isEqualTo(årMnd("2023-03"))
        assertThat(barnetrygdPerioder[1].søkerHarSelvstendigRett).isTrue()
        assertThat(barnetrygdPerioder).allSatisfy {
            assertThat(it.personIdent).isEqualTo(barnAktør.aktivFødselsnummer())
            assertThat(it.utbetaltPerMnd).isEqualTo(660)
            assertThat(it.ytelseTypeEkstern).isEqualTo(YtelseTypeEkstern.ORDINÆR_BARNETRYGD)
        }
    }

    @Test
    fun `skal splitte per andel og per selvstendig rett`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør, satsendringFom = årMnd("2021-01"), selvstendigRettFom = årMnd("2022-01"))

        // Act
        val barnetrygdPerioder =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2019, 1, 1))
                .single()
                .barnetrygdPerioder
                .sortedBy { it.stønadFom }

        // Assert
        assertThat(barnetrygdPerioder).hasSize(3)
        assertThat(barnetrygdPerioder[0].stønadFom).isEqualTo(årMnd("2019-04"))
        assertThat(barnetrygdPerioder[0].stønadTom).isEqualTo(årMnd("2020-12"))
        assertThat(barnetrygdPerioder[0].utbetaltPerMnd).isEqualTo(660)
        assertThat(barnetrygdPerioder[0].søkerHarSelvstendigRett).isFalse()
        assertThat(barnetrygdPerioder[1].stønadFom).isEqualTo(årMnd("2021-01"))
        assertThat(barnetrygdPerioder[1].stønadTom).isEqualTo(årMnd("2021-12"))
        assertThat(barnetrygdPerioder[1].utbetaltPerMnd).isEqualTo(1054)
        assertThat(barnetrygdPerioder[1].søkerHarSelvstendigRett).isFalse()
        assertThat(barnetrygdPerioder[2].stønadFom).isEqualTo(årMnd("2022-01"))
        assertThat(barnetrygdPerioder[2].stønadTom).isEqualTo(årMnd("2023-03"))
        assertThat(barnetrygdPerioder[2].utbetaltPerMnd).isEqualTo(1054)
        assertThat(barnetrygdPerioder[2].søkerHarSelvstendigRett).isTrue()
    }

    @Test
    fun `skal splitte barnetrygdperioden når søkers bosatt i riket-vilkår er løpende`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør, selvstendigRettFom = årMnd("2022-01"), søkersVilkårErLøpende = true)

        // Act
        val barnetrygdPerioder =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2019, 1, 1))
                .single()
                .barnetrygdPerioder
                .sortedBy { it.stønadFom }

        // Assert
        assertThat(barnetrygdPerioder).hasSize(2)
        assertThat(barnetrygdPerioder[0].stønadFom).isEqualTo(årMnd("2019-04"))
        assertThat(barnetrygdPerioder[0].stønadTom).isEqualTo(årMnd("2021-12"))
        assertThat(barnetrygdPerioder[0].søkerHarSelvstendigRett).isFalse()
        assertThat(barnetrygdPerioder[1].stønadFom).isEqualTo(årMnd("2022-01"))
        assertThat(barnetrygdPerioder[1].stønadTom).isEqualTo(årMnd("2023-03"))
        assertThat(barnetrygdPerioder[1].søkerHarSelvstendigRett).isTrue()
    }

    @Test
    fun `skal splitte perioden for hvert barn når søker har selvstendig rett i deler av perioden`() {
        // Arrange
        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val barn1Aktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)
        val barn2Aktør = personidentService.hentOgLagreAktør(barn2.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(
            fagsak,
            barn1,
            barn1Aktør,
            selvstendigRettFom = årMnd("2022-01"),
            barn2 = barn2 to barn2Aktør,
        )

        // Act
        val barnetrygdPerioder =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder

        // Assert
        assertThat(barnetrygdPerioder).hasSize(4)
        listOf(barn1Aktør, barn2Aktør).forEach { barnAktør ->
            val perioderForBarn = barnetrygdPerioder.filter { it.personIdent == barnAktør.aktivFødselsnummer() }.sortedBy { it.stønadFom }
            assertThat(perioderForBarn).hasSize(2)
            assertThat(perioderForBarn[0].stønadFom).isEqualTo(årMnd("2019-04"))
            assertThat(perioderForBarn[0].stønadTom).isEqualTo(årMnd("2021-12"))
            assertThat(perioderForBarn[0].søkerHarSelvstendigRett).isFalse()
            assertThat(perioderForBarn[1].stønadFom).isEqualTo(årMnd("2022-01"))
            assertThat(perioderForBarn[1].stønadTom).isEqualTo(årMnd("2023-03"))
            assertThat(perioderForBarn[1].søkerHarSelvstendigRett).isTrue()
        }
    }

    @Test
    fun `skal returnere én fagsak per fagsakeier når relatert fagsak finnes via flere barn`() {
        // Arrange
        val søker = tilfeldigPerson()
        val annenForelder = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val annenForelderAktør = personidentService.hentOgLagreAktør(annenForelder.aktør.aktivFødselsnummer(), true)
        val barn1Aktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)
        val barn2Aktør = personidentService.hentOgLagreAktør(barn2.aktør.aktivFødselsnummer(), true)

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barn1Aktør, barn2 = barn2 to barn2Aktør)

        // Begge barna finnes i annen forelders fagsak, som dermed er relatert via to barn.
        // NB: hentBarnetrygd dedupliserer relaterte fagsaker for å unngå doble oppslag, men resultatet ville
        // uansett blitt gruppert per fagsakeier til slutt, så denne testen dekker formen på svaret – ikke dedupliseringen.
        val relatertFagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(annenForelder.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(relatertFagsak, barn1, barn1Aktør, barn2 = barn2 to barn2Aktør)

        // Act
        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))

        // Assert
        assertThat(barnetrygdTilPensjon.map { it.fagsakEiersIdent })
            .containsExactlyInAnyOrder(søkerAktør.aktivFødselsnummer(), annenForelderAktør.aktivFødselsnummer())
    }

    @Test
    fun `skal utlede søkerHarSelvstendigRett fra hver fagsaks egen søker, ikke fra den det spørres på`() {
        // Arrange
        val søker = tilfeldigPerson()
        val annenForelder = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val annenForelderAktør = personidentService.hentOgLagreAktør(annenForelder.aktør.aktivFødselsnummer(), true)
        val barnAktør = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)

        // Søker har ikke selvstendig rett, mens annen forelder (relatert fagsak via samme barn) har det
        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(fagsak, barn1, barnAktør)

        val relatertFagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(annenForelder.aktør.aktivFødselsnummer())
        leggTilAvsluttetBehandling(relatertFagsak, barn1, barnAktør, selvstendigRettFom = årMnd("2019-04"))

        // Act
        val barnetrygdTilPensjon = pensjonService.hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))

        // Assert
        val perioderForSøker = barnetrygdTilPensjon.single { it.fagsakEiersIdent == søkerAktør.aktivFødselsnummer() }.barnetrygdPerioder
        val perioderForAnnenForelder =
            barnetrygdTilPensjon.single { it.fagsakEiersIdent == annenForelderAktør.aktivFødselsnummer() }.barnetrygdPerioder
        assertThat(perioderForSøker).isNotEmpty()
        assertThat(perioderForSøker).allSatisfy { assertThat(it.søkerHarSelvstendigRett).isFalse() }
        assertThat(perioderForAnnenForelder).isNotEmpty()
        assertThat(perioderForAnnenForelder).allSatisfy { assertThat(it.søkerHarSelvstendigRett).isTrue() }
    }

    @Test
    fun `skal ikke sette søkerHarSelvstendigRett på perioder fra Infotrygd`() {
        // Arrange
        val søker = tilfeldigPerson()
        val søkerAktør = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)

        mockInfotrygdBarnetrygdResponse(søkerAktør)

        // Act
        val barnetrygdPeriode =
            pensjonService
                .hentBarnetrygd(søkerAktør.aktivFødselsnummer(), LocalDate.of(2023, 1, 1))
                .single()
                .barnetrygdPerioder
                .single()

        // Assert
        assertThat(barnetrygdPeriode.kildesystem).isEqualTo("Infotrygd")
        assertThat(barnetrygdPeriode.søkerHarSelvstendigRett).isNull()
    }

    private fun leggTilAvsluttetBehandling(
        fagsak: Fagsak,
        barn1: Person,
        barnAktør: Aktør,
        fom: YearMonth = årMnd("2019-04"),
        tom: YearMonth = årMnd("2023-03"),
        satsendringFom: YearMonth? = null,
        selvstendigRettFom: YearMonth? = null,
        søkersVilkårErLøpende: Boolean = false,
        barn2: Pair<Person, Aktør>? = null,
    ) {
        with(behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsak))) {
            val behandling = this
            lagreSøkerOgBarnMedVilkårsvurdering(
                behandling,
                listOfNotNull(barnAktør, barn2?.second),
                fom,
                tom,
                selvstendigRettFom,
                søkersVilkårErLøpende,
            )
            with(lagInitiellTilkjentYtelse(behandling, lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id))) {
                val andelPerioder =
                    if (satsendringFom == null) {
                        listOf(Triple(fom, tom, 660))
                    } else {
                        listOf(Triple(fom, satsendringFom.minusMonths(1), 660), Triple(satsendringFom, tom, 1054))
                    }
                listOfNotNull(barn1 to barnAktør, barn2).forEach { (barn, aktør) ->
                    andelPerioder.forEach { (andelFom, andelTom, beløp) ->
                        andelerTilkjentYtelse.add(
                            lagAndelTilkjentYtelse(
                                andelFom,
                                andelTom,
                                YtelseType.ORDINÆR_BARNETRYGD,
                                beløp,
                                behandling,
                                person = barn,
                                aktør = aktør,
                                tilkjentYtelse = this,
                            ),
                        )
                    }
                }
                tilkjentYtelseRepository.save(this)
            }
            avsluttOgLagreBehandling(behandling)
        }
    }

    private fun lagreSøkerOgBarnMedVilkårsvurdering(
        behandling: Behandling,
        barnAktører: List<Aktør>,
        fom: YearMonth,
        tom: YearMonth,
        selvstendigRettFom: YearMonth?,
        søkersVilkårErLøpende: Boolean,
    ) {
        require(selvstendigRettFom == null || selvstendigRettFom <= tom) { "selvstendigRettFom må være innenfor andelen" }
        val søkerAktør = behandling.fagsak.aktør
        personopplysningGrunnlagRepository.save(
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
                barnasIdenter = barnAktører.map { it.aktivFødselsnummer() },
                søkerAktør = søkerAktør,
                barnAktør = barnAktører,
            ),
        )

        val vilkårFom = fom.minusMonths(1).atDay(15)
        val vilkårTom = if (søkersVilkårErLøpende) null else tom.atEndOfMonth()
        val bosattIRiketPerioder =
            if (selvstendigRettFom == null || selvstendigRettFom <= fom) {
                listOf(Triple(vilkårFom, vilkårTom, selvstendigRettFom != null))
            } else {
                listOf(
                    Triple(vilkårFom, selvstendigRettFom.minusMonths(1).atEndOfMonth(), false),
                    Triple(selvstendigRettFom.atDay(1), vilkårTom, true),
                )
            }
        val vilkårsvurdering =
            lagVilkårsvurdering(
                behandling = behandling,
                lagPersonResultater = { vilkårsvurdering ->
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = vilkårsvurdering,
                            aktør = søkerAktør,
                            lagVilkårResultater = { personResultat ->
                                bosattIRiketPerioder
                                    .map { (periodeFom, periodeTom, harSelvstendigRett) ->
                                        lagVilkårResultat(
                                            personResultat = personResultat,
                                            vilkårType = Vilkår.BOSATT_I_RIKET,
                                            periodeFom = periodeFom,
                                            periodeTom = periodeTom,
                                            behandlingId = behandling.id,
                                            utdypendeVilkårsvurderinger =
                                                listOfNotNull(UtdypendeVilkårsvurdering.ANNEN_FORELDER_OMFATTET_AV_NORSK_LOVGIVNING.takeIf { harSelvstendigRett }),
                                        )
                                    }.toSet()
                            },
                        ),
                        *barnAktører
                            .map { barnAktør ->
                                lagPersonResultat(
                                    vilkårsvurdering = vilkårsvurdering,
                                    aktør = barnAktør,
                                )
                            }.toTypedArray(),
                    )
                },
            )
        vilkårsvurderingService.lagreNyOgDeaktiverGammel(vilkårsvurdering)
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingHentOgPersisterService.lagreEllerOppdater(behandling, false)
    }

    private fun mockInfotrygdBarnetrygdResponse(
        søker: Aktør,
        barn: Aktør? = null,
        stønadFom: YearMonth = YearMonth.now(),
        stønadTom: YearMonth = YearMonth.now(),
    ) {
        fakeInfotrygdBarnetrygdKlient.leggTilBarnetrygdTilPensjon(
            søker.aktivFødselsnummer(),
            BarnetrygdTilPensjonResponse(
                fagsaker =
                    listOf(
                        BarnetrygdTilPensjon(
                            søker.aktivFødselsnummer(),
                            listOf(
                                BarnetrygdPeriode(
                                    personIdent = barn?.aktivFødselsnummer() ?: søker.aktivFødselsnummer(),
                                    delingsprosentYtelse = YtelseProsent.FULL,
                                    ytelseTypeEkstern = YtelseTypeEkstern.ORDINÆR_BARNETRYGD,
                                    utbetaltPerMnd = 1054,
                                    stønadFom = stønadFom,
                                    stønadTom = stønadTom,
                                    kildesystem = "Infotrygd",
                                    sakstypeEkstern = SakstypeEkstern.NASJONAL,
                                ),
                            ),
                        ),
                    ),
            ),
        )
    }
}
