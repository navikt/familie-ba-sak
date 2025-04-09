package no.nav.familie.ba.sak.ekstern

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.behandling.EksternBehandlingRelasjonRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandlingsresultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.EksternBehandlingRelasjon
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.klage.KlageService
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.TilgangService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class EksternKlageControllerTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val eksternBehandlingRelasjonRepository: EksternBehandlingRelasjonRepository,
    @Autowired private val klageService: KlageService,
) : AbstractSpringIntegrationTest() {
    private val tilgangService = mockk<TilgangService>()
    private val eksternKlageController =
        EksternKlageController(
            klageService = klageService,
            tilgangService = tilgangService,
        )

    @BeforeEach
    fun setup() {
        mockkObject(SikkerhetContext)
        every { tilgangService.validerTilgangTilHandlingOgFagsak(any(), any(), any(), any()) } just runs
        every { SikkerhetContext.kallKommerFraKlage() } returns true
    }

    @AfterEach
    fun cleanup() {
        unmockkObject(SikkerhetContext)
    }

    @Nested
    inner class OpprettRevurderingKlage {
        @Test
        fun `skal opprette revurdering klage og lage en ekstern behandling relasjon mellom revurderingen og klagen`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))

            behandlingRepository.save(
                lagBehandlingUtenId(
                    fagsak = fagsak,
                    resultat = Behandlingsresultat.INNVILGET,
                    status = BehandlingStatus.AVSLUTTET,
                ),
            )

            val klagebehandlingId = UUID.randomUUID()

            // Act
            val opprettRevurderingResponse =
                eksternKlageController.opprettRevurderingKlage(
                    fagsakId = fagsak.id,
                    klagebehandlingId = klagebehandlingId,
                )

            // Assert
            assertThat(opprettRevurderingResponse.data?.opprettetBehandling).isEqualTo(true)
            assertThat(opprettRevurderingResponse.data?.opprettet).isNotNull()
            assertThat(opprettRevurderingResponse.data?.ikkeOpprettet).isNull()
            val revurderingBehandlingId =
                opprettRevurderingResponse.data!!
                    .opprettet!!
                    .eksternBehandlingId
                    .toLong()
            val eksternBehandlingRelasjon = eksternBehandlingRelasjonRepository.findAllByInternBehandlingId(revurderingBehandlingId)
            assertThat(eksternBehandlingRelasjon).hasSize(1)
            assertThat(eksternBehandlingRelasjon[0].id).isNotNull()
            assertThat(eksternBehandlingRelasjon[0].internBehandlingId).isEqualTo(revurderingBehandlingId)
            assertThat(eksternBehandlingRelasjon[0].eksternBehandlingId).isEqualTo(klagebehandlingId.toString())
            assertThat(eksternBehandlingRelasjon[0].eksternBehandlingFagsystem).isEqualTo(EksternBehandlingRelasjon.Fagsystem.KLAGE)
            assertThat(eksternBehandlingRelasjon[0].opprettetTidspunkt).isNotNull()
        }
    }
}
