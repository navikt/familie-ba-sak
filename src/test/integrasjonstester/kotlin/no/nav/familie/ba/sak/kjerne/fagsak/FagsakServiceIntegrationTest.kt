package no.nav.familie.ba.sak.kjerne.fagsak

import io.mockk.every
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.config.DatabaseCleanupService
import no.nav.familie.ba.sak.config.FakePdlIdentRestClient
import no.nav.familie.ba.sak.config.MockPersonopplysningerService.Companion.leggTilPersonInfo
import no.nav.familie.ba.sak.config.tilAktør
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.RestFagsakDeltager
import no.nav.familie.ba.sak.ekstern.restDomene.RestInstitusjon
import no.nav.familie.ba.sak.ekstern.restDomene.RestSkjermetBarnSøker
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.FamilieIntegrasjonerTilgangskontrollClient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.ForelderBarnRelasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.IdentInformasjon
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PersonInfo
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.NyBehandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.RegistrerPersongrunnlagDTO
import no.nav.familie.ba.sak.kjerne.steg.StegService
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringRepository
import no.nav.familie.ba.sak.statistikk.saksstatistikk.domene.SaksstatistikkMellomlagringType.SAK
import no.nav.familie.kontrakter.felles.personopplysning.FORELDERBARNRELASJONROLLE
import no.nav.familie.unleash.UnleashService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpServerErrorException
import java.time.LocalDate
import java.time.YearMonth

