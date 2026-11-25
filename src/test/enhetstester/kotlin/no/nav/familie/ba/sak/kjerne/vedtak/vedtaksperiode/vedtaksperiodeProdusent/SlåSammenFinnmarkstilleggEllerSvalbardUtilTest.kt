package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.vedtaksperiodeProdusent

import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.tidslinje.Periode
import no.nav.familie.tidslinje.tilTidslinje
import no.nav.familie.tidslinje.utvidelser.byttUtNullListeMedTomListe
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import no.nav.familie.tidslinje.utvidelser.tilPerioderIkkeNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.YearMonth

class SlåSammenFinnmarkstilleggEllerSvalbardUtilTest {
    @Nested
    inner class SlåSammenSplitterPåUtdypendeVilkår {
        @ParameterizedTest
        @EnumSource(UtdypendeVilkårsvurdering::class, names = ["BOSATT_I_FINNMARK_NORD_TROMS", "BOSATT_PÅ_SVALBARD"])
        fun `skal beholde eksplisitt avslag på annet vilkår når bare søker får tilleggsmarkering`(
            utdypendeVilkår: UtdypendeVilkårsvurdering,
        ) {
            // Arrange
            val fom = LocalDate.of(2025, 10, 1)
            val splitt = LocalDate.of(2025, 11, 1)
            val tom = LocalDate.of(2025, 12, 31)
            val vilkårsvurdering = lagVilkårsvurdering()
            val søker = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)
            val barn = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)
            val bosattIRiketSøker =
                lagVilkårResultat(
                    id = 1,
                    personResultat = søker,
                    periodeFom = fom,
                    periodeTom = splitt.minusDays(1),
                )
            val bosattIRiketBarn =
                lagVilkårResultat(
                    id = 3,
                    personResultat = barn,
                    periodeFom = fom,
                    periodeTom = tom,
                )
            val lovligOpphold =
                lagVilkårResultat(
                    id = 4,
                    personResultat = barn,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    periodeFom = fom,
                    periodeTom = splitt.minusDays(1),
                )
            val bosattIRiketSøkerEtter =
                bosattIRiketSøker.copy(
                    id = 2,
                    periodeFom = splitt,
                    periodeTom = tom,
                    utdypendeVilkårsvurderinger = listOf(utdypendeVilkår),
                )
            val avslagLovligOpphold =
                lovligOpphold.copy(
                    id = 5,
                    periodeFom = splitt,
                    periodeTom = tom,
                    resultat = Resultat.IKKE_OPPFYLT,
                    erEksplisittAvslagPåSøknad = true,
                )
            val perioder =
                listOf(
                    Periode(listOf(bosattIRiketSøker, bosattIRiketBarn, lovligOpphold), fom, splitt.minusDays(1)),
                    Periode(listOf(bosattIRiketSøkerEtter, bosattIRiketBarn, avslagLovligOpphold), splitt, tom),
                )

            // Act
            val sammenslåttePerioder =
                perioder
                    .tilTidslinje()
                    .slåSammenSplitterPåUtdypendeVilkår()
                    .tilPerioder()
                    .map { it.byttUtNullListeMedTomListe() }

