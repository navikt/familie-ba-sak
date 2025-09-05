package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.kontrakter.ba.tss.SamhandlerAdresse
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class InstitusjonService(
    val fagsakRepository: FagsakRepository,
    val samhandlerKlient: SamhandlerKlient,
    val institusjonRepository: InstitusjonRepository,
    val integrasjonClient: IntegrasjonClient,
    val kodeverkService: KodeverkService,
) {
    fun hentEllerOpprettInstitusjon(
        orgNummer: String,
        tssEksternId: String?,
    ): Institusjon =
        institusjonRepository.findByOrgNummer(orgNummer) ?: institusjonRepository.saveAndFlush(
            Institusjon(
                orgNummer = orgNummer,
                tssEksternId = tssEksternId,
            ),
        )

    @Cacheable("samhandler", cacheManager = "shortCache")
    fun hentSamhandler(orgNummer: String): SamhandlerInfo {
        val samhandlerinfoFraTss = samhandlerKlient.hentSamhandler(orgNummer)
        val organisasjonsinfoFraEreg = integrasjonClient.hentOrganisasjon(orgNummer)
        val postnummer = organisasjonsinfoFraEreg.adresse?.postnummer
        return if (postnummer == null) {
            return samhandlerinfoFraTss
        } else {
            val poststed = kodeverkService.hentPoststed(postnummer) ?: ""

            val samhandlerAdresse =
                SamhandlerAdresse(
                    adresselinjer = listOfNotNull(organisasjonsinfoFraEreg.adresse?.adresselinje1, organisasjonsinfoFraEreg.adresse?.adresselinje2, organisasjonsinfoFraEreg.adresse?.adresselinje3),
                    postNr = postnummer,
                    postSted = poststed,
                    adresseType = organisasjonsinfoFraEreg.adresse?.type!!,
                )

            samhandlerinfoFraTss.copy(
                navn = organisasjonsinfoFraEreg.navn,
                adresser = listOf(samhandlerAdresse),
            )
        }
    }

    fun søkSamhandlere(
        navn: String?,
        postnummer: String?,
        område: String?,
    ): List<SamhandlerInfo> {
        val komplettSamhandlerListe = mutableListOf<SamhandlerInfo>()
        var side = 0
        do {
            val søkeresultat = samhandlerKlient.søkSamhandlere(navn, postnummer, område, side)
            side++
            komplettSamhandlerListe.addAll(søkeresultat.samhandlere)
        } while (søkeresultat.finnesMerInfo)

        return komplettSamhandlerListe
    }
}
