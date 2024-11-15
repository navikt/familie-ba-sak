package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagFagsak
import no.nav.familie.ba.sak.common.lagTilkjentYtelse
import no.nav.familie.ba.sak.common.randomAktør
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TilkjentYtelseRepositoryTest(
    @Autowired
    private val aktørIdRepository: AktørIdRepository,
    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    @Autowired
    private val fagsakRepository: FagsakRepository,
    @Autowired
    private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {
    @Nested
    inner class HarFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygdTest {
        @Test
        fun `skal returnere true hvis klassifisering er BAUTV-OP`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandling(fagsak = fagsak))

            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    utbetalingsoppdrag = "\"klassifisering\":\"BAUTV-OP\"",
                ),
            )

            // Act
            val harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd =
                tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(fagsak.id)

            // Assert
            assertThat(harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd).isTrue()
        }

        @Test
        fun `skal returnere false hvis klassifisering ikke er BAUTV-OP`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsak(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandling(fagsak = fagsak))

            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    utbetalingsoppdrag = "\"klassifisering\":\"BATR\"",
                ),
            )

            // Act
            val harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd =
                tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(fagsak.id)

            // Assert
            assertThat(harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd).isFalse()
        }
    }
}
