package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.RestBrevmottaker
import no.nav.familie.ba.sak.ekstern.restDomene.tilBrevMottaker
import no.nav.familie.ba.sak.integrasjoner.pdl.PersonopplysningerService
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
) {

    @Transactional
    fun leggTilBrevmottaker(restBrevMottaker: RestBrevmottaker, behandlingId: Long) {
        val brevmottaker = restBrevMottaker.tilBrevMottaker(behandlingId)

        loggService.opprettBrevmottakerLogg(
            brevmottaker = brevmottaker,
            brevmottakerFjernet = false,
        )

        brevmottakerRepository.save(brevmottaker)
    }

    @Transactional
    fun fjernBrevmottaker(id: Long) {
        val brevmottaker =
            brevmottakerRepository.findByIdOrNull(id) ?: throw Feil("Finner ikke brevmottaker med id=$id")

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
        manueltRegistrerteMottakere: List<Brevmottaker>,
        søkersident: String,
        søkersnavn: String = hentMottakerNavn(søkersident),
    ): List<MottakerInfo> {
        val manuellDødsbo = manueltRegistrerteMottakere.filter { it.type == MottakerType.DØDSBO }
            .map {
                MottakerInfo(
                    brukerId = "",
                    brukerIdType = null,
                    erInstitusjonVerge = false,
                    navn = søkersnavn,
                    manuellAdresseInfo = lagManuellAdresseInfo(it),
                )
            }.singleOrNull()

        if (manuellDødsbo != null) {
            // brev sendes kun til den manuelt registerte dødsboadressen
            return manuellDødsbo.toList()
        }

        val manuellAdresseUtenlands = manueltRegistrerteMottakere.filter { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
            .zeroSingleOrThrow {
                FunksjonellFeil("Mottakerfeil: Det er registret mer enn en utenlandsk adresse tilhørende bruker")
            }?.let {
                MottakerInfo(
                    brukerId = søkersident,
                    brukerIdType = BrukerIdType.FNR,
                    erInstitusjonVerge = false,
                    navn = søkersnavn,
                    manuellAdresseInfo = lagManuellAdresseInfo(it),
                )
            }

        // brev sendes til brukers (manuelt) registerte adresse (i utlandet)
        val bruker = manuellAdresseUtenlands ?: MottakerInfo(
            brukerId = søkersident,
            brukerIdType = BrukerIdType.FNR,
            erInstitusjonVerge = false,
            navn = søkersnavn,
        )

        // ...og evt. til en manuelt registrert verge eller fullmektig i tillegg
        val manuellTilleggsmottaker = manueltRegistrerteMottakere.filter { it.type != MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
            .zeroSingleOrThrow {
                FunksjonellFeil("Mottakerfeil: ${first().type.visningsnavn} kan ikke kombineres med ${last().type.visningsnavn}")
            }?.let {
                MottakerInfo(
                    brukerId = "",
                    brukerIdType = null,
                    erInstitusjonVerge = false,
                    navn = it.navn,
                    manuellAdresseInfo = lagManuellAdresseInfo(it),
                )
            }

        return listOfNotNull(bruker, manuellTilleggsmottaker)
    }

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

private fun List<Brevmottaker>.zeroSingleOrThrow(exception: List<Brevmottaker>.() -> Exception): Brevmottaker? =
    if (size in 0..1) {
        singleOrNull()
    } else {
        throw exception()
    }
