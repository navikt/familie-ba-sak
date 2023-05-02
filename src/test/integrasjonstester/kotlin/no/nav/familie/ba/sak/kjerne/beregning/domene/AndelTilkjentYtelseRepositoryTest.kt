package no.nav.familie.ba.sak.kjerne.beregning.domene

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.config.AbstractSpringIntegrationTest
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.personident.Aktør
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
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
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) : AbstractSpringIntegrationTest() {

    @Nested
    inner class hentSisteAndelPerIdent {

        val søker = tilfeldigPerson()
        val barn1 = tilfeldigPerson()
        val barn2 = tilfeldigPerson()

        val fagsak = fagsakService.hentEllerOpprettFagsakForPersonIdent(søker.aktør.aktivFødselsnummer())
        val aktørSøker = personidentService.hentOgLagreAktør(søker.aktør.aktivFødselsnummer(), true)
        val aktørBarn1 = personidentService.hentOgLagreAktør(barn1.aktør.aktivFødselsnummer(), true)
        val aktørBarn2 = personidentService.hentOgLagreAktør(barn2.aktør.aktivFødselsnummer(), true)

        val førsteBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))
        val andreBehandling = behandlingService.lagreNyOgDeaktiverGammelBehandling(lagBehandling(fagsak))

        @Test
        fun `ingen andeler`() {
            assertThat(hentSisteAndelPerIdent()).hasSize(0)
        }

        // TODO test uten utbetalingsoppdrag

        @Test
        fun `2 ulike personer med samme type`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn1,
                        person = barn1,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        aktør = aktørBarn2,
                        person = barn2,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 5),
                        offset = 1
                    )
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(2)
            with(sisteAndelPerIdent[Pair(YtelseType.SMÅBARNSTILLEGG, barn1.aktør.aktivFødselsnummer())]!!) {
                assertThat(getPeriodeOffset()).isEqualTo(0L)
                assertThat(getForrigePeriodeOffset()).isNull()
                assertThat(getFom()).isEqualTo(LocalDate.of(2020, 1, 1))
                assertThat(getTom()).isEqualTo(LocalDate.of(2020, 1, 31))
            }
            with(sisteAndelPerIdent[Pair(YtelseType.SMÅBARNSTILLEGG, barn2.aktør.aktivFødselsnummer())]!!) {
                assertThat(getPeriodeOffset()).isEqualTo(1L)
                assertThat(getForrigePeriodeOffset()).isNull()
                assertThat(getFom()).isEqualTo(LocalDate.of(2020, 3, 1))
                assertThat(getTom()).isEqualTo(LocalDate.of(2020, 5, 31))
            }
        }

        @Test
        fun `førstegångsbehandling med flere andeler per person`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                        forrigeOffset = 0
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                        fom = YearMonth.of(2020, 3),
                        tom = YearMonth.of(2020, 3),
                        offset = 2,
                        forrigeOffset = null
                    )
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(2)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[Pair(YtelseType.SMÅBARNSTILLEGG, fødselsnummer)]!!) {
                assertThat(getPeriodeOffset()).isEqualTo(1L)
                assertThat(getForrigePeriodeOffset()).isEqualTo(0L)
                assertThat(getFom()).isEqualTo(LocalDate.of(2020, 3, 1))
                assertThat(getTom()).isEqualTo(LocalDate.of(2020, 5, 31))
            }
            with(sisteAndelPerIdent[Pair(YtelseType.UTVIDET_BARNETRYGD, fødselsnummer)]!!) {
                assertThat(getPeriodeOffset()).isEqualTo(2L)
                assertThat(getForrigePeriodeOffset()).isNull()
                assertThat(getFom()).isEqualTo(LocalDate.of(2020, 3, 1))
                assertThat(getTom()).isEqualTo(LocalDate.of(2020, 5, 31))
            }
        }

        @Test
        fun `siste andelen kommer fra revurderingen`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 2),
                        offset = 0
                    )
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            with(lagInitiellTilkjentYtelse(andreBehandling)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 1,
                        forrigeOffset = 0
                    )
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(1)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[Pair(YtelseType.SMÅBARNSTILLEGG, fødselsnummer)]!!) {
                assertThat(getPeriodeOffset()).isEqualTo(1L)
                assertThat(getForrigePeriodeOffset()).isEqualTo(0L)
                assertThat(getFom()).isEqualTo(LocalDate.of(2020, 1, 1))
                assertThat(getTom()).isEqualTo(LocalDate.of(2020, 3, 31))
                assertThat(getKildeBehandlingId()).isEqualTo(andreBehandling.id)
            }
        }

        @Test
        fun `en revurdering opphører en andel, sånn at siste andelen finnes i en tidligere behandling`() {
            with(lagInitiellTilkjentYtelse(førsteBehandling)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 0
                    ),
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 4),
                        tom = YearMonth.of(2020, 5),
                        offset = 1,
                        forrigeOffset = 0
                    )
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }
            with(lagInitiellTilkjentYtelse(andreBehandling)) {
                val andeler = listOf(
                    lagAndel(
                        tilkjentYtelse = this,
                        fom = YearMonth.of(2020, 1),
                        tom = YearMonth.of(2020, 3),
                        offset = 0
                    )
                )
                andelerTilkjentYtelse.addAll(andeler)
                tilkjentYtelseRepository.saveAndFlush(this)
            }

            val sisteAndelPerIdent = hentSisteAndelPerIdent()
            assertThat(sisteAndelPerIdent).hasSize(1)
            val fødselsnummer = aktørSøker.aktivFødselsnummer()
            with(sisteAndelPerIdent[Pair(YtelseType.SMÅBARNSTILLEGG, fødselsnummer)]!!) {
                assertThat(getPeriodeOffset()).isEqualTo(1L)
                assertThat(getForrigePeriodeOffset()).isEqualTo(0L)
                assertThat(getFom()).isEqualTo(LocalDate.of(2020, 4, 1))
                assertThat(getTom()).isEqualTo(LocalDate.of(2020, 5, 31))
                assertThat(getKildeBehandlingId()).isEqualTo(førsteBehandling.id)
            }
        }

        private fun hentSisteAndelPerIdent(): Map<Pair<YtelseType, String>, SisteAndelTilkjentYtelse> {
            return andelTilkjentYtelseRepository.hentSisteAndelPerIdent(fagsak.id)
                .groupBy { Pair(it.getType(), it.getIdent()) }
                .mapValues { it.value.single() }
        }

        fun lagAndel(
            tilkjentYtelse: TilkjentYtelse,
            ytelseType: YtelseType = YtelseType.SMÅBARNSTILLEGG,
            person: Person? = null,
            aktør: Aktør? = null,
            fom: YearMonth,
            tom: YearMonth,
            offset: Long,
            forrigeOffset: Long? = null
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
                forrigeperiodeIdOffset = forrigeOffset
            )
    }
}
