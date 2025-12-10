package no.nav.familie.ba.sak.kjerne.falskidentitet

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitet
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Kjønn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.Adresser
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import org.springframework.stereotype.Service

@Service
class FalskIdentitetService(
    val personRepository: PersonRepository,
    val pdlRestKlient: PdlRestKlient,
) {
    fun hentFalskIdentitet(aktør: Aktør): FalskIdentitet? {
        val pdlFalskIdentitet = pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer())
        if (pdlFalskIdentitet != null && pdlFalskIdentitet.erFalsk) {
            val person = personRepository.findByAktør(aktør).filter { it.personopplysningGrunnlag.aktiv }
            val adresser = person.firstOrNull()?.let { Adresser.opprettFra(it) }

            // Vurdere å bruke mer informasjon fra PdlFalskIdentitet hvis tilgjengelig
            // Henter navn, fødselsdato, kjønn og adresser fra personopplysningene i stedet for PDL
            return FalskIdentitet(
                navn = person.firstOrNull()?.navn ?: "Falsk identitet",
                fødselsdato = person.firstOrNull()?.fødselsdato,
                kjønn = person.firstOrNull()?.kjønn ?: Kjønn.UKJENT,
                adresser = adresser,
            )
        } else {
            return null
        }
    }
}
