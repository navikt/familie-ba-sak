package no.nav.familie.ba.sak.bisys

import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class BisysService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {
    fun hentUtvidetBarnetrygd(ident: String, fraDato: LocalDate) =
        infotrygdBarnetrygdClient.hentUtvidetBarnetrygd(ident, YearMonth.of(fraDato.year, fraDato.month))
}