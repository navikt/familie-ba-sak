package no.nav.familie.ba.sak.kjerne.etterbetalingkorrigering

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.randomAktørId
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.Fagsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.hamcrest.CoreMatchers.`is` as Is

class EtterbetalingKorrigeringRepositoryTest(
    @Autowired private val aktørIdRepository: AktørIdRepository,
    @Autowired private val fagsakRepository: FagsakRepository,
    @Autowired private val behandlingRepository: BehandlingRepository,
    @Autowired private val etterbetalingKorrigeringRepository: EtterbetalingKorrigeringRepository
) : AbstractSpringIntegrationTest() {

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal returnere null dersom det ikke eksisterer en aktiv etterbetaling korrigering på behandling`() {
        val behandling = opprettBehandling()

        val inaktivEtterbetalingKorrigering = EtterbetalingKorrigering(
            id = 10000001,
            årsak = EtterbetalingKorrigeringÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
            begrunnelse = "Test på inaktiv korrigering",
            beløp = 1000,
            behandling = behandling,
            aktiv = false
        )

        etterbetalingKorrigeringRepository.saveAndFlush(inaktivEtterbetalingKorrigering)

        val ikkeEksisterendeEtterbetalingKorrigering =
            etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandling.id)

        assertThat(ikkeEksisterendeEtterbetalingKorrigering, Is(nullValue()))
    }

    @Test
    fun `finnAktivtKorrigeringPåBehandling skal returnere aktiv etterbetaling korrigering på behandling dersom det finnes`() {
        val behandling = opprettBehandling()

        val aktivEtterbetalingKorrigering = EtterbetalingKorrigering(
            id = 10000002,
            årsak = EtterbetalingKorrigeringÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
            begrunnelse = "Test på aktiv korrigering",
            beløp = 1000,
            behandling = behandling,
            aktiv = true
        )

        etterbetalingKorrigeringRepository.saveAndFlush(aktivEtterbetalingKorrigering)

        val eksisterendeEtterbetalingKorrigering =
            etterbetalingKorrigeringRepository.finnAktivtKorrigeringPåBehandling(behandling.id)!!

        assertThat(eksisterendeEtterbetalingKorrigering.begrunnelse, Is("Test på aktiv korrigering"))
        assertThat(eksisterendeEtterbetalingKorrigering.beløp, Is(1000))
    }

    @Test
    fun `finnAlleKorrigeringPåBehandling skal returnere alle etterbetalingkorrigering på behandling`() {
        val behandling = opprettBehandling()

        val aktivEtterbetalingKorrigering = EtterbetalingKorrigering(
            id = 10000003,
            årsak = EtterbetalingKorrigeringÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
            begrunnelse = "1",
            beløp = 1000,
            behandling = behandling,
            aktiv = true
        )

        val inaktivEtterbetalingKorrigering = EtterbetalingKorrigering(
            id = 10000004,
            årsak = EtterbetalingKorrigeringÅrsak.REFUSJON_FRA_ANDRE_MYNDIGHETER,
            begrunnelse = "2",
            beløp = 1000,
            behandling = behandling,
            aktiv = false
        )

        etterbetalingKorrigeringRepository.saveAndFlush(aktivEtterbetalingKorrigering)
        etterbetalingKorrigeringRepository.saveAndFlush(inaktivEtterbetalingKorrigering)

        val eksisterendeEtterbetalingKorrigering =
            etterbetalingKorrigeringRepository.hentAlleKorrigeringPåBehandling(behandling.id)

        assertThat(eksisterendeEtterbetalingKorrigering.size, Is(2))
        assertThat(eksisterendeEtterbetalingKorrigering.map { it.begrunnelse }, containsInAnyOrder("1", "2"))
    }

    private fun opprettBehandling(): Behandling {
        val søker = aktørIdRepository.save(randomAktørId())
        val fagsak = fagsakRepository.save(Fagsak(aktør = søker))

        return behandlingRepository.save(lagBehandling(fagsak))
    }
}
