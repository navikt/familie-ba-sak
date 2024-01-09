package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.lagPersonResultat
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class VilkårsvurderingValideringTest {

    @Test
    fun `skal kaste feil hvis søker vurderes etter nasjonal og minst ett barn etter EØS`() {
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søker = lagPersonEnkel(PersonType.SØKER)
        val barn1 = lagPersonEnkel(PersonType.BARN)
        val barn2 = lagPersonEnkel(PersonType.BARN)
        val personResultatSøker = byggPersonResultatForPerson(søker, Regelverk.NASJONALE_REGLER, vilkårsvurdering)
        val personResultatBarn1 = byggPersonResultatForPerson(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
        val personResultatBarn2 = byggPersonResultatForPerson(barn2, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

        vilkårsvurdering.personResultater = setOf(
            personResultatSøker,
            personResultatBarn1,
            personResultatBarn2
        )

        assertThrows<FunksjonellFeil> { validerIkkeBlandetRegelverk(vilkårsvurdering = vilkårsvurdering, søkerOgBarn = listOf(søker, barn1, barn2)) }
    }

    @Test
    fun `skal ikke kaste feil hvis både søker og barn vurderes etter eøs`() {
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søker = lagPersonEnkel(PersonType.SØKER)
        val barn1 = lagPersonEnkel(PersonType.BARN)
        val personResultatSøker = byggPersonResultatForPerson(søker, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
        val personResultatBarn1 = byggPersonResultatForPerson(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)

        vilkårsvurdering.personResultater = setOf(
            personResultatSøker,
            personResultatBarn1
        )

        assertDoesNotThrow { validerIkkeBlandetRegelverk(vilkårsvurdering = vilkårsvurdering, søkerOgBarn = listOf(søker, barn1)) }
    }

    @Test
    fun `skal ikke kaste feil hvis søker vurderes etter eøs, men barn vurderes etter nasjonal`() {
        val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
        val søker = lagPersonEnkel(PersonType.SØKER)
        val barn1 = lagPersonEnkel(PersonType.BARN)
        val personResultatSøker = byggPersonResultatForPerson(søker, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
        val personResultatBarn1 = byggPersonResultatForPerson(barn1, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

        vilkårsvurdering.personResultater = setOf(
            personResultatSøker,
            personResultatBarn1
        )

        assertDoesNotThrow { validerIkkeBlandetRegelverk(vilkårsvurdering = vilkårsvurdering, søkerOgBarn = listOf(søker, barn1)) }
    }

    private fun byggPersonResultatForPerson(person: PersonEnkel, regelverk: Regelverk, vilkårsvurdering: Vilkårsvurdering): PersonResultat{
        return lagPersonResultat(
            vilkårsvurdering = vilkårsvurdering,
            person = lagPerson(type = person.type, aktør = person.aktør),
            periodeFom = LocalDate.now().minusMonths(2),
            periodeTom = null,
            resultat = Resultat.OPPFYLT,
            vurderesEtter = regelverk,
            lagFullstendigVilkårResultat = true
        )
    }

    private fun lagPersonEnkel(personType: PersonType): PersonEnkel{
        return PersonEnkel(
            type = personType,
            aktør = randomAktør(),
            dødsfallDato = null,
            fødselsdato = if (personType == PersonType.SØKER) LocalDate.now().minusYears(34) else LocalDate.now().minusYears(4),
            målform = Målform.NB
        )
    }
}
