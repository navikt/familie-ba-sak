package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
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
    val organisasjonService: OrganisasjonService,
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
    fun hentSamhandlerFraTssOgEreg(orgNummer: String): SamhandlerInfo {
        val samhandlerinfoFraTss = samhandlerKlient.hentSamhandler(orgNummer)
        val organisasjonsinfoFraEreg = organisasjonService.hentOrganisasjon(orgNummer)

        return organisasjonsinfoFraEreg.adresse?.let { adresse ->
            val poststed = kodeverkService.hentPoststed(adresse.postnummer) ?: ""
            val samhandlerAdresse =
                SamhandlerAdresse(
                    adresselinjer = listOfNotNull(adresse.adresselinje1, adresse.adresselinje2, adresse.adresselinje3),
                    postNr = adresse.postnummer,
                    postSted = poststed,
                    adresseType = adresse.type,
                    kommunenummer = adresse.kommunenummer,
                    gyldighetsperiode = adresse.gyldighetsperiode?.let { Gyldighetsperiode(it.fom, it.tom) },
                )
            samhandlerinfoFraTss.copy(navn = organisasjonsinfoFraEreg.navn, adresser = listOf(samhandlerAdresse))
        } ?: samhandlerinfoFraTss
    }

    @Cacheable("samhandlerBehandling", cacheManager = "shortCache")
    fun hentSamhandlerForBehandling(behandlingId: BehandlingId): SamhandlerInfo =
        institusjonsinfoRepository.findByBehandlingId(behandlingId.id)?.tilSamhandlerInfo()
            ?: hentSamhandlerInfo(behandlingId)

    private fun hentSamhandlerInfo(behandlingId: BehandlingId): SamhandlerInfo {
        val institusjon =
            behandlingHentOgPersisterService.hent(behandlingId.id).fagsak.institusjon
                ?: throw FunksjonellFeil("Behandlingen hører ikke til en institusjon")
        return hentSamhandlerFraTssOgEreg(institusjon.orgNummer)
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

    fun lagreInstitusjonsinfo(
        behandlingId: Long,
    ) {
        val behandling = behandlingHentOgPersisterService.hent(behandlingId)
        if (behandling.status.erLåstForVidereRedigering()) {
            return
        }
        val institusjon = behandling.fagsak.institusjon ?: throw Feil("Fagsaken mangler institusjon")

        organisasjonService.hentOrganisasjon(institusjon.orgNummer).apply {
            val institusjonsinfo = institusjonsinfoRepository.findByBehandlingId(behandlingId)
            if (institusjonsinfo != null) {
                institusjonsinfoRepository.delete(institusjonsinfo)
            }

            val organisasjonAdresse = this.adresse ?: throw FunksjonellFeil("Fant ikke adresse for institusjonen ${this.organisasjonsnummer}.")
            Institusjonsinfo(
                institusjon = institusjon,
                behandlingId = behandlingId,
                type = organisasjonAdresse.type,
                navn = this.navn,
                adresselinje1 = organisasjonAdresse.adresselinje1,
                adresselinje2 = organisasjonAdresse.adresselinje2,
                adresselinje3 = organisasjonAdresse.adresselinje3,
                postnummer = organisasjonAdresse.postnummer,
                poststed = kodeverkService.hentPoststed(organisasjonAdresse.postnummer) ?: "",
                kommunenummer = organisasjonAdresse.kommunenummer,
                gyldighetsperiode =
                    DatoIntervallEntitet(
                        fom = organisasjonAdresse.gyldighetsperiode?.fom!!,
                        tom = this.adresse?.gyldighetsperiode?.tom,
                    ),
            ).also {
                institusjonsinfoRepository.save(it)
            }
        }
    }
}
