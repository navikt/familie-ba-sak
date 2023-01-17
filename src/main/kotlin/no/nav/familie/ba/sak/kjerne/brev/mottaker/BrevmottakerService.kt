package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.ekstern.restDomene.tilBrevMottaker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakerService(
    @Autowired
    private val brevmottakerRepository: BrevmottakerRepository
) {

    @Transactional
    fun leggTilBrevmottaker(restBrevMottaker: RestBrevmottaker, behandlingId: Long) {
        brevmottakerRepository.save(restBrevMottaker.tilBrevMottaker(behandlingId))
    }

    @Transactional
    fun oppdaterBrevmottaker(restBrevmottaker: RestBrevmottaker, id: Long) {
        brevmottakerRepository.findById(id).orElseThrow { Feil("Finner ikke brevmottaker med id=$id") }.apply {
            type = restBrevmottaker.type
            navn = restBrevmottaker.navn
            adresselinje1 = restBrevmottaker.adresselinje1
            adresselinje2 = restBrevmottaker.adresselinje2
            postnummer = restBrevmottaker.postnummer
            poststed = restBrevmottaker.poststed
            landkode = restBrevmottaker.landkode
        }
    }

    @Transactional
    fun fjernBrevmottaker(id: Long) {
        brevmottakerRepository.deleteById(id)
    }

    fun hentBrevmottakere(behandlingId: Long) = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId).map {
        RestBrevmottaker(
            id = it.id,
            type = it.type,
            navn = it.navn,
            adresselinje1 = it.adresselinje1,
            adresselinje2 = it.adresselinje2,
            postnummer = it.postnummer,
            poststed = it.poststed,
            landkode = it.landkode
        )
    }
}
