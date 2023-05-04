package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.integrasjoner.økonomi.AndelTilkjentYtelseForUtbetalingsoppdrag
import no.nav.familie.ba.sak.integrasjoner.økonomi.IdentOgYtelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.kjerne.beregning.BeregningService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class AndelTilkjentYtelseRepositoryTest(
    @Autowired
    private val fagsakService: FagsakService,

    @Autowired
    private val personidentService: PersonidentService,

    @Autowired
    private val behandlingService: BehandlingService,

    @Autowired
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,

    @Autowired
    private val beregningService: BeregningService,

    @Autowired
    private val behandlingRepository: BehandlingRepository,
) : AbstractSpringIntegrationTest() {

    @Nested
    inner class HentSisteAndelPerIdent {

        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        val aktørSøker = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val aktørBarn1 = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)
        val aktørBarn2 = personidentService.hentOgLagreAktør(barn2.aktør.aktivFødselsnummer(), true)

        lateinit var førsteBehandling: Behandling

        @BeforeEach
        fun setUp() {
            førsteBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        }

        @Test
        fun `ingen andeler`() {
            assertThat(hentSisteAndelPerIdent()).hasSize(0)
        }

        @Test
        fun `uten utbetalingsoppdrag`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = null)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).isEmpty()
        }

        @Test
        fun `behandling er ikke avsluttet`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).isEmpty()
        }

        @Test
        fun `2 ulike personer med samme type`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn2,
                        person = barn2,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(2)
            with(sisteAndelPerIdent[IdentOgYtelse(barn1.aktør.aktivFødselsnummer(), YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(0L)
                assertThat(forrigePeriodeOffset).isNull()
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 1))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 2))
            }
            with(sisteAndelPerIdent[IdentOgYtelse(barn2.aktør.aktivFødselsnummer(), YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isNull()
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 5))
            }
        }

        @Test
        fun `førstegångsbehandling med flere andeler per person`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                        forrigeOffset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 3),
                        offset = 2,
                        forrigeOffset = null,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(2)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isEqualTo(0L)
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 5))
            }
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.UTVIDET_BARNETRYGD)]!!) {
                assertThat(periodeOffset).isEqualTo(2L)
                assertThat(forrigePeriodeOffset).isNull()
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 3))
            }
        }

        @Test
        fun `siste andelen kommer fra revurderingen`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val revurdering = lagRevurdering()
            with(lagInitiellTilkjentYtelse(revurdering, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 1,
                        forrigeOffset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(revurdering)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(1)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isEqualTo(0L)
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 1))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 3))
                assertThat(kildeBehandlingId).isEqualTo(revurdering.id)
            }
        }

        @Test
        fun `en revurdering opphører en andel, sånn at siste andelen finnes i en tidligere behandling`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 0,
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 4),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                        forrigeOffset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(førsteBehandling)
            val revurdering = lagRevurdering()
            with(lagInitiellTilkjentYtelse(revurdering, utbetalingsoppdrag = "utbetalingsoppdrag")) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 0,
                    ),
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            avsluttOgLagreBehandling(revurdering)
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(1)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[IdentOgYtelse(fødselsnummer, YtelseType.SMÅBARNSTILLEGG)]!!) {
                assertThat(periodeOffset).isEqualTo(1L)
                assertThat(forrigePeriodeOffset).isEqualTo(0L)
                assertThat(stønadFom).isEqualTo(YearMonth.of(2020, 4))
                assertThat(stønadTom).isEqualTo(YearMonth.of(2020, 5))
                assertThat(kildeBehandlingId).isEqualTo(førsteBehandling.id)
            }
        }

        fun hentSisteAndelPerIdent(): Map<IdentOgYtelse, AndelTilkjentYtelseForUtbetalingsoppdrag> {
            return beregningService.hentSisteAndelPerIdent(fagsak.id)
        }

        fun lagAndel(
            tilkjentYtelse: TilkjentYtelse,
            ytelseType: YtelseType = YtelseType.SMÅBARNSTILLEGG,
            person: Person? = null,
            aktør: Aktør? = null,
            fom: YearMonth,
            tom: YearMonth,
            offset: Long,
            forrigeOffset: Long? = null,
        ): AndelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                fom,
                tom,
                ytelseType,
                1345,
                tilkjentYtelse.behandling,
                person = person ?: søker,
                aktør = aktør ?: aktørSøker,
                tilkjentYtelse = tilkjentYtelse,
                periodeIdOffset = offset,
                forrigeperiodeIdOffset = forrigeOffset,
            )

        private fun lagRevurdering() = behandlingService.lagreNyOgDeaktiverGammelBehandling(
            lagBehandling(fagsak, behandlingType = BehandlingType.REVURDERING),
        )
    }

    private fun avsluttOgLagreBehandling(behandling: Behandling) {
        behandling.status = BehandlingStatus.AVSLUTTET
        behandling.leggTilBehandlingStegTilstand(StegType.BEHANDLING_AVSLUTTET)
        behandlingRepository.save(behandling)
    }
}
