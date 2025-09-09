package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.IntegrasjonClient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.samhandler.SamhandlerKlient
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.eøs.felles.BehandlingId
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.kontrakter.ba.tss.Gyldighetsperiode
import no.nav.familie.kontrakter.ba.tss.SamhandlerAdresse
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class InstitusjonService(
    val fagsakRepository: FagsakRepository,
    val samhandlerKlient: SamhandlerKlient,
    val institusjonRepository: InstitusjonRepository,
    val institusjonsinfoRepository: InstitusjonsinfoRepository,
    val integrasjonClient: IntegrasjonClient,
    val kodeverkService: KodeverkService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
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
        return if (organisasjonsinfoFraEreg.adresse == null) {
            return samhandlerinfoFraTss
        } else {
            val adresse = organisasjonsinfoFraEreg.adresse!!

            val postnummer = adresse.postnummer
            val poststed = kodeverkService.hentPoststed(postnummer) ?: ""

            val samhandlerAdresse =
                SamhandlerAdresse(
                    adresselinjer = listOfNotNull(organisasjonsinfoFraEreg.adresse?.adresselinje1, organisasjonsinfoFraEreg.adresse?.adresselinje2, organisasjonsinfoFraEreg.adresse?.adresselinje3),
                    postNr = postnummer,
                    postSted = poststed,
                    adresseType = adresse.type,
                    kommunenummer = adresse.kommunenummer,
                    gyldighetsperiode = adresse.gyldighetsperiode?.let { Gyldighetsperiode(it.fom, it.tom) },
                )

            samhandlerinfoFraTss.copy(
                navn = organisasjonsinfoFraEreg.navn,
                adresser = listOf(samhandlerAdresse),
            )
        }
    }

    @Cacheable("samhandlerBehandling", cacheManager = "shortCache")
    fun hentSamhandlerForBehandling(behandlingId: BehandlingId): SamhandlerInfo {
        val institusjonsadresse = institusjonsinfoRepository.findByBehandlingId(behandlingId.id)

        return institusjonsadresse?.tilSamhandlerInfo() ?: hentSamhandlerInfo(behandlingId)
    }

    private fun hentSamhandlerInfo(behandlingId: BehandlingId): SamhandlerInfo {
        val institusjon =
            behandlingHentOgPersisterService
                .hent(behandlingId.id)
                .fagsak.institusjon

        if (institusjon == null) {
            throw FunksjonellFeil("Behandlingen hører ikke til en institusjon")
        }
        return hentSamhandler(institusjon.orgNummer)
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

    fun Institusjonsinfo.tilSamhandlerInfo(): SamhandlerInfo =
        SamhandlerInfo(
            tssEksternId = this.institusjon.tssEksternId!!,
            navn = this.navn,
            adresser =
                listOf(
                    SamhandlerAdresse(
                        adresselinjer = listOfNotNull(this.adresselinje1, this.adresselinje2, this.adresselinje3),
                        postNr = this.postnummer,
                        postSted = this.poststed,
                        adresseType = this.type,
                        kommunenummer = this.kommunenummer,
                        gyldighetsperiode = this.gyldighetsperiode.fom?.let { Gyldighetsperiode(it, this.gyldighetsperiode.tom) },
                    ),
                ),
            orgNummer = this.institusjon.orgNummer,
        )
}
