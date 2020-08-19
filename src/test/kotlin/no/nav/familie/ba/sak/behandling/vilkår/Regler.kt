package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.junit.jupiter.api.Test
import java.time.LocalDate


class ReglerTest {
    @Test
    fun `foo`() {
        val fakta = Fakta(Person(aktørId = randomAktørId(),
                personIdent = PersonIdent(randomFnr()),
                type = PersonType.SØKER,
                personopplysningGrunnlag = PersonopplysningGrunnlag(0, 0, mutableSetOf(), true),
                fødselsdato = LocalDate.of(1991, 1, 1),
                navn = "navn",
                kjønn = Kjønn.KVINNE,
                bostedsadresse = null,
                sivilstand = SIVILSTAND.GIFT))
        morHarJobbetINorgeSiste5År(fakta)
    }
}