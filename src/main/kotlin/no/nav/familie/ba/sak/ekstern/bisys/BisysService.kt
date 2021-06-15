package no.nav.familie.ba.sak.ekstern.bisys


import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.common.toYearMonth
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class BisysService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {
    fun hentUtvidetBarnetrygd(personIdent: String, fraDato: LocalDate) =
        infotrygdBarnetrygdClient.hentUtvidetBarnetrygd(personIdent, fraDato.toYearMonth())
}