package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.beregning.domene.YtelseType.SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.common.dato
import no.nav.familie.ba.sak.common.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.andelerTilOpphørMedDato
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.andelerTilOpprettelse
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.kjedeinndelteAndeler
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.oppdaterBeståendeAndelerMedOffset
import no.nav.familie.ba.sak.økonomi.ØkonomiUtils.sisteBeståendeAndelPerKjede
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ØkonomiUtilsTest {

    @Test
    fun `skal separere småbarnstillegg`() {
        val person = tilfeldigPerson()
        val kjederBehandling = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2023-10-01",
                                       "2025-01-01",
                                       SMÅBARNSTILLEGG,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2027-10-01",
                                       "2028-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))

        assertEquals(2, kjederBehandling.size)
    }

    @Test
    fun `skal siste før første berørte andel i kjede`() {
        val person = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person2),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person2)))
        val kjederBehandling2 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2022-10-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person2),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person2)))

        val sisteBeståendePerKjede =
                sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(dato("2019-04-01"), sisteBeståendePerKjede[person.personIdent.ident]?.stønadFom)
        assertEquals(dato("2022-01-01"), sisteBeståendePerKjede[person2.personIdent.ident]?.stønadFom)
    }


    @Test
    fun `skal sette null som siste bestående for person med endring i første`() {
        val person = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))
        val kjederBehandling2 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2018-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))

        val sisteBeståendePerKjede =
                sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(null, sisteBeståendePerKjede[person.personIdent.ident])
    }


    @Test
    fun `skal sette null som siste bestående for ny person`() {
        val person = tilfeldigPerson()
        val kjederBehandling = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2023-10-01",
                                       "2025-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2027-10-01",
                                       "2028-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))

        val sisteBeståendePerKjede = sisteBeståendeAndelPerKjede(forrigeKjeder = emptyMap(), oppdaterteKjeder = kjederBehandling)
        assertEquals(null, sisteBeståendePerKjede[person.personIdent.ident]?.stønadFom)
    }

    @Test
    fun `skal settes null som siste bestående ved fullt opphørt person`() {
        val person = tilfeldigPerson()

        val kjederBehandling = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2023-10-01",
                                       "2025-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2027-10-01",
                                       "2028-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))

        val sisteBeståendePerKjede = sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling, oppdaterteKjeder = emptyMap())
        assertEquals(null, sisteBeståendePerKjede[person.personIdent.ident]?.stønadFom)
    }

    @Test
    fun `skal velge rette perioder til opphør og oppbygging fra endring`() {
        val person = tilfeldigPerson()
        val datoSomSkalOppdateres = "2022-01-01"
        val datoSomErOppdatert = "2021-01-01"

        val kjederBehandling1 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(datoSomSkalOppdateres,
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2025-04-01",
                                       "2026-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))
        val kjederBehandling2 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse(datoSomErOppdatert,
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2025-04-01",
                                       "2026-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))

        val sisteBeståendePerKjede =
                sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        val andelerTilOpprettelse =
                andelerTilOpprettelse(oppdaterteKjeder = kjederBehandling2,
                                      sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede)
        val andelerTilOpphørMedDato =
                andelerTilOpphørMedDato(forrigeKjeder = kjederBehandling1,
                                        oppdaterteKjeder = kjederBehandling2,
                                        sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede)

        assertEquals(1, andelerTilOpprettelse.size)
        assertEquals(2, andelerTilOpprettelse.first().size)
        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(dato(datoSomSkalOppdateres), andelerTilOpphørMedDato.first().second)
    }

    @Test
    fun `skal gjøre separate endringer på ordinær og småbarnstillegg`() {
        val person = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person)))
        val kjederBehandling2 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2019-06-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       SMÅBARNSTILLEGG,
                                       1054,
                                       person = person)))

        val sisteBeståendePerKjede =
                sisteBeståendeAndelPerKjede(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        val andelerTilOpprettelse =
                andelerTilOpprettelse(oppdaterteKjeder = kjederBehandling2,
                                      sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede)
        val andelerTilOpphørMedDato =
                andelerTilOpphørMedDato(forrigeKjeder = kjederBehandling1,
                                        oppdaterteKjeder = kjederBehandling2,
                                        sisteBeståendeAndelIHverKjede = sisteBeståendePerKjede)

        assertEquals(2, andelerTilOpprettelse.size)
        assertEquals(1, andelerTilOpphørMedDato.size)
        assertEquals(dato("2019-04-01"), andelerTilOpphørMedDato.first().second)
    }

    @Test
    fun `skal oppdatere offset på bestående behandler i oppdaterte kjeder`() {
        val person = tilfeldigPerson()
        val person2 = tilfeldigPerson()

        val kjederBehandling1 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       periodeIdOffset = 1,
                                       forrigeperiodeIdOffset = 0,
                                       person = person),
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       periodeIdOffset = 3,
                                       forrigeperiodeIdOffset = 2,
                                       person = person2)))
        val kjederBehandling2 = kjedeinndelteAndeler(listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2019-12-12",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person2)))

        val oppdaterte =
                oppdaterBeståendeAndelerMedOffset(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)

        assertEquals(1, oppdaterte.getValue(person.personIdent.ident).first().periodeOffset)
        assertEquals(0, oppdaterte.getValue(person.personIdent.ident).first().forrigePeriodeOffset)
        assertEquals(null, oppdaterte.getValue(person2.personIdent.ident).first().periodeOffset)
        assertEquals(null, oppdaterte.getValue(person2.personIdent.ident).first().forrigePeriodeOffset)
    }
}