            // Assert
            assertThat(sammenslåttePerioder).containsExactlyElementsOf(perioder)
        }

        @ParameterizedTest
        @EnumSource(UtdypendeVilkårsvurdering::class, names = ["BOSATT_I_FINNMARK_NORD_TROMS", "BOSATT_PÅ_SVALBARD"])
        fun `skal slå sammen perioder når bare søker får tilleggsmarkering og vilkårresultatene har ulik id`(
            utdypendeVilkår: UtdypendeVilkårsvurdering,
        ) {
            // Arrange
            val fom = LocalDate.of(2025, 10, 1)
            val splitt = LocalDate.of(2025, 11, 1)
            val tom = LocalDate.of(2025, 12, 31)
            val vilkårsvurdering = lagVilkårsvurdering()
            val søker = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)
            val barn = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)
            val bosattIRiketSøker =
                lagVilkårResultat(
                    id = 1,
                    personResultat = søker,
                    periodeFom = fom,
                    periodeTom = splitt.minusDays(1),
                )
            val bosattIRiketSøkerEtter =
                bosattIRiketSøker.copy(
                    id = 2,
                    periodeFom = splitt,
                    periodeTom = tom,
                    utdypendeVilkårsvurderinger = listOf(utdypendeVilkår),
                )
            val bosattIRiketBarn =
                lagVilkårResultat(
                    id = 3,
                    personResultat = barn,
                    periodeFom = fom,
                    periodeTom = tom,
                )
            val perioder =
                listOf(
                    Periode(listOf(bosattIRiketSøker, bosattIRiketBarn), fom, splitt.minusDays(1)),
                    Periode(listOf(bosattIRiketSøkerEtter, bosattIRiketBarn), splitt, tom),
                )

            // Act
            val sammenslåttePerioder =
                perioder
                    .tilTidslinje()
                    .slåSammenSplitterPåUtdypendeVilkår()
                    .tilPerioder()
                    .map { it.byttUtNullListeMedTomListe() }

            // Assert
            assertThat(sammenslåttePerioder).containsExactly(Periode(listOf(bosattIRiketSøker, bosattIRiketBarn), fom, tom))
        }
    }

    @Nested
    inner class SlåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering {
        private val fom = LocalDate.of(2026, 1, 1)
        private val splitt = LocalDate.of(2026, 2, 1)
        private val tom = LocalDate.of(2026, 3, 31)
        private val person = lagPerson()
        private val vilkårResultat =
            VilkårResultatForVedtaksperiode(
                lagVilkårResultat(
                    personResultat = lagPersonResultat(vilkårsvurdering = lagVilkårsvurdering(), aktør = person.aktør),
                    periodeFom = fom,
                    periodeTom = splitt.minusDays(1),
                ),
            )
        private val grunnlag =
            VedtaksperiodeGrunnlagForPersonVilkårIkkeInnvilget(
                person = person,
                vilkårResultaterForVedtaksperiode = listOf(vilkårResultat),
            )
        private val grunnlagMedSvalbard =
            grunnlag.copy(
                vilkårResultaterForVedtaksperiode =
                    listOf(
                        vilkårResultat.copy(
                            fom = splitt,
                            tom = tom,
                            utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                        ),
                    ),
            )

        @Test
        fun `skal slå sammen tre perioder uten utbetaling og beholde første grunnlag`() {
            // Arrange
            val grunnlagMedFinnmark =
                grunnlagMedSvalbard.copy(
                    vilkårResultaterForVedtaksperiode =
                        grunnlagMedSvalbard.vilkårResultaterForVedtaksperiode.map {
                            it.copy(utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS))
                        },
                )
            val perioder =
                listOf<Periode<VedtaksperiodeGrunnlagForPerson>>(
                    Periode(grunnlag, fom, splitt.minusDays(1)),
                    Periode(grunnlagMedFinnmark, splitt, tom.minusMonths(1)),
                    Periode(grunnlagMedSvalbard, tom.minusMonths(1).plusDays(1), tom),
                )

            // Act
            val sammenslåttePerioder = perioder.tilTidslinje().slåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering().tilPerioderIkkeNull()

            // Assert
            assertThat(sammenslåttePerioder).containsExactly(Periode(grunnlag, fom, tom))
        }

        @Test
        fun `skal beholde splitt ved endring i tilleggsmarkering når det er utbetaling`() {
            // Arrange
            val andel = AndelForVedtaksperiode(lagAndelTilkjentYtelse(fom = YearMonth.from(fom), tom = YearMonth.from(tom), person = person))
            val grunnlagMedUtbetaling =
                VedtaksperiodeGrunnlagForPersonVilkårInnvilget(
                    person = person,
                    vilkårResultaterForVedtaksperiode = grunnlag.vilkårResultaterForVedtaksperiode,
                    andeler = listOf(andel),
                )
            val perioder =
                listOf<Periode<VedtaksperiodeGrunnlagForPerson>>(
                    Periode(grunnlagMedUtbetaling, fom, splitt.minusDays(1)),
                    Periode(grunnlagMedUtbetaling.copy(vilkårResultaterForVedtaksperiode = grunnlagMedSvalbard.vilkårResultaterForVedtaksperiode), splitt, tom),
                )

            // Act
            val sammenslåttePerioder = perioder.tilTidslinje().slåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering().tilPerioderIkkeNull()

            // Assert
            assertThat(sammenslåttePerioder).containsExactlyElementsOf(perioder)
        }

        @Test
        fun `skal ikke slå sammen perioder over et opphold`() {
            // Arrange
            val perioder =
                listOf<Periode<VedtaksperiodeGrunnlagForPerson>>(
                    Periode(grunnlag, fom, splitt.minusDays(1)),
                    Periode(grunnlagMedSvalbard, splitt.plusDays(1), tom),
                )

            // Act
            val sammenslåttePerioder = perioder.tilTidslinje().slåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering().tilPerioderIkkeNull()

            // Assert
            assertThat(sammenslåttePerioder).containsExactlyElementsOf(perioder)
        }

        @Test
        fun `skal beholde splitt når andre vilkår endres sammen med tilleggsmarkering`() {
            // Arrange
            val grunnlagMedEndretResultat =
                grunnlagMedSvalbard.copy(
                    vilkårResultaterForVedtaksperiode = grunnlagMedSvalbard.vilkårResultaterForVedtaksperiode.map { it.copy(resultat = Resultat.IKKE_OPPFYLT) },
                )
            val perioder =
                listOf<Periode<VedtaksperiodeGrunnlagForPerson>>(
                    Periode(grunnlag, fom, splitt.minusDays(1)),
                    Periode(grunnlagMedEndretResultat, splitt, tom),
                )

            // Act
            val sammenslåttePerioder = perioder.tilTidslinje().slåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering().tilPerioderIkkeNull()

            // Assert
            assertThat(sammenslåttePerioder).containsExactlyElementsOf(perioder)
        }

        @Test
        fun `skal beholde splitt når tilleggsmarkeringen er lik første grunnlag etter en sammenslåing`() {
            // Arrange
            val grunnlagUtenTilleggEtter =
                grunnlag.copy(vilkårResultaterForVedtaksperiode = listOf(vilkårResultat.copy(fom = LocalDate.of(2026, 3, 1), tom = tom)))
            val perioder =
                listOf<Periode<VedtaksperiodeGrunnlagForPerson>>(
                    Periode(grunnlag, fom, splitt.minusDays(1)),
                    Periode(grunnlagMedSvalbard, splitt, LocalDate.of(2026, 2, 28)),
                    Periode(grunnlagUtenTilleggEtter, LocalDate.of(2026, 3, 1), tom),
                )

            // Act
            val sammenslåttePerioder = perioder.tilTidslinje().slåSammenPerioderUtenUtbetalingMedBareEndringITilleggsmarkering().tilPerioderIkkeNull()

            // Assert
            assertThat(sammenslåttePerioder).containsExactly(
                Periode(grunnlag, fom, LocalDate.of(2026, 2, 28)),
                perioder.last(),
            )
        }
    }

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
        fun `skal returnere false når vilkårresultatene er like, men kommer i ulik rekkefølge`() {
            // Arrange
            val lovligOpphold = vilkårResultat.copy(id = 2, vilkårType = Vilkår.LOVLIG_OPPHOLD)

            // Act
            val erUlik = listOf(lovligOpphold, vilkårResultat).erUlikUtenomFinnmarksOgSvalbardTillegg(listOf(vilkårResultat, lovligOpphold))

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
