package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.tilfeldigPerson
import no.nav.familie.ba.sak.datagenerator.tilfeldigSøker
import no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler.Filtreringsregel.Identifikator
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Evaluering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.erOppfylt
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class FiltreringsregelEvaluatorForSøknadTest {
    private val filtreringsregelEvaluator = FiltreringsregelEvaluator()

    @Nested
    inner class NårSøknadenOppfyllerAlleRegler {
        @Test
        fun `skal gi oppfylt`() {
            // Act
            val evalueringer = evaluer()

            // Assert
            assertThat(evalueringer.erOppfylt()).isTrue
        }
    }

    @Nested
    inner class NårSøkerEllerBarnHarDNummer {
        @Test
        fun `skal avvise når søker har d-nummer`() {
            // Act
            val evalueringer = evaluer(søker = søker(ident = "44086226621"))

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_D_NUMMER)
        }

        @Test
        fun `skal avvise når barn har d-nummer`() {
            // Act
            val evalueringer = evaluer(barna = listOf(barn(ident = "41111777001")))

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_HAR_IKKE_D_NUMMER)
        }
    }

    @Nested
    inner class NårSøkerEllerBarnHarUgyldigFødselsnummer {
        @Test
        fun `skal avvise når søker har ugyldig fødselsnummer`() {
            // Act
            val evalueringer = evaluer(søker = søker(ident = "04086200000"))

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_GYLDIG_FNR)
        }

        @Test
        fun `skal avvise når barn har ugyldig fødselsnummer`() {
            // Act
            val evalueringer = evaluer(barna = listOf(barn(ident = "21111700000")))

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_GYLDIG_FNR)
        }
    }

    @Nested
    inner class NårSøkerEllerBarnIkkeLever {
        @Test
        fun `skal avvise når søker ikke lever`() {
            // Act
            val evalueringer = evaluer(søkerLever = false)

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_LEVER)
        }

        @Test
        fun `skal avvise når barn ikke lever`() {
            // Act
            val evalueringer = evaluer(barnaLever = false)

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_LEVER)
        }
    }

    @Nested
    inner class NårSøkerIkkeErMyndigEllerHarVerge {
        @Test
        fun `skal avvise når søker er under 18 år`() {
            // Act
            val evalueringer = evaluer(søker = søker(fødselsdato = LocalDate.now().minusYears(17)))

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_ER_OVER_18_ÅR)
        }

        @Test
        fun `skal avvise når søker har verge`() {
            // Act
            val evalueringer = evaluer(søkerHarVerge = true)

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_VERGE)
        }
    }

    @Nested
    inner class NårSøkerMottarUtvidetEllerEøsBarnetrygd {
        @Test
        fun `skal avvise når søker mottar løpende utvidet barnetrygd`() {
            // Act
            val evalueringer = evaluer(søkerMottarLøpendeUtvidet = true)

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET)
        }

        @Test
        fun `skal avvise når søker mottar løpende EØS-barnetrygd`() {
            // Act
            val evalueringer = evaluer(søkerMottarEøsBarnetrygd = true)

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD)
        }
    }

    @Nested
    inner class NårDetUtbetalesBarnetrygdForBarnetTilAnnenMottaker {
        @Test
        fun `skal avvise når det utbetales barnetrygd for barnet til annen mottaker i inneværende måned`() {
            // Act
            val evalueringer = evaluer(utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned = true)

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED)
        }
    }

    private fun evaluer(
        søker: Person = søker(),
        barna: List<Person> = listOf(barn()),
        søkerLever: Boolean = true,
        barnaLever: Boolean = true,
        søkerHarVerge: Boolean = false,
        søkerMottarLøpendeUtvidet: Boolean = false,
        søkerMottarEøsBarnetrygd: Boolean = false,
        utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned: Boolean = false,
    ): List<Evaluering> =
        filtreringsregelEvaluator.evaluerFiltreringsregler(
            FILTRERINGSREGLER_SØKNAD,
            FiltreringsreglerFaktaSøknad(
                søker = søker,
                søkerMottarLøpendeUtvidet = søkerMottarLøpendeUtvidet,
                søkerOppfyllerVilkårForUtvidetBarnetrygd = false,
                søkerMottarEøsBarnetrygd = søkerMottarEøsBarnetrygd,
                barnaSomSkalVurderes = barna,
                søkerLever = søkerLever,
                barnaLever = barnaLever,
                søkerHarVerge = søkerHarVerge,
                utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned = utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned,
                søkerHarIkkeLøpendeUtbetalingOgHarAldriHattUtbetaling = false,
            ),
        )

    private fun søker(
        ident: String = "04086226621",
        fødselsdato: LocalDate = LocalDate.now().minusYears(20),
    ): Person = tilfeldigSøker(fødselsdato = fødselsdato, aktør = lagAktør(ident))

    private fun barn(
        ident: String = "21111777001",
        fødselsdato: LocalDate = LocalDate.now(),
    ): Person = tilfeldigPerson(fødselsdato = fødselsdato, aktør = lagAktør(ident))

    private fun assertFørsteIkkeOppfylteRegel(
        evalueringer: List<Evaluering>,
        identifikator: Identifikator,
    ) {
        assertThat(evalueringer.erOppfylt()).isFalse
        assertThat(evalueringer.first { it.resultat == Resultat.IKKE_OPPFYLT }.identifikator).isEqualTo(identifikator.name)
    }
}
