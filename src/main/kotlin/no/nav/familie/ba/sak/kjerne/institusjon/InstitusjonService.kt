package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InstitusjonService(
    val fagsakRepository: FagsakRepository,
    val samhandlerKlient: SamhandlerKlient
) {

    @Transactional
    fun registrerInstitusjonForFagsak(fagsakId: Long, institusjon: Institusjon) {
        var fagsak = fagsakRepository.finnFagsak(fagsakId)
        if (fagsak != null && fagsak.type == FagsakType.INSTITUSJON) {
            fagsak.institusjon = institusjon
            fagsakRepository.save((fagsak))
        } else {
            throw Feil("Registrer institusjon for fagsak som er ${fagsak?.type}")
        }
    }

    fun hentSamhandler(orgNummer: String): SamhandlerInfo {
        return samhandlerKlient.hentSamhandler(orgNummer)
    }

    fun søkSamhandlere(navn: String): List<SamhandlerInfo> {
        val komplettSamhandlerListe = mutableListOf<SamhandlerInfo>()
        var side = 0
        do {
            val søkeresultat = samhandlerKlient.søkSamhandlere(navn, side)
            side++
            komplettSamhandlerListe.addAll(søkeresultat.samhandlere)
        } while (søkeresultat.finnesMerInfo)

        return komplettSamhandlerListe
    }
}