class FagsakServiceIntegrationTest(
    @Autowired
    private val fagsakService: FagsakService,
    @Autowired
    private val behandlingService: BehandlingService,
    @Autowired
    private val personidentService: PersonidentService,
    @Autowired
    private val stegService: StegService,
    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
    @Autowired
    private val databaseCleanupService: DatabaseCleanupService,
    @Autowired
    private val saksstatistikkMellomlagringRepository: SaksstatistikkMellomlagringRepository,
    @Autowired
    private val mockFamilieIntegrasjonerTilgangskontrollClient: FamilieIntegrasjonerTilgangskontrollClient,
    @Autowired
    private val unleashService: UnleashService,
    @Autowired
    private val fakePdlIdentRestClient: FakePdlIdentRestClient,
) : AbstractSpringIntegrationTest() {
    @BeforeEach
    fun init() {
        databaseCleanupService.truncate()
    }

    @Test
    fun `Skal teste at man henter alle fagsakene til barnet`() {
        val barnFnr = randomFnr()

        val barnAktør = personidentService.hentOgLagreAktørIder(listOf(barnFnr), true)

        fun opprettGrunnlag(behandling: Behandling) =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                behandling.fagsak.aktør.aktivFødselsnummer(),
                listOf(barnFnr),
                søkerAktør = behandling.fagsak.aktør,
                barnAktør = barnAktør,
            )

        val fagsakMor = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingMor = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsakMor))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor))
        behandlingService.oppdaterStatusPåBehandling(behandlingMor.id, BehandlingStatus.AVSLUTTET)
        val behandlingMor2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsakMor))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor2))

        val fagsakFar = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingFar = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsakFar))
        persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingFar))

        val fagsaker = fagsakService.hentFagsakerPåPerson(barnAktør.first())
        assertEquals(2, fagsaker.size)
        assertThat(persongrunnlagRepository.findAll()).hasSize(4)
    }

    @Test
    fun `Skal kun hente løpende fagsak for søker`() {
        val søker = lagPerson(type = PersonType.SØKER)

        val normalFagsakForSøker = opprettFagsakForPersonMedStatus(personIdent = søker.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.AVSLUTTET)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.OPPRETTET)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(søker.aktør)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(normalFagsakForSøker, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente løpende institusjonsfagsak for søker`() {
        val barn = lagPerson(type = PersonType.BARN)

        val normalFagsakForSøker = opprettFagsakForPersonMedStatus(personIdent = barn.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.LØPENDE, fagsakType = FagsakType.INSTITUSJON)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.AVSLUTTET)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.OPPRETTET)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(normalFagsakForSøker, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente fagsak hvor barn har løpende andel`() {
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsak = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = barn.aktør,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = barn.aktør,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsak, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsak, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal ikke hente fagsak hvor barn har andel som ikke er løpende`() {
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsak = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = barn.aktør,
                ),
            )
        opprettAndelerOgBehandling(fagsak = fagsak, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        assertEquals(0, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal hente to fagsaker hvis aktør er søker i en sak og blir mottatt barnetrygd for i en annen`() {
        val person = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(person.aktør.aktivFødselsnummer(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = person.aktør,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(person.aktør)

        assertEquals(2, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedSøkerSomDeltaker.first())
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.last())
    }

    @Test
    fun `Skal ikke hente fagsak hvis barn kun har løpende andeler i en gammel behandling som senere er opphørt`() {
        val person = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val gamlePerioder =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = person.aktør,
                ),
            )

        val nyePerioder =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = gamlePerioder) // gammel behandling
        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = nyePerioder) // ny behandling

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(person.aktør)

        assertEquals(0, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal returnere fagsak hvor person mottar løpende utvidet`() {
        val person = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer(), barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)

        val perioder =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = person.aktør,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = barn.aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErSøker, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioder)

        val fagsakerMedPersonSomFårUtvidetEllerOrdinær = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(1, fagsakerMedPersonSomFårUtvidetEllerOrdinær.size)
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedPersonSomFårUtvidetEllerOrdinær.single())
    }

    @Test
    fun `Skal returnere ikke fagsak hvor person mottok utvidet som ikke er løpende lenger`() {
        val person = lagPerson(type = PersonType.SØKER)
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer(), barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)

        val perioder =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = barn.aktør,
                    ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErSøker, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioder)

        val fagsakerMedPersonSomFårUtvidetEllerOrdinær = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(0, fagsakerMedPersonSomFårUtvidetEllerOrdinær.size)
    }

    @Test
    fun `Skal kun hente én fagsak hvis aktør er søker i en sak (uten løpende utvidet) og blir mottatt barnetrygd for i en annen`() {
        val person = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(person.aktør.aktivFødselsnummer(), FagsakStatus.LØPENDE) // Fagsak hvor person er søker, men ikke har noen løpende utvidet-andeler
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilAndeler =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = person.aktør,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilAndeler)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente to fagsaker hvor person mottar løpende utvidet i en behandling og blir mottatt løpende ordinær for i en annen`() {
        val person = lagPerson(type = PersonType.BARN)
        val barn = lagPerson(type = PersonType.BARN)
        personidentService.hentOgLagreAktørIder(listOf(person.aktør.aktivFødselsnummer(), barn.aktør.aktivFødselsnummer()), lagre = true)

        val fagsakHvorPersonErBarn = opprettFagsakForPersonMedStatus(randomFnr(), FagsakStatus.LØPENDE)
        val fagsakHvorPersonErSøker = opprettFagsakForPersonMedStatus(person.aktør.aktivFødselsnummer(), FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE) // Lager en ekstre fagsak for å teste at denne ikke kommer med

        val perioderTilFagsakBarn =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = person.aktør,
                ),
            )

        val perioderTilFagsakSøker =
            listOf(
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(10),
                    tom = YearMonth.now().minusMonths(3),
                    aktør = person.aktør,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
                PeriodeForAktør(
                    fom = YearMonth.now().minusMonths(2),
                    tom = YearMonth.now().plusMonths(6),
                    aktør = person.aktør,
                    ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                ),
            )

        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErBarn, barnasIdenter = listOf(person.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilFagsakBarn)
        opprettAndelerOgBehandling(fagsak = fagsakHvorPersonErSøker, barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()), perioderTilAndeler = perioderTilFagsakSøker)

        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        assertEquals(2, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.first())
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedSøkerSomDeltaker.last())
    }

    @Test
    fun `Skal returnere eksisterende fagsak ved forsøk på å opprette institusjon fagsak med org nummer som allerede finnes for person`() {
        // Arrange
        val barn = lagPerson(type = PersonType.BARN)
        val institusjon = RestInstitusjon(orgNummer = "123456789", tssEksternId = "testid")

        val fagsak = opprettFagsakForPersonMedStatus(personIdent = barn.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.AVSLUTTET, fagsakType = FagsakType.INSTITUSJON)

        // Act && Assert

        val returnertFagsak =
            fagsakService.hentEllerOpprettFagsak(
                fagsakRequest =
                    FagsakRequest(
                        personIdent = barn.aktør.aktivFødselsnummer(),
                        fagsakType = FagsakType.INSTITUSJON,
                        institusjon = institusjon,
                    ),
            )

        assertThat(returnertFagsak.data?.id).isEqualTo(fagsak.id)
    }

    @Test
    fun `Skal opprette fagsak for skjermet barn som ikke er automatisk behandling`() {
        // Arrange
        System.setProperty("mockFeatureToggleAnswer", "true")
        val barn = lagPerson(type = PersonType.BARN)

        // Act
        val opprettetFagsak = opprettFagsakForPersonMedStatus(personIdent = barn.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.AVSLUTTET, fagsakType = FagsakType.SKJERMET_BARN)

        // Assert
        val skjermetBarnSøker = opprettetFagsak.skjermetBarnSøker

        assertThat(skjermetBarnSøker).isNotNull
    }

    @Test
    fun `Skal ikke opprette fagsak for skjermet barn hvis automatisk behandling`() {
        // Arrange
        val barn = lagPerson(type = PersonType.BARN)

        System.setProperty("mockFeatureToggleAnswer", "true")

        // Act
        assertThrows<FunksjonellFeil> {
            opprettFagsakForPersonMedStatus(
                personIdent = barn.aktør.aktivFødselsnummer(),
                fagsakStatus = FagsakStatus.AVSLUTTET,
                fagsakType = FagsakType.SKJERMET_BARN,
                fraAutomatiskBehandling = true,
            )
        }.also {
            assertThat(it.melding).isEqualTo("Kan ikke opprette fagsak med fagsaktype SKJERMET_BARN automatisk")
        }
    }

    private data class PeriodeForAktør(
        val fom: YearMonth,
        val tom: YearMonth,
        val aktør: Aktør,
        val ytelseType: YtelseType = YtelseType.ORDINÆR_BARNETRYGD,
    )

    private fun opprettFagsakForPersonMedStatus(
        personIdent: String,
        fagsakStatus: FagsakStatus,
        fagsakType: FagsakType = FagsakType.NORMAL,
        fraAutomatiskBehandling: Boolean = false,
    ): Fagsak {
        val institusjon = RestInstitusjon(orgNummer = "123456789", tssEksternId = "testid")
        val skjermetBarnSøker = RestSkjermetBarnSøker(randomFnr())
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = personIdent,
                fraAutomatiskBehandling = fraAutomatiskBehandling,
                fagsakType = fagsakType,
                institusjon = if (fagsakType == FagsakType.INSTITUSJON) institusjon else null,
                skjermetBarnSøker = if (fagsakType == FagsakType.SKJERMET_BARN) skjermetBarnSøker else null,
            )
        return fagsakService.oppdaterStatus(fagsak, fagsakStatus)
    }

    private fun opprettAndelerOgBehandling(
        fagsak: Fagsak,
        barnasIdenter: List<String>,
        perioderTilAndeler: List<PeriodeForAktør>,
    ) {
        val nyBehandling =
            NyBehandling(
                kategori = BehandlingKategori.NASJONAL,
                underkategori = BehandlingUnderkategori.ORDINÆR,
                søkersIdent = fagsak.aktør.aktivFødselsnummer(),
                behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                navIdent = randomFnr(),
                barnasIdenter = barnasIdenter,
                søknadMottattDato = LocalDate.now().minusMonths(1),
                fagsakId = fagsak.id,
            )
        val behandling = behandlingService.opprettBehandling(nyBehandling = nyBehandling)
        val tilkjentYtelse = TilkjentYtelse(behandling = behandling, endretDato = LocalDate.now(), opprettetDato = LocalDate.now())
        val andelerTilkjentYtelse =
            perioderTilAndeler.map {
                lagAndelTilkjentYtelse(
                    fom = it.fom,
                    tom = it.tom,
                    aktør = it.aktør,
                    behandling = behandling,
                    tilkjentYtelse = tilkjentYtelse,
                    ytelseType = it.ytelseType,
                )
            }

        tilkjentYtelse.andelerTilkjentYtelse.addAll(andelerTilkjentYtelse)
        tilkjentYtelseRepository.save(tilkjentYtelse)

        behandlingService.oppdaterStatusPåBehandling(behandlingId = behandling.id, status = BehandlingStatus.AVSLUTTET)
    }
}
