package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.defaultFagsak
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagPerson
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class ØkonomiUtilsTest {

    @Test
    fun `skal separere småbarnstillegg`() {
        val person = tilfeldigPerson()
        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2023-10"),
                    årMnd("2025-01"),
                    SMÅBARNSTILLEGG,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2027-10"),
                    årMnd("2028-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                )
            )
        )

        assertEquals(2, kjederBehandling.size)
    }

    @Test
    fun `skal siste før første berørte andel i kjede`() {
        val person = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2
                )
            )
        )
        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2022-10"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(årMnd("2019-04"), sisteBeståendePerKjede[KjedeId(person.aktør, ORDINÆR_BARNETRYGD)]?.stønadFom)
        assertEquals(årMnd("2022-01"), sisteBeståendePerKjede[KjedeId(person2.aktør, ORDINÆR_BARNETRYGD)]?.stønadFom)
    }

    @Test
    fun `skal sette null som siste bestående for person med endring i første`() {
        val person = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                )
            )
        )
        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2018-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(null, sisteBeståendePerKjede[KjedeId(person.aktør, ORDINÆR_BARNETRYGD)])
    }

    @Test
    fun `skal sette null som siste bestående for ny person`() {
        val person = tilfeldigPerson()
        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2023-10"),
                    årMnd("2025-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2027-10"),
                    årMnd("2028-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = emptyMap(), oppdaterteKjeder = kjederBehandling)
        assertEquals(null, sisteBeståendePerKjede[KjedeId(person.aktør, ORDINÆR_BARNETRYGD)]?.stønadFom)
    }

    @Test
    fun `skal settes null som siste bestående ved fullt opphørt person`() {
        val person = tilfeldigPerson()

        val kjederBehandling = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2023-10"),
                    årMnd("2025-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2027-10"),
                    årMnd("2028-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling, oppdaterteKjeder = emptyMap())
        assertEquals(null, sisteBeståendePerKjede[KjedeId(person.aktør, ORDINÆR_BARNETRYGD)]?.stønadFom)
    }

    @Test
    fun `skal velge rette perioder til opphør og oppbygging fra endring`() {
        val person = tilfeldigPerson()

        val datoSomSkalOppdateres = "2022-01"
        val datoSomErOppdatert = "2021-01"

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd(datoSomSkalOppdateres),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2025-04"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør
                )
            )
        )
        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd(datoSomErOppdatert),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2025-04"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person,
                    aktør = person.aktør
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2
            )
        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling2,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )
        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling1,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )

        assertEquals(1, andelerTilOpprettelse.size)
        assertEquals(2, andelerTilOpprettelse.first().size)
        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(årMnd(datoSomSkalOppdateres), andelerTilOpphørMedDato.first().second)
    }

    @Test
    fun `skal opphøre først barn helt og innvilge nytt barn når første barn ikke er innvilget i andre behandling`() {
        val førsteBarn = tilfeldigPerson()
        val andreBarn = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = førsteBarn,
                    aktør = førsteBarn.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2020-02"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1345,
                    person = førsteBarn,
                    aktør = førsteBarn.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2023-02"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1654,
                    person = førsteBarn,
                    aktør = førsteBarn.aktør
                )
            )
        )
        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2020-04"),
                    årMnd("2021-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = andreBarn,
                    aktør = andreBarn.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2021-02"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = andreBarn,
                    aktør = andreBarn.aktør
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2025-04"),
                    årMnd("2026-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = andreBarn,
                    aktør = andreBarn.aktør
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2
            )
        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling2,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )
        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling1,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )

        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(YearMonth.of(2023, 2), andelerTilOpphørMedDato.first().first.stønadFom)
        assertEquals(YearMonth.of(2019, 4), andelerTilOpphørMedDato.first().second)
        assertEquals(3, andelerTilOpprettelse.first().size)
        assertEquals(YearMonth.of(2020, 4), andelerTilOpprettelse.first().first().stønadFom)
        assertEquals(YearMonth.of(2021, 1), andelerTilOpprettelse.first().first().stønadTom)
        assertEquals(YearMonth.of(2025, 4), andelerTilOpprettelse.first().last().stønadFom)
        assertEquals(YearMonth.of(2026, 1), andelerTilOpprettelse.first().last().stønadTom)
    }

    @Test
    fun `skal gjøre separate endringer på ordinær og småbarnstillegg`() {
        val person = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                )
            )
        )
        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2019-06"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2022-01"),
                    årMnd("2023-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    SMÅBARNSTILLEGG,
                    1054,
                    person = person
                )
            )
        )

        val sisteBeståendePerKjede =
            sisteBeståendeAndelPerKjede(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2
            )
        val andelerTilOpprettelse =
            andelerTilOpprettelse(
                oppdaterteKjeder = kjederBehandling2,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )
        val andelerTilOpphørMedDato =
            andelerTilOpphørMedDato(
                forrigeKjeder = kjederBehandling1,
                sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede
            )

        assertEquals(2, andelerTilOpprettelse.size)
        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(årMnd("2019-04"), andelerTilOpphørMedDato.first().second)
    }

    @Test
    fun `skal oppdatere offset på bestående behandler i oppdaterte kjeder`() {
        val person = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    periodeIdOffset = 1,
                    forrigeperiodeIdOffset = 0,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    periodeIdOffset = 3,
                    forrigeperiodeIdOffset = 2,
                    person = person2
                )
            )
        )
        val kjederBehandling2 = kjedeinndelteAndeler(
            listOf(
                lagAndelTilkjentYtelse(
                    årMnd("2019-04"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person
                ),
                lagAndelTilkjentYtelse(
                    årMnd("2019-12"),
                    årMnd("2020-01"),
                    ORDINÆR_BARNETRYGD,
                    1054,
                    person = person2
                )
            )
        )

        val oppdaterte =
            oppdaterBeståendeAndelerMedOffset(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2
            )

        assertEquals(1, oppdaterte.getValue(KjedeId(person.aktør, ORDINÆR_BARNETRYGD)).first().periodeOffset)
        assertEquals(0, oppdaterte.getValue(KjedeId(person.aktør, ORDINÆR_BARNETRYGD)).first().forrigePeriodeOffset)
        assertEquals(null, oppdaterte.getValue(KjedeId(person2.aktør, ORDINÆR_BARNETRYGD)).first().periodeOffset)
        assertEquals(null, oppdaterte.getValue(KjedeId(person2.aktør, ORDINÆR_BARNETRYGD)).first().forrigePeriodeOffset)
    }

    @Test
    fun `Skal returnere offset-oppdatering for andel som er lik som i forrige behandling og nå har feil offset`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val fagsak = defaultFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak)
        val forrigeBehandling = lagBehandling(fagsak = fagsak)

        val andelerIForrigeBehandling = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 1,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 0,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            )

        )

        val andelerIDenneBehandlingen = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2022, 9),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 4,
                forrigeperiodeIdOffset = 1,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 4,
                forrigeperiodeIdOffset = 0,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 10),
                tom = YearMonth.of(2028, 8),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn2.aktør,
                beløp = 1676,
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = null,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2028, 9),
                tom = YearMonth.of(2040, 8),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn2.aktør,
                beløp = 1054,
                periodeIdOffset = 3,
                forrigeperiodeIdOffset = 2,
                behandling = behandling
            )
        )

        val oppdateringer = ØkonomiUtils.finnBeståendeAndelerMedOffsetSomMåOppdateres(
            forrigeKjeder = kjedeinndelteAndeler(andelerIForrigeBehandling),
            oppdaterteKjeder = kjedeinndelteAndeler(andelerIDenneBehandlingen)
        )

        assertEquals(1, oppdateringer.size)

        val oppdatering = oppdateringer.single()

        assertEquals(4, oppdatering.beståendeAndelSomSkalHaOppdatertOffset.periodeOffset)
        assertEquals(0, oppdatering.beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset)
        assertEquals(behandling.id, oppdatering.beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId)

        assertEquals(0, oppdatering.periodeOffset)
        assertEquals(null, oppdatering.forrigePeriodeOffset)
        assertEquals(forrigeBehandling.id, oppdatering.kildeBehandlingId)
    }

    @Test
    fun `Skal returnere offset-oppdatering for andel som er lik som i forrige behandling og allerede har riktig offset`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val fagsak = defaultFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak)
        val forrigeBehandling = lagBehandling(fagsak = fagsak)

        val andelerIForrigeBehandling = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 1,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling,
                kildeBehandlingId = forrigeBehandling.id
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 0,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling,
                kildeBehandlingId = forrigeBehandling.id
            )

        )

        val andelerIDenneBehandlingen = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2022, 9),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 1,
                forrigeperiodeIdOffset = null,
                behandling = behandling,
                kildeBehandlingId = forrigeBehandling.id
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 0,
                forrigeperiodeIdOffset = null,
                behandling = behandling,
                kildeBehandlingId = forrigeBehandling.id
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 10),
                tom = YearMonth.of(2028, 8),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn2.aktør,
                beløp = 1676,
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = null,
                behandling = behandling,
                kildeBehandlingId = behandling.id
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2028, 9),
                tom = YearMonth.of(2040, 8),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn2.aktør,
                beløp = 1054,
                periodeIdOffset = 3,
                forrigeperiodeIdOffset = 2,
                behandling = behandling,
                kildeBehandlingId = behandling.id
            )
        )

        val oppdateringer = ØkonomiUtils.finnBeståendeAndelerMedOffsetSomMåOppdateres(
            forrigeKjeder = kjedeinndelteAndeler(andelerIForrigeBehandling),
            oppdaterteKjeder = kjedeinndelteAndeler(andelerIDenneBehandlingen)
        )

        assertTrue(oppdateringer.isEmpty())
    }

    @Test
    fun `Skal returnere offset-oppdatering for de andelene som er like som i forrige behandling og nå har feil offset`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val fagsak = defaultFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak)
        val forrigeBehandling = lagBehandling(fagsak = fagsak)

        val andelerIForrigeBehandling = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 1,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 0,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            )

        )

        val andelerIDenneBehandlingen = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 4,
                forrigeperiodeIdOffset = 1,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = 0,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 10),
                tom = YearMonth.of(2028, 8),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn2.aktør,
                beløp = 1676,
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = null,
                behandling = behandling
            )
        )

        val oppdateringer = ØkonomiUtils.finnBeståendeAndelerMedOffsetSomMåOppdateres(
            forrigeKjeder = kjedeinndelteAndeler(andelerIForrigeBehandling),
            oppdaterteKjeder = kjedeinndelteAndeler(andelerIDenneBehandlingen)
        )

        assertEquals(2, oppdateringer.size)

        val oppdateringUtvidet = oppdateringer.find { it.beståendeAndelSomSkalHaOppdatertOffset.erUtvidet() }
        val oppdateringOrdinær = oppdateringer.find { !it.beståendeAndelSomSkalHaOppdatertOffset.erUtvidet() }

        assertEquals(4, oppdateringUtvidet!!.beståendeAndelSomSkalHaOppdatertOffset.periodeOffset)
        assertEquals(1, oppdateringUtvidet.beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset)
        assertEquals(behandling.id, oppdateringUtvidet.beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId)

        assertEquals(1, oppdateringUtvidet.periodeOffset)
        assertEquals(null, oppdateringUtvidet.forrigePeriodeOffset)
        assertEquals(forrigeBehandling.id, oppdateringUtvidet.kildeBehandlingId)

        assertEquals(2, oppdateringOrdinær!!.beståendeAndelSomSkalHaOppdatertOffset.periodeOffset)
        assertEquals(0, oppdateringOrdinær.beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset)
        assertEquals(behandling.id, oppdateringOrdinær.beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId)

        assertEquals(0, oppdateringOrdinær.periodeOffset)
        assertEquals(null, oppdateringOrdinær.forrigePeriodeOffset)
        assertEquals(forrigeBehandling.id, oppdateringOrdinær.kildeBehandlingId)
    }

    @Test
    fun `Skal returnere offset-oppdatering for andeler som er like som forrige behandling, men er null nå`() {
        val søker = lagPerson(type = PersonType.SØKER)
        val barn1 = lagPerson(type = PersonType.BARN)
        val barn2 = lagPerson(type = PersonType.BARN)
        val fagsak = defaultFagsak(aktør = søker.aktør)
        val behandling = lagBehandling(fagsak = fagsak)
        val forrigeBehandling = lagBehandling(fagsak = fagsak)

        val andelerIForrigeBehandling = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 2,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2022, 10),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 3,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2028, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 0,
                forrigeperiodeIdOffset = null,
                behandling = forrigeBehandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2028, 5),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = 1,
                forrigeperiodeIdOffset = 0,
                behandling = forrigeBehandling
            )
        )

        val andelerIDenneBehandlingen = listOf(
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.UTVIDET_BARNETRYGD,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = null,
                forrigeperiodeIdOffset = null,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2022, 8),
                ytelseType = YtelseType.SMÅBARNSTILLEGG,
                aktør = søker.aktør,
                beløp = 1054,
                periodeIdOffset = 4,
                forrigeperiodeIdOffset = 3,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2022, 4),
                tom = YearMonth.of(2028, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = null,
                forrigeperiodeIdOffset = null,
                behandling = behandling
            ),
            lagAndelTilkjentYtelse(
                fom = YearMonth.of(2028, 5),
                tom = YearMonth.of(2033, 3),
                ytelseType = YtelseType.ORDINÆR_BARNETRYGD,
                aktør = barn1.aktør,
                beløp = 1054,
                periodeIdOffset = null,
                forrigeperiodeIdOffset = null,
                behandling = behandling
            )
        )

        val oppdateringer = ØkonomiUtils.finnBeståendeAndelerMedOffsetSomMåOppdateres(
            forrigeKjeder = kjedeinndelteAndeler(andelerIForrigeBehandling),
            oppdaterteKjeder = kjedeinndelteAndeler(andelerIDenneBehandlingen)
        )

        assertEquals(3, oppdateringer.size)

        val oppdateringUtvidet = oppdateringer.find { it.beståendeAndelSomSkalHaOppdatertOffset.erUtvidet() }
        val oppdateringerOrdinær = oppdateringer.filter { !it.beståendeAndelSomSkalHaOppdatertOffset.erUtvidet() }
            .sortedBy { it.beståendeAndelSomSkalHaOppdatertOffset.stønadFom }

        assertEquals(null, oppdateringUtvidet!!.beståendeAndelSomSkalHaOppdatertOffset.periodeOffset)
        assertEquals(null, oppdateringUtvidet.beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset)
        assertEquals(behandling.id, oppdateringUtvidet.beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId)

        assertEquals(2, oppdateringUtvidet.periodeOffset)
        assertEquals(null, oppdateringUtvidet.forrigePeriodeOffset)
        assertEquals(forrigeBehandling.id, oppdateringUtvidet.kildeBehandlingId)

        assertEquals(2, oppdateringerOrdinær.size)

        val førsteOrdinæreOppdatering = oppdateringerOrdinær.first()
        val andreOrdinæreOppdatering = oppdateringerOrdinær.last()

        assertEquals(null, førsteOrdinæreOppdatering.beståendeAndelSomSkalHaOppdatertOffset.periodeOffset)
        assertEquals(null, førsteOrdinæreOppdatering.beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset)
        assertEquals(behandling.id, førsteOrdinæreOppdatering.beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId)

        assertEquals(0, førsteOrdinæreOppdatering.periodeOffset)
        assertEquals(null, førsteOrdinæreOppdatering.forrigePeriodeOffset)
        assertEquals(forrigeBehandling.id, førsteOrdinæreOppdatering.kildeBehandlingId)

        assertEquals(null, andreOrdinæreOppdatering.beståendeAndelSomSkalHaOppdatertOffset.periodeOffset)
        assertEquals(null, andreOrdinæreOppdatering.beståendeAndelSomSkalHaOppdatertOffset.forrigePeriodeOffset)
        assertEquals(behandling.id, andreOrdinæreOppdatering.beståendeAndelSomSkalHaOppdatertOffset.kildeBehandlingId)

        assertEquals(1, andreOrdinæreOppdatering.periodeOffset)
        assertEquals(0, andreOrdinæreOppdatering.forrigePeriodeOffset)
        assertEquals(forrigeBehandling.id, andreOrdinæreOppdatering.kildeBehandlingId)
    }
}
