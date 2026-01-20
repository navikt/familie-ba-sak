package no.nav.familie.ba.sak.kjerne.fagsak

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.ekstern.restDomene.SkjermetBarnSøkerDto
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
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
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
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,
    @Autowired
    private val persongrunnlagService: PersongrunnlagService,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `Skal teste at man henter alle fagsakene til barnet`() {
        // Arrange
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

        val personopplysningGrunnlag = mutableListOf<PersonopplysningGrunnlag>()

        val fagsakMor = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingMor = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsakMor))
        personopplysningGrunnlag.add(persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor)))
        personopplysningGrunnlag.add(persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor)))
        behandlingService.oppdaterStatusPåBehandling(behandlingMor.id, BehandlingStatus.AVSLUTTET)
        val behandlingMor2 = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsakMor))
        personopplysningGrunnlag.add(persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingMor2)))

        val fagsakFar = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingFar = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(fagsakFar))
        personopplysningGrunnlag.add(persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingFar)))

        // Oppretter fagsak med tilhørende behandling og personopplsyninggrunnlag, og arkiverer den.
        val arkivertFagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(randomFnr())
        val behandlingArkivertFagsak = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandlingUtenId(arkivertFagsak))
        personopplysningGrunnlag.add(persongrunnlagService.lagreOgDeaktiverGammel(opprettGrunnlag(behandlingArkivertFagsak)))
        fagsakService.lagre(arkivertFagsak.also { it.arkivert = true })

        // Act
        val fagsaker = fagsakService.hentFagsakerPåPerson(barnAktør.first())

        // Assert
        assertEquals(2, fagsaker.size)
        assertThat(persongrunnlagRepository.findAll().map { it.id }).containsAll(personopplysningGrunnlag.map { it.id })
    }

    @Test
    fun `Skal kun hente løpende fagsak for søker`() {
        // Arrange
        val søker = lagPerson(type = PersonType.SØKER)

        val normalFagsakForSøker = opprettFagsakForPersonMedStatus(personIdent = søker.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.AVSLUTTET)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.OPPRETTET)

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(søker.aktør)

        // Assert
        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(normalFagsakForSøker, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente løpende institusjonsfagsak for søker`() {
        // Arrange
        val barn = lagPerson(type = PersonType.BARN)

        val normalFagsakForSøker = opprettFagsakForPersonMedStatus(personIdent = barn.aktør.aktivFødselsnummer(), fagsakStatus = FagsakStatus.LØPENDE, fagsakType = FagsakType.INSTITUSJON)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.LØPENDE)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.AVSLUTTET)
        opprettFagsakForPersonMedStatus(personIdent = randomFnr(), fagsakStatus = FagsakStatus.OPPRETTET)

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        // Assert
        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(normalFagsakForSøker, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente fagsak hvor barn har løpende andel`() {
        // Arrange
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

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        // Assert
        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsak, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal ikke hente fagsak hvor barn har andel som ikke er løpende`() {
        // Arrange
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

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(barn.aktør)

        // Assert
        assertEquals(0, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal hente to fagsaker hvis aktør er søker i en sak og blir mottatt barnetrygd for i en annen`() {
        // Arrange
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

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(person.aktør)

        // Assert
        assertEquals(2, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedSøkerSomDeltaker.first())
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.last())
    }

    @Test
    fun `Skal ikke hente fagsak hvis barn kun har løpende andeler i en gammel behandling som senere er opphørt`() {
        // Arrange
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

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørErSøkerEllerMottarLøpendeOrdinær(person.aktør)

        // Assert
        assertEquals(0, fagsakerMedSøkerSomDeltaker.size)
    }

    @Test
    fun `Skal returnere fagsak hvor person mottar løpende utvidet`() {
        // Arrange
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

        // Act
        val fagsakerMedPersonSomFårUtvidetEllerOrdinær = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        // Assert
        assertEquals(1, fagsakerMedPersonSomFårUtvidetEllerOrdinær.size)
        assertEquals(fagsakHvorPersonErSøker, fagsakerMedPersonSomFårUtvidetEllerOrdinær.single())
    }

    @Test
    fun `Skal returnere ikke fagsak hvor person mottok utvidet som ikke er løpende lenger`() {
        // Arrange
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

        // Act
        val fagsakerMedPersonSomFårUtvidetEllerOrdinær = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        // Assert
        assertEquals(0, fagsakerMedPersonSomFårUtvidetEllerOrdinær.size)
    }

    @Test
    fun `Skal kun hente én fagsak hvis aktør er søker i en sak (uten løpende utvidet) og blir mottatt barnetrygd for i en annen`() {
        // Arrange
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

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        // Assert
        assertEquals(1, fagsakerMedSøkerSomDeltaker.size)
        assertEquals(fagsakHvorPersonErBarn, fagsakerMedSøkerSomDeltaker.single())
    }

    @Test
    fun `Skal hente to fagsaker hvor person mottar løpende utvidet i en behandling og blir mottatt løpende ordinær for i en annen`() {
        // Arrange
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

        // Act
        val fagsakerMedSøkerSomDeltaker = fagsakService.finnAlleFagsakerHvorAktørHarLøpendeYtelseAvType(aktør = person.aktør, ytelseTyper = listOf(YtelseType.ORDINÆR_BARNETRYGD, YtelseType.UTVIDET_BARNETRYGD))

        // Assert
        assertEquals(2, fagsakerMedSøkerSomDeltaker.size)
        assertThat(fagsakerMedSøkerSomDeltaker).containsExactlyInAnyOrderElementsOf(listOf(fagsakHvorPersonErBarn, fagsakHvorPersonErSøker))
    }

    @Test
    fun `Skal returnere eksisterende fagsak ved forsøk på å opprette institusjon fagsak med org nummer som allerede finnes for person`() {
        // Arrange
        val barn = lagPerson(type = PersonType.BARN)
        val institusjon = InstitusjonDto(orgNummer = "123456789", tssEksternId = "testid")

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
        val institusjon = InstitusjonDto(orgNummer = "123456789", tssEksternId = "testid")
        val skjermetBarnSøker = SkjermetBarnSøkerDto(randomFnr())
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
