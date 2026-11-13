package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate

class SlåSammenFinnmarkstilleggEllerSvalbardUtilTest {
    @Nested
    inner class ErUlikUtenomFinnmarksOgSvalbardTillegg {
        private val vilkårResultat =
            lagVilkårResultat(
                id = 1,
                personResultat = lagPersonResultat(vilkårsvurdering = lagVilkårsvurdering()),
                periodeFom = LocalDate.of(2010, 6, 1),
                periodeTom = LocalDate.of(2025, 10, 31),
                vurderesEtter = Regelverk.NASJONALE_REGLER,
            )

        @ParameterizedTest
        @EnumSource(UtdypendeVilkårsvurdering::class, names = ["BOSATT_I_FINNMARK_NORD_TROMS", "BOSATT_PÅ_SVALBARD"])
        fun `skal returnere false når bare tilleggsmarkering, datoer og metadata er ulike`(
            utdypendeVilkår: UtdypendeVilkårsvurdering,
        ) {
            // Arrange
            val endretVilkårResultat =
                vilkårResultat.copy(
                    id = 2,
                    periodeFom = LocalDate.of(2025, 11, 1),
                    periodeTom = null,
                    utdypendeVilkårsvurderinger = listOf(utdypendeVilkår),
                    begrunnelse = "Oppdatert vurdering",
                    sistEndretIBehandlingId = vilkårResultat.sistEndretIBehandlingId + 1,
                    erAutomatiskVurdert = true,
                    erOpprinneligPreutfyltIBehandling = 42,
                    opprinneligKopiertFraVilkårResultat = 43,
                    opprinneligKopiertIBehandlingId = 44,
                )

            // Act
            val erUlik = listOf(endretVilkårResultat).erUlikUtenomFinnmarksOgSvalbardTillegg(listOf(vilkårResultat))

            // Assert
            assertThat(erUlik).isFalse()
        }

        @Test
        fun `skal returnere true ved endring i regelverk`() {
            // Arrange
            val endretVilkårResultat = vilkårResultat.copy(vurderesEtter = Regelverk.EØS_FORORDNINGEN)

            // Act
            val erUlik = listOf(endretVilkårResultat).erUlikUtenomFinnmarksOgSvalbardTillegg(listOf(vilkårResultat))

            // Assert
            assertThat(erUlik).isTrue()
        }

        @ParameterizedTest
        @EnumSource(UtdypendeVilkårsvurdering::class, names = ["BOSATT_I_FINNMARK_NORD_TROMS", "BOSATT_PÅ_SVALBARD"])
        fun `skal returnere true når andre utdypende vilkår endres sammen med tilleggsmarkering`(
            utdypendeVilkår: UtdypendeVilkårsvurdering,
        ) {
            // Arrange
            val endretVilkårResultat =
                vilkårResultat.copy(
                    utdypendeVilkårsvurderinger = listOf(utdypendeVilkår, UtdypendeVilkårsvurdering.VURDERT_MEDLEMSKAP),
                )

            // Act
            val erUlik = listOf(endretVilkårResultat).erUlikUtenomFinnmarksOgSvalbardTillegg(listOf(vilkårResultat))

            // Assert
            assertThat(erUlik).isTrue()
        }

        @Test
        fun `skal returnere true ved endring i resultat`() {
            // Arrange
            val endretVilkårResultat = vilkårResultat.copy(resultat = Resultat.IKKE_OPPFYLT)

            // Act
            val erUlik = listOf(endretVilkårResultat).erUlikUtenomFinnmarksOgSvalbardTillegg(listOf(vilkårResultat))

            // Assert
            assertThat(erUlik).isTrue()
        }
    }
}
