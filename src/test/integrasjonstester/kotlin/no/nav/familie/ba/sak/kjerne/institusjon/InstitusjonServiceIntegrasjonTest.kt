package no.nav.familie.ba.sak.kjerne.institusjon

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.ekstern.restDomene.InstitusjonDto
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class InstitusjonServiceIntegrasjonTest(
    @Autowired private val institusjonsinfoRepository: InstitusjonsinfoRepository,
    @Autowired private val institusjonService: InstitusjonService,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {
    @Test
    fun `skal lagre og finne Institusjonsinfo for behandlingId`() {
        // Given
        val orgNr = UUID.randomUUID().toString()
        val tssEksternId = UUID.randomUUID().toString()
        institusjonService.hentEllerOpprettInstitusjon(orgNr, tssEksternId)
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = tilfeldigPerson().aktør.aktivFødselsnummer(),
                institusjon = InstitusjonDto(orgNr, tssEksternId),
                fagsakType = FagsakType.INSTITUSJON,
            )
        val behandling = lagBehandlingUtenId(fagsak).also { behandlingRepository.saveAndFlush(it) }

        // When
        institusjonService.lagreInstitusjonsinfo(behandling.id)

        // Then
        institusjonsinfoRepository.findByBehandlingId(behandling.id)!!.also {
            assertThat(it.behandlingId).isEqualTo(behandling.id)
            assertThat(it.institusjon.orgNummer).isEqualTo(orgNr)
            assertThat(it.institusjon.tssEksternId).isEqualTo(tssEksternId)
            assertThat(it.navn).isEqualTo("Testinstitusjon")
            assertThat(it.type).isEqualTo("Forretningsadresse")
            assertThat(it.kommunenummer).isEqualTo("0301")
            assertThat(it.poststed).isEqualTo("Oslo")
            assertThat(it.postnummer).isEqualTo("0661")
            assertThat(it.gyldighetsperiode.fom).isEqualTo(LocalDate.of(2020, 1, 1))
            assertThat(it.gyldighetsperiode.tom).isNull()
            assertThat(it.adresselinje1).isEqualTo("Fyrstikkalleen 1")
            assertThat(it.adresselinje2).isNull()
            assertThat(it.adresselinje3).isEqualTo("Avd BAKS")
        }
    }

    @Test
    fun `Hvis oppdatering av institusjonsinfo, så skal den gamle slettes og ny opprettes`() {
        // Given
        val orgNr = UUID.randomUUID().toString()
        val tssEksternId = UUID.randomUUID().toString()
        institusjonService.hentEllerOpprettInstitusjon(orgNr, tssEksternId)
        val fagsak =
            fagsakService.hentEllerOpprettFagsakForPersonIdent(
                fødselsnummer = tilfeldigPerson().aktør.aktivFødselsnummer(),
                institusjon = InstitusjonDto(orgNr, tssEksternId),
                fagsakType = FagsakType.INSTITUSJON,
            )
        val behandling = lagBehandlingUtenId(fagsak).also { behandlingRepository.saveAndFlush(it) }

        // When
        institusjonService.lagreInstitusjonsinfo(behandling.id)
        val institusjonsinfo = institusjonsinfoRepository.findByBehandlingId(behandling.id)
        institusjonService.lagreInstitusjonsinfo(behandling.id)
        val enAnnenInstitusjonsinfo = institusjonsinfoRepository.findByBehandlingId(behandling.id)

        // Then
        assertThat(institusjonsinfo?.id).isNotNull()
        assertThat(enAnnenInstitusjonsinfo?.id).isNotNull()
        assertThat(institusjonsinfo!!.id).isNotEqualTo(enAnnenInstitusjonsinfo!!.id)
    }
}
