package no.nav.familie.ba.sak.integrasjoner.økonomi

import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.common.årMnd
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.finnBeståendeAndelerMedOppdatertOffset
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.integrasjoner.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SMÅBARNSTILLEGG
import org.junit.jupiter.api.Assertions.assertEquals
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
        assertEquals(årMnd("2019-04"), sisteBeståendePerKjede[person.aktør.aktivFødselsnummer()]?.stønadFom)
        assertEquals(årMnd("2022-01"), sisteBeståendePerKjede[person2.aktør.aktivFødselsnummer()]?.stønadFom)
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
        assertEquals(null, sisteBeståendePerKjede[person.aktør.aktørId])
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
        assertEquals(null, sisteBeståendePerKjede[person.aktør.aktørId]?.stønadFom)
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
        assertEquals(null, sisteBeståendePerKjede[person.aktør.aktørId]?.stønadFom)
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
            finnBeståendeAndelerMedOppdatertOffset(
                forrigeKjeder = kjederBehandling1,
                oppdaterteKjeder = kjederBehandling2
            )
        oppdaterte.forEach { it.oppdater() }

        assertEquals(1, kjederBehandling2.getValue(person.aktør.aktivFødselsnummer()).first().periodeOffset)
        assertEquals(0, kjederBehandling2.getValue(person.aktør.aktivFødselsnummer()).first().forrigePeriodeOffset)
        assertEquals(null, kjederBehandling2.getValue(person2.aktør.aktivFødselsnummer()).first().periodeOffset)
        assertEquals(null, kjederBehandling2.getValue(person2.aktør.aktivFødselsnummer()).first().forrigePeriodeOffset)
    }
}
