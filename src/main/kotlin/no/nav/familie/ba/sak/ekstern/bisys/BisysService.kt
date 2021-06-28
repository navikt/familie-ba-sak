package no.nav.familie.ba.sak.ekstern.bisys


import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.kontrakter.felles.personopplysning.Ident
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BisysService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
                   private val personopplysningerService: PersonopplysningerService) {
    fun hentUtvidetBarnetrygd(personIdent: String, fraDato: LocalDate): BisysUtvidetBarnetrygdResponse {
        val folkeregisteridenter = personopplysningerService.hentIdenter(Ident(personIdent)).filter {
            it.gruppe == "FOLKEREGISTERIDENT"
        }.map { it.ident }

        val samledeUtvidetBarnetrygdPerioder = mutableListOf<UtvidetBarnetrygdPeriode>()

        folkeregisteridenter.forEach { ident ->
            val perioder = infotrygdBarnetrygdClient.hentUtvidetBarnetrygd(ident, fraDato.toYearMonth()).perioder
            samledeUtvidetBarnetrygdPerioder.addAll(perioder)
        }

        return BisysUtvidetBarnetrygdResponse(samledeUtvidetBarnetrygdPerioder.sortedWith(compareBy({ it.stønadstype }, { it.fomMåned})))
    }
}