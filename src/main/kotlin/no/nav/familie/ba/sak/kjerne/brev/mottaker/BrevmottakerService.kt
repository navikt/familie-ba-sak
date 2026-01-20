package no.nav.familie.ba.sak.kjerne.brev.mottaker

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.ekstern.restDomene.BrevmottakerDto
import no.nav.familie.ba.sak.ekstern.restDomene.tilBrevMottaker
import no.nav.familie.ba.sak.kjerne.behandling.ValiderBrevmottakerService
import no.nav.familie.ba.sak.kjerne.brev.domene.ManuellBrevmottaker
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakerService(
    private val brevmottakerRepository: BrevmottakerRepository,
    private val loggService: LoggService,
    private val validerBrevmottakerService: ValiderBrevmottakerService,
) {
    @Transactional
    fun leggTilBrevmottaker(
        brevMottakerDto: BrevmottakerDto,
        behandlingId: Long,
    ) {
        val brevmottaker = brevMottakerDto.tilBrevMottaker(behandlingId)

        validerBrevmottakerService.validerAtBehandlingIkkeInneholderStrengtFortroligePersonerMedManuelleBrevmottakere(
            behandlingId = behandlingId,
            nyBrevmottaker = brevmottaker,
            ekstraBarnLagtTilIBrev = emptyList(),
        )

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
            BrevmottakerDto(
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
        manueltRegistrerteMottakere: List<ManuellBrevmottaker>,
    ): List<MottakerInfo> =
        when {
            manueltRegistrerteMottakere.isEmpty() -> {
                listOf(Bruker)
            }

            manueltRegistrerteMottakere.any { it.type == MottakerType.DØDSBO } -> {
                val dodsbo = manueltRegistrerteMottakere.single { it.type == MottakerType.DØDSBO }
                listOf(Dødsbo(navn = dodsbo.navn, manuellAdresseInfo = lagManuellAdresseInfo(dodsbo)))
            }

            else -> {
                val brukerMedUtenlandskAdresseListe =
                    manueltRegistrerteMottakere
                        .filter { it.type == MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                        .map { BrukerMedUtenlandskAdresse(lagManuellAdresseInfo(it)) }
                if (brukerMedUtenlandskAdresseListe.size > 1) {
                    throw FunksjonellFeil("Mottakerfeil: Det er registrert mer enn en utenlandsk adresse tilhørende bruker")
                }
                val bruker = brukerMedUtenlandskAdresseListe.firstOrNull() ?: Bruker

                val tilleggsmottakerListe =
                    manueltRegistrerteMottakere.filter { it.type != MottakerType.BRUKER_MED_UTENLANDSK_ADRESSE }
                if (tilleggsmottakerListe.size > 1) {
                    throw FunksjonellFeil(
                        "Mottakerfeil: ${tilleggsmottakerListe.first().type.visningsnavn} kan ikke kombineres med ${tilleggsmottakerListe.last().type.visningsnavn}",
                    )
                }
                val tilleggsmottaker =
                    tilleggsmottakerListe.firstOrNull()?.let {
                        FullmektigEllerVerge(navn = it.navn, manuellAdresseInfo = lagManuellAdresseInfo(it))
                    }
                listOfNotNull(bruker, tilleggsmottaker)
            }
        }

    private fun lagManuellAdresseInfo(brevmottaker: ManuellBrevmottaker) =
        ManuellAdresseInfo(
            adresselinje1 = brevmottaker.adresselinje1,
            adresselinje2 = brevmottaker.adresselinje2,
            postnummer = brevmottaker.postnummer,
            poststed = brevmottaker.poststed,
            landkode = brevmottaker.landkode,
        )
}
