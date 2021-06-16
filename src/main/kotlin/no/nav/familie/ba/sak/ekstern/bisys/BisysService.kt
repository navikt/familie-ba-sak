package no.nav.familie.ba.sak.ekstern.bisys

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class BisysService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {
    fun hentUtvidetBarnetrygd(personIdent: String, fraDato: LocalDate) =
        infotrygdBarnetrygdClient.hentUtvidetBarnetrygd(personIdent, YearMonth.of(fraDato.year, fraDato.month))
}