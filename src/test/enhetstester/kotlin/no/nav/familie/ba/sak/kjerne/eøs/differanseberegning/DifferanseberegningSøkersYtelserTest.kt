package no.nav.familie.ba.sak.kjerne.eøs.differanseberegning

import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagInitiellTilkjentYtelse
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.ORDINÆR_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.SMÅBARNSTILLEGG
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType.UTVIDET_BARNETRYGD
import no.nav.familie.ba.sak.kjerne.eøs.assertEqualsUnordered
import no.nav.familie.ba.sak.kjerne.eøs.util.barn
import no.nav.familie.ba.sak.kjerne.eøs.util.der
import no.nav.familie.ba.sak.kjerne.eøs.util.fom
import no.nav.familie.ba.sak.kjerne.eøs.util.født
import no.nav.familie.ba.sak.kjerne.eøs.util.har
import no.nav.familie.ba.sak.kjerne.eøs.util.i
import no.nav.familie.ba.sak.kjerne.eøs.util.minus
import no.nav.familie.ba.sak.kjerne.eøs.util.og
import no.nav.familie.ba.sak.kjerne.eøs.util.tom
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.tidslinje.util.aug
import no.nav.familie.ba.sak.kjerne.tidslinje.util.des
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jan
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jul
import no.nav.familie.ba.sak.kjerne.tidslinje.util.jun
import no.nav.familie.ba.sak.kjerne.tidslinje.util.mai
import org.junit.jupiter.api.Test

class DifferanseberegningSøkersYtelserTest {

    private val Int.kr get() = this
    private val Int.PLN get() = this * 2

    @Test
    fun `skal håndtere to barn og utvidet barnetrygd og småbarnstillegg, der det ene barnet har underskudd etter differanseberegning`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.jan(2017)
        val barn2 = barn født 15.jun(2020)
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling) der
            (søker har 1054.kr i UTVIDET_BARNETRYGD fom jun(2018) tom jul(2024)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom aug(2018) tom des(2019)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom jul(2020) tom mai(2023)) og
            (barn1 har 1054.kr og 1250.PLN i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022)) og
            (barn1 har 1054.kr og 1325.PLN i ORDINÆR_BARNETRYGD fom aug(2022) tom jul(2024)) og
            (barn2 har 1054.kr i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038))

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(listOf(barn1, barn2))

        val forventet = lagInitiellTilkjentYtelse(behandling) der
            (barn1 har 1054.kr og 1250.PLN i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022)) og
            (barn1 har 1054.kr og 1325.PLN i ORDINÆR_BARNETRYGD fom aug(2022) tom jul(2024)) og
            (barn2 har 1054.kr i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038)) og
            (søker har 1054.kr i UTVIDET_BARNETRYGD fom jun(2018) tom jul(2019)) og
            (søker har 1054.kr minus 1054.kr i UTVIDET_BARNETRYGD fom aug(2019) tom jun(2020)) og
            (søker har 1054.kr minus 527.kr i UTVIDET_BARNETRYGD fom jul(2020) tom jul(2024)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom aug(2018) tom jul(2019)) og
            (søker har 660.kr minus 392.kr i SMÅBARNSTILLEGG fom aug(2019) tom des(2019)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom jul(2020) tom mai(2023))

        assertEqualsUnordered(forventet.andelerTilkjentYtelse, nyeAndeler)
    }

    @Test
    fun `differanseberegnet ordinær barnetrygd uten at søker har ytelser, skal gi uendrete andeler for barne`() {
        val barn1 = barn født 13.jan(2017)
        val barn2 = barn født 15.jun(2020)
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling) der
            (barn1 har 1054.kr og 1250.PLN i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022)) og
            (barn1 har 1054.kr og 1325.PLN i ORDINÆR_BARNETRYGD fom aug(2022) tom jul(2024)) og
            (barn2 har 1054.kr i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038))

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(listOf(barn1, barn2))

        val forventet = lagInitiellTilkjentYtelse(behandling) der
            (barn1 har 1054.kr og 1250.PLN i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2022)) og
            (barn1 har 1054.kr og 1325.PLN i ORDINÆR_BARNETRYGD fom aug(2022) tom jul(2024)) og
            (barn2 har 1054.kr i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038))

        assertEqualsUnordered(forventet.andelerTilkjentYtelse, nyeAndeler)
    }

    @Test
    fun `Ingen differranseberegning skal gi uendrete andeler`() {
        val søker = tilfeldigPerson(personType = PersonType.SØKER)
        val barn1 = barn født 13.jan(2017)
        val barn2 = barn født 15.jun(2020)
        val behandling = lagBehandling()

        val tilkjentYtelse = lagInitiellTilkjentYtelse(behandling) der
            (søker har 1054.kr i UTVIDET_BARNETRYGD fom jun(2018) tom jul(2024)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom aug(2018) tom des(2019)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom jul(2020) tom mai(2023)) og
            (barn1 har 1054.kr i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2024)) og
            (barn2 har 1054.kr i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038))

        val nyeAndeler =
            tilkjentYtelse.andelerTilkjentYtelse.differanseberegnSøkersYtelser(listOf(barn1, barn2))

        val forventet = lagInitiellTilkjentYtelse(behandling) der
            (søker har 1054.kr i UTVIDET_BARNETRYGD fom jun(2018) tom jul(2024)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom aug(2018) tom des(2019)) og
            (søker har 660.kr i SMÅBARNSTILLEGG fom jul(2020) tom mai(2023)) og
            (barn1 har 1054.kr i ORDINÆR_BARNETRYGD fom aug(2019) tom jul(2024)) og
            (barn2 har 1054.kr i ORDINÆR_BARNETRYGD fom jul(2020) tom mai(2038))

        assertEqualsUnordered(forventet.andelerTilkjentYtelse, nyeAndeler)
    }

    @Test
    fun `Tom tilkjent ytelse og ingen barn skal ikke gi feil`() {
        val tilkjentYtelse = lagInitiellTilkjentYtelse()

        val nyeAndeler = tilkjentYtelse.andelerTilkjentYtelse
            .differanseberegnSøkersYtelser(emptyList())

        assertEqualsUnordered(emptyList(), nyeAndeler)
    }
}
