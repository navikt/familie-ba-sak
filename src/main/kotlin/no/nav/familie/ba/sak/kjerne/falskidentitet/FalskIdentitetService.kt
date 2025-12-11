package no.nav.familie.ba.sak.kjerne.falskidentitet

import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.FalskIdentitetPersonInfo
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
    fun hentFalskIdentitet(aktør: Aktør): FalskIdentitetPersonInfo? {
        val pdlFalskIdentitet = pdlRestKlient.hentFalskIdentitet(aktør.aktivFødselsnummer())
        if (pdlFalskIdentitet != null && pdlFalskIdentitet.erFalsk) {
            val personer = personRepository.findByAktør(aktør)
            val person = personer.firstOrNull { it.personopplysningGrunnlag.aktiv } ?: personer.firstOrNull()

            // Vurdere å bruke mer informasjon fra PdlFalskIdentitet hvis tilgjengelig
            // Henter navn, fødselsdato, kjønn og adresser fra personopplysningene i stedet for PDL
            return FalskIdentitetPersonInfo(
                navn = person?.navn ?: "Ukjent navn",
                fødselsdato = person?.fødselsdato,
                kjønn = person?.kjønn ?: Kjønn.UKJENT,
                adresser = person?.let { Adresser.opprettFra(it) },
            )
        } else {
            return null
        }
    }
}
