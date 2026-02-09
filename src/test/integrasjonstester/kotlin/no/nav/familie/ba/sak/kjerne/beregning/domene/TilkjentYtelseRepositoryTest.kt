package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.datagenerator.lagBehandlingUtenId
import no.nav.familie.ba.sak.datagenerator.lagFagsakUtenId
import no.nav.familie.ba.sak.datagenerator.lagTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.YtelsetypeBA
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagMinimalUtbetalingsoppdragString
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.utbetalingsoppdrag.lagUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakRepository
import no.nav.familie.ba.sak.kjerne.personident.AktørIdRepository
import no.nav.familie.felles.utbetalingsgenerator.domain.Opphør
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

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
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id, ytelseTypeBa = YtelsetypeBA.UTVIDET_BARNETRYGD),
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
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))

            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    utbetalingsoppdrag = lagMinimalUtbetalingsoppdragString(behandlingId = behandling.id),
                ),
            )

            // Act
            val harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd =
                tilkjentYtelseRepository.harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd(fagsak.id)

            // Assert
            assertThat(harFagsakTattIBrukNyKlassekodeForUtvidetBarnetrygd).isFalse()
        }
    }

    @Nested
    inner class FindByFagsak {
        @Test
        fun `skal returnere alle tilkjente ytelser tilknyttet fagsak`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))
            val behandling = behandlingRepository.save(lagBehandlingUtenId(fagsak = fagsak))
            val migreringsdatoPluss1Mnd = LocalDate.of(2025, 1, 1)

            tilkjentYtelseRepository.save(
                lagTilkjentYtelse(
                    behandling = behandling,
                    utbetalingsoppdrag =
                        jsonMapper.writeValueAsString(
                            lagUtbetalingsoppdrag(
                                listOf(
                                    lagUtbetalingsperiode(
                                        behandlingId = behandling.id,
                                        periodeId = 1,
                                        forrigePeriodeId = null,
                                        ytelseTypeBa = YtelsetypeBA.ORDINÆR_BARNETRYGD,
                                        opphør = Opphør(migreringsdatoPluss1Mnd),
                                    ),
                                ),
                            ),
                        ),
                ),
            )

            // Act
            val tilkjenteYtelser =
                tilkjentYtelseRepository.findByFagsak(
                    fagsakId = fagsak.id,
                )

            // Assert
            assertThat(tilkjenteYtelser).hasSize(1)
        }

        @Test
        fun `skal returnere tom liste dersom det ikke er sendt opphør fra migreringsdato pluss 1 mnd tidligere`() {
            // Arrange
            val aktør = aktørIdRepository.save(randomAktør())
            val fagsak = fagsakRepository.save(lagFagsakUtenId(aktør = aktør))

            // Act
            val tilkjenteYtelser =
                tilkjentYtelseRepository.findByFagsak(
                    fagsakId = fagsak.id,
                )

            // Assert
            assertThat(tilkjenteYtelser).isEmpty()
        }
    }
}
