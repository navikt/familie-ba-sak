package no.nav.familie.ba.sak.kjerne.steg.grunnlagForNyBehandling

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrMatrikkeladresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrUkjentBosted
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrVegadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonopplysningGrunnlagForNyBehandlingServiceTest {
    val personidentService = mockk<PersonidentService>()
    val beregningService = mockk<BeregningService>()
    val persongrunnlagService = mockk<PersongrunnlagService>()

    private val personopplysningGrunnlagForNyBehandlingService = spyk(
        PersonopplysningGrunnlagForNyBehandlingService(
            personidentService = personidentService,
            beregningService = beregningService,
            persongrunnlagService = persongrunnlagService,
        ),
    )

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

    @Test
    fun `hentOgLagrePersonopplysningGrunnlag - skal kopiere persongrunnlaget fra forrige behandling ved satsendring`() {
        val forrigeBehandling = lagBehandling()
        val nyBehandling = lagBehandling(årsak = BehandlingÅrsak.SATSENDRING)
        val søker = PersonIdent(randomFnr())
        val barn = PersonIdent(randomFnr())
        val søkerPerson = lagPerson(personIdent = søker, id = 1)
        val barnPerson = lagPerson(personIdent = barn, id = 2)

        val kopiertPersonopplysningGrunnlag = slot<PersonopplysningGrunnlag>()

        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(forrigeBehandling.id, søkerPerson, barnPerson).also {
                it.personer.map { person ->
                    person.bostedsadresser.addAll(
                        lagGrBostedsadresser(
                            DatoIntervallEntitet(LocalDate.now(), LocalDate.now().plusMonths(4)),
                            person,
                        ),
                    )
                }
            }
        every { persongrunnlagService.hentAktivThrows(forrigeBehandling.id) } returns personopplysningGrunnlag
        every { persongrunnlagService.lagreOgDeaktiverGammel(capture(kopiertPersonopplysningGrunnlag)) } returns mockk()
        personopplysningGrunnlagForNyBehandlingService.opprettKopiEllerNyttPersonopplysningGrunnlag(
            nyBehandling,
            forrigeBehandling,
            søker.ident,
            listOf(barn.ident),
        )

        assertThat(kopiertPersonopplysningGrunnlag.captured.behandlingId, IsEqual(nyBehandling.id))
        assertThat(kopiertPersonopplysningGrunnlag.captured.personer.size, IsEqual(2))
        assertThat(kopiertPersonopplysningGrunnlag.captured.personer.all { it.id == 0L }, IsEqual(true))
    }

    private fun lagGrBostedsadresser(periode: DatoIntervallEntitet, person: Person): List<GrBostedsadresse> =
        listOf(
            GrVegadresse(1, "2", null, "123", "Testgate", "23", null, "0682"),
            GrUkjentBosted("Oslo"),
            GrMatrikkeladresse(1, "2", null, "0682", "23"),
        ).map {
            it.also {
                it.periode = periode
                it.person = person
            }
        }
}
