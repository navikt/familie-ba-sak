package no.nav.familie.ba.sak.ekstern.skatteetaten

import no.nav.familie.ba.sak.integrasjoner.infotrygd.InfotrygdBarnetrygdClient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioder
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerioderResponse
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPerson
import no.nav.familie.eksterne.kontrakter.skatteetaten.SkatteetatenPersonerResponse
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class SkatteetatenService(
    private val infotrygdBarnetrygdClient: InfotrygdBarnetrygdClient,
    private val fagsakRepository: FagsakRepository
) {

    fun finnPersonerMedUtvidetBarnetrygd(år: String): SkatteetatenPersonerResponse {
        val personerFraInfotrygd = infotrygdBarnetrygdClient.hentPersonerMedUtvidetBarnetrygd(år)
        val personerFraBaSak = hentPersonerMedUtvidetBarnetrygd(år)
        val personIdentSet = personerFraBaSak.map { it.ident }.toSet()

        val kombinertListe = personerFraBaSak + personerFraInfotrygd.brukere.filter { !personIdentSet.contains(it.ident) }

        return SkatteetatenPersonerResponse(kombinertListe)
    }

    fun finnPerioderMedUtvidetBarnetrygd(personer: List<String>, år: String): SkatteetatenPerioderResponse {
        val perioderFraInfotrygd = personer.mapNotNull { infotrygdBarnetrygdClient.hentPerioderMedUtvidetBarnetrygd(it, år) }
        val perioderFraBaSak = emptyList<SkatteetatenPerioder>() // Todo
        return SkatteetatenPerioderResponse(perioderFraInfotrygd + perioderFraBaSak)
    }

    private fun hentPersonerMedUtvidetBarnetrygd(år: String): List<SkatteetatenPerson> {
        return fagsakRepository.finnFagsakerMedUtvidetBarnetrygdInnenfor(
            fom = YearMonth.of(år.toInt(), 1),
            tom = YearMonth.of(år.toInt(), 12)
        )
            .map { SkatteetatenPerson(it.first.hentAktivIdent().ident, it.second.atStartOfDay()) }
    }
}