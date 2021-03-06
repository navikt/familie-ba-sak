package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import org.springframework.stereotype.Service

@Service
class OppholdService(
        private val personopplysningerService: PersonopplysningerService
){
    fun hentOpphold(person: Person): List<GrOpphold> =
            personopplysningerService.hentOpphold(person.personIdent.ident).map { GrOpphold(gyldigPeriode = DatoIntervallEntitet(fom = it.oppholdFra,
                                                                                                                                 tom = it.oppholdTil),
                                                                                            type = it.type,
                                                                                            person = person)}
}