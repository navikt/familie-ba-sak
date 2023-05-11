package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.BehandlingValidering.validerBehandlingKanRedigeres
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.ekstern.restDomene.tilBrevMottaker
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.domene.ManuellAdresseInfo
import no.nav.familie.ba.sak.kjerne.steg.domene.MottakerInfo
import no.nav.familie.ba.sak.kjerne.steg.domene.toList
import no.nav.familie.kontrakter.felles.BrukerIdType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
    private val personidentService: PersonidentService,
    private val personopplysningerService: PersonopplysningerService,
    private val behandlingHentOgPersisterService: BehandlingHentOgPersisterService
) {

    @Transactional
    fun leggTilBrevmottaker(restBrevMottaker: RestBrevmottaker, behandlingId: Long) {
        val brevmottaker = restBrevMottaker.tilBrevMottaker(behandlingId)
        validerBehandlingKanRedigeres(behandlingHentOgPersisterService.hentStatus(behandlingId))

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = false,
        )

        brevmottakerRepository.save(brevmottaker)
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
        val brevmottaker =
            brevmottakerRepository.findByIdOrNull(id) ?: throw Feil("Finner ikke brevmottaker med id=$id")
        validerBehandlingKanRedigeres(behandlingHentOgPersisterService.hentStatus(brevmottaker.behandlingId))

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = true,
        )

        brevmottakerRepository.deleteById(id)
    }

    fun hentBrevmottakere(behandlingId: Long) = brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId)

    fun hentRestBrevmottakere(behandlingId: Long) =
        brevmottakerRepository.finnBrevMottakereForBehandling(behandlingId).map {
            RestBrevmottaker(
                id = it.id,
                type = it.type,
                navn = it.navn,
                adresselinje1 = it.adresselinje1,
                adresselinje2 = it.adresselinje2,
                postnummer = it.postnummer,
                poststed = it.poststed,
                landkode = it.landkode,
            )
        }

    fun lagMottakereFraBrevMottakere(
        brevMottakere: List<Brevmottaker>,
        søkersident: String,
        søkersnavn: String = hentMottakerNavn(søkersident),
    ): List<MottakerInfo> =
        brevMottakere.map { brevmottaker ->
            when (brevmottaker.type) {
                MottakerType.FULLMEKTIG, MottakerType.VERGE -> {
                    val finnesBrevmottakerMedUtenlandskAdresse =
                        brevMottakere.any { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                    if (finnesBrevmottakerMedUtenlandskAdresse) { // brev sendes til fullmektig adresse og bruker sin manuell adresse
                        MottakerInfo(
                            brukerId = søkersident,
                            brukerIdType = BrukerIdType.FNR,
                            erInstitusjonVerge = false,
                            navn = brevmottaker.navn,
                            manuellAdresseInfo = lagManuellAdresseInfo(brevmottaker),
                        ).toList()
                    } else { // brev sendes til fullmektig adresse og bruker sin registerte adresse
                        listOf(
                            MottakerInfo(
                                brukerId = søkersident,
                                brukerIdType = BrukerIdType.FNR,
                                erInstitusjonVerge = false,
                                navn = brevmottaker.navn,
                                manuellAdresseInfo = lagManuellAdresseInfo(brevmottaker),
                            ),
                            MottakerInfo(
                                brukerId = søkersident,
                                brukerIdType = BrukerIdType.FNR,
                                erInstitusjonVerge = false,
                                navn = søkersnavn,
                            ),
                        )
                    }
                }

                MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE, MottakerType.DØDSBO ->
                    // brev sendes til kun bruker sin registerte/manuell adresse
                    MottakerInfo(
                        brukerId = søkersident,
                        brukerIdType = BrukerIdType.FNR,
                        erInstitusjonVerge = false,
                        navn = søkersnavn,
                        manuellAdresseInfo = lagManuellAdresseInfo(brevmottaker),
                    ).toList()
            }
        }.flatten()

    // burde denne ta i bruk aktør?
    fun hentMottakerNavn(personIdent: String): String {
        val aktør = personidentService.hentAktør(personIdent)
        return personopplysningerService.hentPersoninfoNavnOgAdresse(aktør).let {
            it.navn!!
        }
    }

    private fun lagManuellAdresseInfo(brevmottaker: Brevmottaker) = ManuellAdresseInfo(
        adresselinje1 = brevmottaker.adresselinje1,
        adresselinje2 = brevmottaker.adresselinje2,
        postnummer = brevmottaker.postnummer,
        poststed = brevmottaker.poststed,
        landkode = brevmottaker.landkode,
    )
}
