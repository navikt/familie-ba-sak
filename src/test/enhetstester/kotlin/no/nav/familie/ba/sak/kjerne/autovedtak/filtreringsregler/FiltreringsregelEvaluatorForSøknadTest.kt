package no.nav.familie.ba.sak.kjerne.autovedtak.filtreringsregler

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagFiltreringsreglerFaktaSøknad
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
    private val featureToggleService = mockk<FeatureToggleService>()
    private val filtreringsregelEvaluator = FiltreringsregelEvaluator(featureToggleService)

    init {
        every { featureToggleService.isEnabled(FeatureToggle.VURDER_ALLE_FILTRERINGSREGLER) } returns false
    }

    @Nested
    inner class NårSøknadenOppfyllerAlleRegler {
        @Test
        fun `skal gi oppfylt`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad()

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(FILTRERINGSREGLER_SØKNAD, fakta)

            // Assert
            assertThat(evalueringer.erOppfylt()).isTrue
        }
    }

    @Nested
    inner class NårSøkerEllerBarnHarDNummer {
        @Test
        fun `skal avvise når søker har d-nummer`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søker = søker(ident = "44086226621"))

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_D_NUMMER)
        }

        @Test
        fun `skal avvise når barn har d-nummer`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(barnaSomSkalVurderes = listOf(barn(ident = "41111777001")))

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_HAR_IKKE_D_NUMMER)
        }
    }

    @Nested
    inner class NårSøkerEllerBarnHarAdressebeskyttelseGradering6Eller19 {
        @Test
        fun `skal avvise når søker har adressebeskyttelse gradering 6 eller 19`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerHarAdressebeskyttelseGradering6Eller19 = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19)
        }

        @Test
        fun `skal avvise når barn har adressebeskyttelse gradering 6 eller 19`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(barnHarAdressebeskyttelseGradering6Eller19 = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_HAR_IKKE_ADRESSEBESKYTTELSE_GRADERING_6_ELLER_19)
        }
    }

    @Test
    fun `skal avvise når ikke alle søknadsbarna har foreldre barn-relasjon til søker`() {
        // Arrange
        val fakta = lagFiltreringsreglerFaktaSøknad(søkerOgBarnHarForelderBarnRelasjon = false)

        // Act
        val evalueringer =
            filtreringsregelEvaluator.evaluerFiltreringsregler(
                FILTRERINGSREGLER_SØKNAD,
                fakta,
            )

        // Assert
        assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_OG_BARN_HAR_FORELDER_BARN_RELASJON)
    }

    @Nested
    inner class NårSøkerEllerBarnHarUgyldigFødselsnummer {
        @Test
        fun `skal avvise når søker har ugyldig fødselsnummer`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søker = søker(ident = "04086200000"))

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_GYLDIG_FNR)
        }

        @Test
        fun `skal avvise når barn har ugyldig fødselsnummer`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(barnaSomSkalVurderes = listOf(barn(ident = "21111700000")))

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_GYLDIG_FNR)
        }
    }

    @Nested
    inner class NårSøkerEllerBarnIkkeLever {
        @Test
        fun `skal avvise når søker ikke lever`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerLever = false)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_LEVER)
        }

        @Test
        fun `skal avvise når barn ikke lever`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(barnaLever = false)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_LEVER)
        }
    }

    @Nested
    inner class NårSøkerIkkeErMyndigEllerHarVerge {
        @Test
        fun `skal avvise når søker er under 18 år`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søker = søker(fødselsdato = LocalDate.now().minusYears(17)))

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_ER_OVER_18_ÅR)
        }

        @Test
        fun `skal avvise når søker har verge`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerHarVerge = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_VERGE)
        }
    }

    @Nested
    inner class NårSøkerMottarUtvidetEllerEøsBarnetrygd {
        @Test
        fun `skal avvise når søker mottar løpende utvidet barnetrygd`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerMottarLøpendeUtvidet = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_MOTTAR_IKKE_LØPENDE_UTVIDET)
        }

        @Test
        fun `skal avvise når søker mottar løpende EØS-barnetrygd`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerMottarEøsBarnetrygd = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_LØPENDE_EØS_BARNETRYGD)
        }
    }

    @Nested
    inner class NårDetUtbetalesBarnetrygdForBarnetTilAnnenMottaker {
        @Test
        fun `skal avvise når det utbetales barnetrygd for barnet til annen mottaker i inneværende måned`() {
            // Arrange
            val fakta =
                lagFiltreringsreglerFaktaSøknad(
                    utbetalesBarnetrygdForBarnetTilAnnenMottakerIInneværendeMåned = true,
                )

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.UTBETALES_IKKE_BARNETRYGD_FOR_BARNET_TIL_ANNEN_MOTTAKER_INNEVÆRENDE_MÅNED)
        }
    }

    @Nested
    inner class NårSøknadenInneholderFaktaSomKreverManuellBehandling {
        @Test
        fun `skal avvise når søker har krysset på EØS-spørsmål i søknaden`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerHarKryssetPåEøsSpørsmålISøknaden = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_KRYSSET_PÅ_EØS_SPØRSMÅL_I_SØKNADEN)
        }

        @Test
        fun `skal avvise når søker har krysset for delt bosted i søknaden`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerHarKryssetForDeltBostedISøknaden = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_DELT_BOSTED_I_SØKNADEN)
        }

        @Test
        fun `skal avvise når søker har krysset for at barn er i fosterhjem eller beredskapshjem i søknaden`() {
            // Arrange
            val fakta =
                lagFiltreringsreglerFaktaSøknad(
                    søkerHarKryssetForFosterhjemEllerBeredskapshjemISøknaden = true,
                )

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_KRYSSET_FOR_FOSTERHJEM_ELLER_BEREDSKAPSHJEM_I_SØKNADEN)
        }

        @Test
        fun `skal avvise når søknaden inneholder vedlegg`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søknadenInneholderVedlegg = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKNADEN_INNEHOLDER_IKKE_VEDLEGG)
        }
    }

    @Nested
    inner class NårSøkerEllerBarnIkkeHarAktivNorskBostedsadresse {
        @Test
        fun `skal avvise når søker ikke har aktiv norsk bostedsadresse`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerHarAktivNorskBostedsadresse = false)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_AKTIV_NORSK_BOSTEDSADRESSE)
        }

        @Test
        fun `skal avvise når barn ikke har aktiv norsk bostedsadresse`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(barnHarAktivNorskBostedsadresse = false)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_HAR_AKTIV_NORSK_BOSTEDSADRESSE)
        }
    }

    @Nested
    inner class SøkerHarIkkeOppfyltUtvidetVilkår {
        @Test
        fun `skal avvise når søker ikke oppfyller vilkår for utvidet barnetrygd`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerOppfyllerVilkårForUtvidetBarnetrygd = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_HAR_IKKE_OPPFYLT_UTVIDET_VILKÅR)
        }
    }

    @Nested
    inner class NårSøkerEllerBarnErUkrainskStatsborger {
        @Test
        fun `skal avvise når søker er ukrainsk statsborger`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(søkerHarUkrainskStatsborgerskap = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.SØKER_ER_IKKE_UKRAINSK_STATSBORGER)
        }

        @Test
        fun `skal avvise når barn er ukrainsk statsborger`() {
            // Arrange
            val fakta = lagFiltreringsreglerFaktaSøknad(barnHarUkrainskStatsborgerskap = true)

            // Act
            val evalueringer =
                filtreringsregelEvaluator.evaluerFiltreringsregler(
                    FILTRERINGSREGLER_SØKNAD,
                    fakta,
                )

            // Assert
            assertFørsteIkkeOppfylteRegel(evalueringer, Identifikator.BARN_ER_IKKE_UKRAINSK_STATSBORGER)
        }
    }

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
