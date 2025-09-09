package no.nav.familie.ba.sak.kjerne.steg

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.KodeverkService
import no.nav.familie.ba.sak.integrasjoner.organisasjon.OrganisasjonService
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjon
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonService
import no.nav.familie.ba.sak.kjerne.institusjon.Institusjonsinfo
import no.nav.familie.ba.sak.kjerne.institusjon.InstitusjonsinfoRepository
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import org.springframework.stereotype.Service

@Service
class RegistrerInstitusjon(
    val institusjonService: InstitusjonService,
    val loggService: LoggService,
    val behandlingHentOgPersisterService: BehandlingHentOgPersisterService,
    val fagsakService: FagsakService,
    val organisasjonService: OrganisasjonService,
    val institusjonsinfoRepository: InstitusjonsinfoRepository,
    val kodeverkService: KodeverkService,
) : BehandlingSteg<Institusjon> {
    override fun utførStegOgAngiNeste(
        behandling: Behandling,
        institusjon: Institusjon,
    ): StegType {
        val institusjon =
            institusjonService
                .hentEllerOpprettInstitusjon(
                    orgNummer = institusjon.orgNummer,
                    tssEksternId = institusjon.tssEksternId,
                ).apply {
                    val fagsak = behandling.fagsak
                    fagsak.institusjon = this
                    fagsakService.lagre(fagsak)
                }
        loggService.opprettRegistrerInstitusjonLogg(
            behandling,
        )

        organisasjonService.hentOrganisasjon(institusjon.orgNummer).apply {
            val institusjonsadresse = institusjonsinfoRepository.findByBehandlingId(behandling.id)
            if (institusjonsadresse == null) {
                val organisasjonAdresse = this.adresse ?: throw FunksjonellFeil("Fant ikke adresse for institusjonen ${this.organisasjonsnummer}.")
                Institusjonsinfo(
                    institusjon = institusjon,
                    behandlingId = behandling.id,
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
            } else {
            }
        }

        return hentNesteStegForNormalFlyt(behandling = behandlingHentOgPersisterService.hent(behandlingId = behandling.id))
    }

    override fun stegType(): StegType = StegType.REGISTRERE_INSTITUSJON
}
