package no.nav.familie.ba.sak.økonomi

import no.nav.familie.ba.sak.beregning.domene.YtelseType.*
import no.nav.familie.ba.sak.common.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ØkonomiUtilsTest {

    @Test
    fun `skal sette dirty kjede fom første berørte andel i kjede sin fom-dato`() {
        val person = tilfeldigPerson()

        val andelerBehandling1 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2023-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person))
        val andelerBehandling2 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2022-01-01",
                                       "2022-10-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person))

        val kjederBehandling1 = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandling1)
        val kjederBehandling2 = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandling2)

        val dirtyKjedeFomOversikt = ØkonomiUtils.dirtyKjedeFomOversikt(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(1, dirtyKjedeFomOversikt.size)
        assertEquals(dato("2022-01-01"), dirtyKjedeFomOversikt[person.personIdent.ident])
    }


    @Test
    fun `skal kun legge til kjede som er dirty`() {
        val personMedEndretAndel = tilfeldigPerson()
        val personMedUendretAndel = tilfeldigPerson()

        val andelerBehandling1 = listOf(
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = personMedEndretAndel),
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = personMedUendretAndel))
        val andelerBehandling2= listOf(
                lagAndelTilkjentYtelse("2019-10-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = personMedEndretAndel),
                lagAndelTilkjentYtelse("2019-04-01",
                                       "2020-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = personMedUendretAndel))

        val kjederBehandling1 = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandling1)
        val kjederBehandling2 = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandling2)

        val dirtyKjedeFomOversikt = ØkonomiUtils.dirtyKjedeFomOversikt(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(1, dirtyKjedeFomOversikt.size)
        assertEquals(dato("2019-04-01"), dirtyKjedeFomOversikt[personMedEndretAndel.personIdent.ident])
    }

    @Test
    fun `skal velge første berørte dato blant både opphørte og opprettede`() {
        val person = tilfeldigPerson()

        val andelerBehandlingTidligstFom = listOf(
                lagAndelTilkjentYtelse("2020-01-01",
                                       "2025-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person))
        val andelerBehandlingSenestFom= listOf(
                lagAndelTilkjentYtelse("2023-10-01",
                                       "2025-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person))

        val kjederBehandling1 = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandlingTidligstFom)
        val kjederBehandling2 = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandlingSenestFom)

        val dirtyKjedeFomOversikt1 = ØkonomiUtils.dirtyKjedeFomOversikt(forrigeKjeder = kjederBehandling1, oppdaterteKjeder = kjederBehandling2)
        assertEquals(dato("2020-01-01"), dirtyKjedeFomOversikt1[person.personIdent.ident])

        val dirtyKjedeFomOversikt2 = ØkonomiUtils.dirtyKjedeFomOversikt(forrigeKjeder = kjederBehandling2, oppdaterteKjeder = kjederBehandling1)
        assertEquals(dato("2020-01-01"), dirtyKjedeFomOversikt2[person.personIdent.ident])
    }
    @Test
    fun `skal settes dirty fra fom på første periode ved ny person`() {
        val person = tilfeldigPerson()

        val andelerBehandling= listOf(
                lagAndelTilkjentYtelse("2023-10-01",
                                       "2025-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2027-10-01",
                                       "2028-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person))

        val kjederBehandling = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandling)

        val dirtyKjedeFomOversikt = ØkonomiUtils.dirtyKjedeFomOversikt(forrigeKjeder = emptyMap(), oppdaterteKjeder = kjederBehandling)
        assertEquals(dato("2023-10-01"), dirtyKjedeFomOversikt[person.personIdent.ident])
    }

    @Test
    fun `skal settes dirty fra fom på første periode ved opphørt person`() {
        val person = tilfeldigPerson()

        val andelerBehandling= listOf(
                lagAndelTilkjentYtelse("2023-10-01",
                                       "2025-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person),
                lagAndelTilkjentYtelse("2027-10-01",
                                       "2028-01-01",
                                       ORDINÆR_BARNETRYGD,
                                       1054,
                                       person = person))

        val kjederBehandling = ØkonomiUtils.kjedeinndelteAndeler(andelerBehandling)

        val dirtyKjedeFomOversikt = ØkonomiUtils.dirtyKjedeFomOversikt(forrigeKjeder = kjederBehandling, oppdaterteKjeder = emptyMap())
        assertEquals(dato("2023-10-01"), dirtyKjedeFomOversikt[person.personIdent.ident])
    }
}