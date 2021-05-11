package no.nav.familie.ba.sak.bisys

import no.nav.familie.ba.sak.infotrygd.InfotrygdBarnetrygdClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class BisysService(private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient) {
    fun hentUtvidetBarnetrygd(ident: String, fraDato: LocalDate): List<UtvidetBarnetrygdPeriode> {
        val perioderMedUtvidetBarnetrygd = infotrygdBarnetrygdClient.hentPerioderMedUtvidetBarnetrygd(ident, fraDato);


        // TODO: Hent perioder bruker har hatt utvidet barnetrygd for ba-sak (ba-sak har ingen fagsaker enda med personer som har perioder med utvidet barnetrygd)
        // TODO: Slå sammen periodene til en felles BisysPeriode

        return perioderMedUtvidetBarnetrygd // + perioder fra ba-sak.
    }
}