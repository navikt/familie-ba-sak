package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.falskidentitet.FalskIdentitetService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Dødsfall
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagForNyBehandlingServiceTest.Companion.validerAtPersonerIGrunnlagErLike
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrMatrikkeladresseBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrUkjentBostedBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.bostedsadresse.GrVegadresseBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.arbeidsforhold.GrArbeidsforhold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class PersonopplysningGrunnlagForNyBehandlingServiceTest {
    val personidentService = mockk<PersonidentService>()
    val beregningService = mockk<BeregningService>()
    val persongrunnlagService = mockk<PersongrunnlagService>()
    val falskIdentitetService = mockk<FalskIdentitetService>()

    private val personopplysningGrunnlagForNyBehandlingService =
        spyk(
            PersonopplysningGrunnlagForNyBehandlingService(
                personidentService = personidentService,
                beregningService = beregningService,
                persongrunnlagService = persongrunnlagService,
                falskIdentitetService = falskIdentitetService,
            ),
        )

    @Nested
    inner class OpprettKopiEllerNyttPersonopplysningGrunnlag {
        @Test
        fun `Skal sende med barna fra forrige behandling ved førstegangsbehandling nummer to`() {
            val søker = lagPerson()
            val barnFraForrigeBehandling = lagPerson(type = PersonType.BARN)
            val barn = lagPerson(type = PersonType.BARN)

            val barnFnr = barn.aktør.aktivFødselsnummer()
            val søkerFnr = søker.aktør.aktivFødselsnummer()

            val forrigeBehandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)
            val behandling = lagBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING)

            every { personidentService.hentOgLagreAktør(søkerFnr, true) } returns søker.aktør
            every { personidentService.hentOgLagreAktørIder(listOf(barnFnr), true) } returns listOf(barn.aktør)

            every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(forrigeBehandling.id) } returns
                listOf(barnFraForrigeBehandling.aktør)

            every { persongrunnlagService.hentSøkersMålform(forrigeBehandling.id) } returns søker.målform

            every {
                persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns PersonopplysningGrunnlag(behandlingId = behandling.id)

            personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
                behandling = behandling,
                forrigeBehandlingSomErVedtatt = forrigeBehandling,
                søkerIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
            )
            verify(exactly = 1) {
                persongrunnlagService.hentOgLagreSøkerOgBarnINyttGrunnlag(
                    aktør = søker.aktør,
                    barnFraInneværendeBehandling = listOf(barn.aktør),
                    barnFraForrigeBehandling = listOf(barnFraForrigeBehandling.aktør),
                    behandling = behandling,
                    målform = søker.målform,
                )
            }
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING"])
        fun `skal kopiere persongrunnlaget fra forrige behandling ved satsendring og månedlig valutajustering`(
            årsak: BehandlingÅrsak,
        ) {
            val forrigeBehandling = lagBehandling()
            val nyBehandling = lagBehandling(årsak = årsak)
            val søker = PersonIdent(randomFnr())
            val barn = PersonIdent(randomFnr())
            val søkerPerson = lagPerson(personIdent = søker, id = 1)
            val barnPerson = lagPerson(personIdent = barn, id = 2)

            val periode = DatoIntervallEntitet(LocalDate.now(), LocalDate.now().plusMonths(4))

            val kopiertPersonopplysningGrunnlag = slot<PersonopplysningGrunnlag>()

            val grVegadresse =
                GrVegadresseBostedsadresse(1, "2", null, "123", "Testgate", "23", null, "0682", "Oslo").medPeriodeOgPerson(periode, søkerPerson)
            val grUkjentBosted = GrUkjentBostedBostedsadresse("Oslo").medPeriodeOgPerson(periode, søkerPerson)
            val grMatrikkeladresse = GrMatrikkeladresseBostedsadresse(1, "2", null, "0682", "Oslo", "23").medPeriodeOgPerson(periode, søkerPerson)

            val statsborgerskap =
                GrStatsborgerskap(
                    id = 1,
                    gyldigPeriode = periode,
                    landkode = "N",
                    medlemskap = Medlemskap.EØS,
                    person = søkerPerson,
                )
            val opphold =
                GrOpphold(id = 1, gyldigPeriode = periode, type = OPPHOLDSTILLATELSE.PERMANENT, person = søkerPerson)
            val arbeidsforhold =
                GrArbeidsforhold(
                    id = 1,
                    periode = periode,
                    arbeidsgiverId = "1",
                    arbeidsgiverType = "AS",
                    person = søkerPerson,
                )
            val sivilstand =
                GrSivilstand(id = 1, fom = LocalDate.now(), type = SIVILSTANDTYPE.REGISTRERT_PARTNER, person = søkerPerson)
            val dødsfall =
                Dødsfall(
                    id = 1,
                    person = søkerPerson,
                    dødsfallDato = LocalDate.now(),
                    dødsfallAdresse = "Adresse",
                    dødsfallPostnummer = "1234",
                    dødsfallPoststed = "Oslo",
                )

            val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søkerPerson, barnPerson).also {
                    it.personer.map { person ->
                        if (person.aktør.aktivFødselsnummer() == søker.ident) {
                            person.bostedsadresser.addAll(
                                listOf(
                                    grVegadresse,
                                    grUkjentBosted,
                                    grMatrikkeladresse,
                                ),
                            )
                            person.statsborgerskap.addAll(listOf(statsborgerskap))
                            person.opphold.addAll(listOf(opphold))
                            person.arbeidsforhold.addAll(listOf(arbeidsforhold))
                            person.sivilstander.addAll(listOf(sivilstand))
                            person.dødsfall = dødsfall
                        }
                    }
                }
            every { persongrunnlagService.hentAktivThrows(forrigeBehandling.id) } returns personopplysningGrunnlag
            every { persongrunnlagService.lagreOgDeaktiverGammel(capture(kopiertPersonopplysningGrunnlag)) } returns mockk()
            every { personidentService.hentOgLagreAktør(any(), any()) } returns lagAktør(søker.ident)
            every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(forrigeBehandling.id) } returns
                listOf(
                    lagAktør(
                        barn.ident,
                    ),
                )
            personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
                nyBehandling,
                forrigeBehandling,
                søker.ident,
                listOf(barn.ident),
            )

            assertThat(kopiertPersonopplysningGrunnlag.captured.behandlingId).isEqualTo(nyBehandling.id)
            assertThat(kopiertPersonopplysningGrunnlag.captured.personer.size).isEqualTo(2)

            validerAtPersonerIGrunnlagErLike(personopplysningGrunnlag, kopiertPersonopplysningGrunnlag.captured, true)
        }

        @Test
        fun `skal kopiere søker og barn med tilkjent ytelse fra persongrunnlaget fra forrige behandling ved satsendring`() {
            val forrigeBehandling = lagBehandling()
            val nyBehandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING)
            val søker = PersonIdent(randomFnr())
            val barn1 = PersonIdent(randomFnr())
            val barn2 = PersonIdent(randomFnr())
            val søkerPerson = lagPerson(personIdent = søker, id = 1)
            val barnPerson1 = lagPerson(personIdent = barn1, id = 2)
            val barnPerson2 = lagPerson(personIdent = barn2, id = 3)

            val periode = DatoIntervallEntitet(LocalDate.now(), LocalDate.now().plusMonths(4))

            val kopiertPersonopplysningGrunnlag = slot<PersonopplysningGrunnlag>()

            val grVegadresse =
                GrVegadresseBostedsadresse(1, "2", null, "123", "Testgate", "23", null, "0682", "Oslo").medPeriodeOgPerson(periode, søkerPerson)
            val grUkjentBosted = GrUkjentBostedBostedsadresse("Oslo").medPeriodeOgPerson(periode, søkerPerson)
            val grMatrikkeladresse = GrMatrikkeladresseBostedsadresse(1, "2", null, "0682", "Oslo", "23").medPeriodeOgPerson(periode, søkerPerson)

            val statsborgerskap =
                GrStatsborgerskap(
                    id = 1,
                    gyldigPeriode = periode,
                    landkode = "N",
                    medlemskap = Medlemskap.EØS,
                    person = søkerPerson,
                )
            val opphold =
                GrOpphold(id = 1, gyldigPeriode = periode, type = OPPHOLDSTILLATELSE.PERMANENT, person = søkerPerson)
            val arbeidsforhold =
                GrArbeidsforhold(
                    id = 1,
                    periode = periode,
                    arbeidsgiverId = "1",
                    arbeidsgiverType = "AS",
                    person = søkerPerson,
                )
            val sivilstand =
                GrSivilstand(id = 1, fom = LocalDate.now(), type = SIVILSTANDTYPE.REGISTRERT_PARTNER, person = søkerPerson)
            val dødsfall =
                Dødsfall(
                    id = 1,
                    person = søkerPerson,
                    dødsfallDato = LocalDate.now(),
                    dødsfallAdresse = "Adresse",
                    dødsfallPostnummer = "1234",
                    dødsfallPoststed = "Oslo",
                )

            val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søkerPerson, barnPerson1, barnPerson2).also {
                    it.personer.map { person ->
                        if (person.aktør.aktivFødselsnummer() == søker.ident) {
                            person.bostedsadresser.addAll(
                                listOf(
                                    grVegadresse,
                                    grUkjentBosted,
                                    grMatrikkeladresse,
                                ),
                            )
                            person.statsborgerskap.addAll(listOf(statsborgerskap))
                            person.opphold.addAll(listOf(opphold))
                            person.arbeidsforhold.addAll(listOf(arbeidsforhold))
                            person.sivilstander.addAll(listOf(sivilstand))
                            person.dødsfall = dødsfall
                        }
                    }
                }

            val forventetPersonopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søkerPerson, barnPerson1).also {
                    it.personer.map { person ->
                        if (person.aktør.aktivFødselsnummer() == søker.ident) {
                            person.bostedsadresser.addAll(
                                listOf(
                                    grVegadresse,
                                    grUkjentBosted,
                                    grMatrikkeladresse,
                                ),
                            )
                            person.statsborgerskap.addAll(listOf(statsborgerskap))
                            person.opphold.addAll(listOf(opphold))
                            person.arbeidsforhold.addAll(listOf(arbeidsforhold))
                            person.sivilstander.addAll(listOf(sivilstand))
                            person.dødsfall = dødsfall
                        }
                    }
                }
            every { persongrunnlagService.hentAktivThrows(forrigeBehandling.id) } returns personopplysningGrunnlag
            every { persongrunnlagService.lagreOgDeaktiverGammel(capture(kopiertPersonopplysningGrunnlag)) } returns mockk()
            every { personidentService.hentOgLagreAktør(any(), any()) } returns lagAktør(søker.ident)
            every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(forrigeBehandling.id) } returns
                listOf(
                    lagAktør(
                        barn1.ident,
                    ),
                )
            personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
                nyBehandling,
                forrigeBehandling,
                søker.ident,
                listOf(barn1.ident),
            )

            assertThat(kopiertPersonopplysningGrunnlag.captured.behandlingId).isEqualTo(nyBehandling.id)
            assertThat(kopiertPersonopplysningGrunnlag.captured.personer.size).isEqualTo(2)

            validerAtPersonerIGrunnlagErLike(
                forventetPersonopplysningGrunnlag,
                kopiertPersonopplysningGrunnlag.captured,
                true,
            )
        }

        @Test
        fun `skal kaste feil dersom behandling er satsendring og forrige behandling er null`() {
            val nyBehandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING)
            val søker = PersonIdent(randomFnr())
            val barn = PersonIdent(randomFnr())
            assertThatThrownBy {
                personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
                    behandling = nyBehandling,
                    forrigeBehandlingSomErVedtatt = null,
                    søker.ident,
                    listOf(barn.ident),
                )
            }.isInstanceOf(Feil::class.java)
        }

        @Test
        fun `skal kopiere søker og barn med tilkjent ytelse fra persongrunnlaget fra forrige behandling og markere falske identiteter ved årsak falsk identitet`() {
            // Arrange
            val forrigeBehandling = lagBehandling()
            val nyBehandling = lagBehandling(årsak = BehandlingÅrsak.FALSK_IDENTITET)
            val søker = PersonIdent(randomFnr())
            val barn = PersonIdent(randomFnr())
            val søkerPerson = lagPerson(personIdent = søker, id = 1)
            val barnPerson = lagPerson(personIdent = barn, id = 2)

            val personopplysningGrunnlag = lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søkerPerson, barnPerson)
            val forventetPersonopplysningGrunnlag = lagTestPersonopplysningGrunnlag(nyBehandling.id, søkerPerson.copy(harFalskIdentitet = true), barnPerson.copy())

            val kopiertPersonopplysningGrunnlag = slot<PersonopplysningGrunnlag>()

            every { persongrunnlagService.hentAktivThrows(forrigeBehandling.id) } returns personopplysningGrunnlag
            every { persongrunnlagService.lagreOgDeaktiverGammel(capture(kopiertPersonopplysningGrunnlag)) } returns mockk()
            every { personidentService.hentOgLagreAktør(any(), any()) } returns lagAktør(søker.ident)
            every { falskIdentitetService.harFalskIdentitet(søkerPerson.aktør) } returns true
            every { falskIdentitetService.harFalskIdentitet(barnPerson.aktør) } returns false
            every { beregningService.finnBarnFraBehandlingMedTilkjentYtelse(forrigeBehandling.id) } returns
                listOf(
                    lagAktør(
                        barn.ident,
                    ),
                )

            // Act
            personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(nyBehandling, forrigeBehandling, søker.ident, listOf(barn.ident))

            // Assert
            val personopplysningGrunnlagNyBehandling = kopiertPersonopplysningGrunnlag.captured

            assertThat(personopplysningGrunnlagNyBehandling.behandlingId).isEqualTo(nyBehandling.id)
            assertThat(personopplysningGrunnlagNyBehandling.personer.size).isEqualTo(2)
            assertThat(personopplysningGrunnlagNyBehandling.personer.single { it.aktør.aktivFødselsnummer() == søker.ident }.harFalskIdentitet).isTrue
            assertThat(personopplysningGrunnlagNyBehandling.personer.single { it.aktør.aktivFødselsnummer() == barn.ident }.harFalskIdentitet).isFalse

            validerAtPersonerIGrunnlagErLike(
                forventetPersonopplysningGrunnlag,
                kopiertPersonopplysningGrunnlag.captured,
                true,
            )
        }
    }

    private fun GrBostedsadresse.medPeriodeOgPerson(
        periode: DatoIntervallEntitet,
        person: Person,
    ): GrBostedsadresse =
        this.also {
            it.periode = periode
            it.person = person
        }
}
