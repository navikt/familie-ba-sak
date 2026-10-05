package no.nav.familie.ba.sak.kjerne.søknad

import no.nav.familie.ba.sak.datagenerator.lagStringSøknadsfelt
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.erFosterbarn
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.erIBeredskapshjem
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.harKryssetForDeltBosted
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.inneholderVedlegg
import no.nav.familie.ba.sak.kjerne.søknad.SøknadMapper.Companion.tilBehandlingUnderkategori
import no.nav.familie.kontrakter.ba.søknad.v4.Søknadstype
import no.nav.familie.kontrakter.ba.søknad.v7.Dokumentasjonsbehov
import no.nav.familie.kontrakter.ba.søknad.v7.Søknaddokumentasjon
import no.nav.familie.kontrakter.ba.søknad.v7.Søknadsvedlegg
import no.nav.familie.kontrakter.ba.søknad.v8.AndreForelder
import no.nav.familie.kontrakter.ba.søknad.v8.AndreForelderUtvidet
import no.nav.familie.kontrakter.felles.søknad.Søknadsfelt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SøknadMapperTest {
    @Nested
    inner class TilBehandlingUnderkategori {
        @Test
        fun `skal mappe Søknadstype ORDINÆR til BehandlingUnderkategori ORDINÆR`() {
            // Act
            val behandlingUnderkategori = Søknadstype.ORDINÆR.tilBehandlingUnderkategori()

            // Assert
            assertThat(behandlingUnderkategori)
                .isEqualTo(BehandlingUnderkategori.ORDINÆR)
        }

        @Test
        fun `skal mappe Søknadstype UTVIDET til BehandlingUnderkategori UTVIDET`() {
            // Act
            val behandlingUnderkategori = Søknadstype.UTVIDET.tilBehandlingUnderkategori()

            // Assert
            assertThat(behandlingUnderkategori)
                .isEqualTo(BehandlingUnderkategori.UTVIDET)
        }

        @Test
        fun `Søknadstype IKKE_SATT skal kaste feil`() {
            // Act & Assert
            val exception = assertThrows<IllegalArgumentException> { Søknadstype.IKKE_SATT.tilBehandlingUnderkategori() }
            assertThat(exception.message).isEqualTo("Søknadstype i Søknad må være satt for innsendte søknader: ${Søknadstype.IKKE_SATT}")
        }
    }

    @Nested
    inner class ErFosterbarn {
        @Test
        fun `skal gi true når søker har krysset ja for at barnet er fosterbarn`() {
            // Arrange
            val spørsmål = mapOf<String, Søknadsfelt<Any>>("erFosterbarn" to lagStringSøknadsfelt("JA"))

            // Act & Assert
            assertThat(spørsmål.erFosterbarn()).isTrue()
        }

        @Test
        fun `skal gi false når søker har krysset nei for at barnet er fosterbarn`() {
            // Arrange
            val spørsmål = mapOf<String, Søknadsfelt<Any>>("erFosterbarn" to lagStringSøknadsfelt("NEI"))

            // Act & Assert
            assertThat(spørsmål.erFosterbarn()).isFalse()
        }

        @Test
        fun `skal gi false når søknaden ikke inneholder spørsmålet om fosterbarn`() {
            // Arrange
            val spørsmål = emptyMap<String, Søknadsfelt<Any>>()

            // Act & Assert
            assertThat(spørsmål.erFosterbarn()).isFalse()
        }
    }

    @Nested
    inner class ErIBeredskapshjem {
        @Test
        fun `skal gi true når søker har svart ja på at barnet bor i beredskapshjem`() {
            // Arrange
            val spørsmål =
                mapOf<String, Søknadsfelt<Any>>(
                    "erIBeredskapshjem" to lagStringSøknadsfelt("JA"),
                )
            // Act & Assert
            assertThat(spørsmål.erIBeredskapshjem()).isTrue()
        }

        @Test
        fun `skal gi false når søker har svart nei på at barnet bor i beredskapshjem`() {
            // Arrange
            val spørsmål =
                mapOf<String, Søknadsfelt<Any>>(
                    "erIBeredskapshjem" to lagStringSøknadsfelt("NEI"),
                )
            // Act & Assert
            assertThat(spørsmål.erIBeredskapshjem()).isFalse()
        }

        @Test
        fun `skal gi false når eldre søknad mangler spørsmål om beredskapshjem`() {
            // Arrange
            val spørsmål = emptyMap<String, Søknadsfelt<Any>>()
            // Act & Assert
            assertThat(spørsmål.erIBeredskapshjem()).isFalse()
        }
    }

    @Nested
    inner class HarKryssetForDeltBosted {
        @Test
        fun `skal gi true når søker har krysset ja for skriftlig avtale om delt bosted`() {
            // Arrange
            val andreForelder = lagAndreForelder(skriftligAvtaleOmDeltBosted = lagStringSøknadsfelt("JA"))

            // Act & Assert
            assertThat(andreForelder.harKryssetForDeltBosted()).isTrue()
        }

        @Test
        fun `skal gi false når søker har krysset nei for skriftlig avtale om delt bosted`() {
            // Arrange
            val andreForelder = lagAndreForelder(skriftligAvtaleOmDeltBosted = lagStringSøknadsfelt("NEI"))

            // Act & Assert
            assertThat(andreForelder.harKryssetForDeltBosted()).isFalse()
        }

        @Test
        fun `skal gi false når andre forelder er oppgitt uten å ha svart på spørsmålet om delt bosted`() {
            // Arrange
            val andreForelder = lagAndreForelder(skriftligAvtaleOmDeltBosted = null)

            // Act & Assert
            assertThat(andreForelder.harKryssetForDeltBosted()).isFalse()
        }

        @Test
        fun `skal gi false når andre forelder ikke er oppgitt`() {
            // Arrange
            val andreForelder: AndreForelder? = null

            // Act & Assert
            assertThat(andreForelder.harKryssetForDeltBosted()).isFalse()
        }

        private fun lagAndreForelder(skriftligAvtaleOmDeltBosted: Søknadsfelt<String>?): AndreForelder =
            AndreForelder(
                kanIkkeGiOpplysninger = lagStringSøknadsfelt("NEI"),
                skriftligAvtaleOmDeltBosted = skriftligAvtaleOmDeltBosted,
                utvidet = AndreForelderUtvidet(),
            )
    }

    @Nested
    inner class InneholderVedlegg {
        @Test
        fun `skal gi true når søker har lastet opp minst ett vedlegg`() {
            // Arrange
            val dokumentasjon =
                listOf(
                    lagSøknaddokumentasjon(
                        opplastedeVedlegg =
                            listOf(
                                Søknadsvedlegg(
                                    dokumentId = "1",
                                    navn = "vedlegg.pdf",
                                    tittel = Dokumentasjonsbehov.ANNEN_DOKUMENTASJON,
                                ),
                            ),
                    ),
                )

            // Act & Assert
            assertThat(dokumentasjon.inneholderVedlegg()).isTrue()
        }

        @Test
        fun `skal gi false når søker har sagt at dokumentasjon er sendt inn uten å laste opp vedlegg`() {
            // Arrange
            val dokumentasjon = listOf(lagSøknaddokumentasjon(opplastedeVedlegg = emptyList()))

            // Act & Assert
            assertThat(dokumentasjon.inneholderVedlegg()).isFalse()
        }

        @Test
        fun `skal gi false når søknaden ikke har dokumentasjon`() {
            // Arrange
            val dokumentasjon = emptyList<Søknaddokumentasjon>()

            // Act & Assert
            assertThat(dokumentasjon.inneholderVedlegg()).isFalse()
        }

        private fun lagSøknaddokumentasjon(opplastedeVedlegg: List<Søknadsvedlegg>): Søknaddokumentasjon =
            Søknaddokumentasjon(
                dokumentasjonsbehov = Dokumentasjonsbehov.ANNEN_DOKUMENTASJON,
                harSendtInn = true,
                opplastedeVedlegg = opplastedeVedlegg,
                dokumentasjonSpråkTittel = emptyMap(),
            )
    }
}
