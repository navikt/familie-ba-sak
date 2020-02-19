package no.nav.familie.ba.sak.util

import no.nav.familie.ba.sak.BehandlingIntegrationTest
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import java.time.LocalDate
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence


fun randomFnr(): String = UUID.randomUUID().toString()

private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

fun lagRandomSaksnummer(): String {
    return ThreadLocalRandom.current()
            .ints(BehandlingIntegrationTest.STRING_LENGTH.toLong(), 0, charPool.size)
            .asSequence()
            .map(charPool::get)
            .joinToString("")
}

fun lagTestPersonopplysningGrunnlag(behandlingId: Long,
                                    søkerPersonIdent: String,
                                    barnPersonIdent: String): PersonopplysningGrunnlag {
    val personopplysningGrunnlag = PersonopplysningGrunnlag(behandlingId = behandlingId)
    val søker = Person(personIdent = PersonIdent(søkerPersonIdent),
                       type = PersonType.SØKER,
                       personopplysningGrunnlag = personopplysningGrunnlag,
                       fødselsdato = LocalDate.of(2019, 1, 1))
    val barn = Person(personIdent = PersonIdent(barnPersonIdent),
                      type = PersonType.BARN,
                      personopplysningGrunnlag = personopplysningGrunnlag,
                      fødselsdato = LocalDate.of(2019, 1, 1))

    personopplysningGrunnlag.personer.add(søker)
    personopplysningGrunnlag.personer.add(barn)

    return personopplysningGrunnlag
}

