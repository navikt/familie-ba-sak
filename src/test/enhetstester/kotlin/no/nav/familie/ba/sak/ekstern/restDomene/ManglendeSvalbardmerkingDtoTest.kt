package no.nav.familie.ba.sak.ekstern.restDomene

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagGrMatrikkelOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrUkjentAdresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrUtenlandskOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ManglendeSvalbardmerkingDtoTest {
    @Nested
    inner class TilManglendeSvalbardmerkingPerioder {
        @Test
        fun `skal finne alle perioder for alle personer som ikke er markert med 'Bosatt på Svalbard' men har oppholdsadresse på Svalbard`() {
            // Arrange
            val bosattPåSvalbardPeriode = DatoIntervallEntitet(fom = LocalDate.of(2024, 8, 15), tom = null)

            val søker =
                lagPerson(type = PersonType.SØKER).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }

            val barn1 =
                lagPerson(type = PersonType.BARN).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrMatrikkelOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }

            val barn2 =
                lagPerson(type = PersonType.BARN).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrUtenlandskOppholdsadresse(
                                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }

            val barn3 =
                lagPerson(type = PersonType.BARN).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrUkjentAdresseOppholdsadresse(
                                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }
            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = søker.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer(), barn3.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = søker.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode),
                            )
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn1.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode),
                            )
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn2.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode),
                            )
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn3.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode),
                            )
                        },
                    ),
                )

            val personer = listOf(søker, barn1, barn2, barn3)

            // Act
            val manglendeSvalbardmerking = personer.tilManglendeSvalbardmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeSvalbardmerking).hasSize(4)
            assertThat(manglendeSvalbardmerking.all { it.manglendeSvalbardmerkingPerioder.size == 1 }).isTrue
            val manglendePerioder = manglendeSvalbardmerking.flatMap { it.manglendeSvalbardmerkingPerioder }
            assertThat(manglendePerioder.all { manglendePeriode -> manglendePeriode.fom == bosattPåSvalbardPeriode.fom && manglendePeriode.tom == bosattPåSvalbardPeriode.tom }).isTrue
        }

        @Test
        fun `skal returnere tom liste dersom alle perioder for alle personer med oppholdsadresse på Svalbard har blitt markert med "Bosatt på Svalbard"`() {
            // Arrange
            val bosattPåSvalbardPeriode = DatoIntervallEntitet(fom = LocalDate.of(2024, 8, 15), tom = null)

            val søker =
                lagPerson(type = PersonType.SØKER).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }

            val barn1 =
                lagPerson(type = PersonType.BARN).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrMatrikkelOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }

            val barn2 =
                lagPerson(type = PersonType.BARN).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrUtenlandskOppholdsadresse(
                                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }

            val barn3 =
                lagPerson(type = PersonType.BARN).also {
                    it.oppholdsadresser =
                        mutableListOf(
                            lagGrUkjentAdresseOppholdsadresse(
                                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                                periode = bosattPåSvalbardPeriode,
                            ),
                        )
                }
            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = søker.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn1.aktør.aktivFødselsnummer(), barn2.aktør.aktivFødselsnummer(), barn3.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = søker.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode, utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)),
                            )
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn1.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode, utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)),
                            )
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn2.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode, utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)),
                            )
                        },
                    ),
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn3.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = bosattPåSvalbardPeriode, utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)),
                            )
                        },
                    ),
                )

            val personer = listOf(søker, barn1, barn2, barn3)

            // Act
            val manglendeSvalbardmerking = personer.tilManglendeSvalbardmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeSvalbardmerking).hasSize(0)
        }

        private fun lagBosattIRiketVilkårForPersonResultat(
            personResultat: PersonResultat,
            behandling: Behandling,
            periode: DatoIntervallEntitet,
            utdypendeVilkårsvurderinger: List<UtdypendeVilkårsvurdering> = emptyList(),
        ): VilkårResultat =
            lagVilkårResultat(
                behandlingId = behandling.id,
                personResultat = personResultat,
                vilkårType = BOSATT_I_RIKET,
                resultat = OPPFYLT,
                periodeFom = periode.fom,
                periodeTom = null,
                begrunnelse = "",
                utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            )
    }

    @Nested
    inner class TilSvalbardOppholdTidslinje {
        @Test
        fun `skal lage perioder med tom dato satt til fom dato til neste element dersom tom dato ikke finnes`() {
            // Arrange
            val førsteFom = LocalDate.of(2021, 1, 1)
            val andreFom = LocalDate.of(2022, 5, 5)
            val tredjeFom = LocalDate.of(2023, 10, 10)

            val grOppholdsadresser =
                listOf(
                    lagGrVegadresseOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = førsteFom, tom = null),
                    ),
                    lagGrMatrikkelOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = andreFom, tom = null),
                    ),
                    lagGrUtenlandskOppholdsadresse(
                        oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                        periode = DatoIntervallEntitet(fom = tredjeFom, tom = null),
                    ),
                )

            // Act
            val oppholdsadresseTidslinje = grOppholdsadresser.tilSvalbardOppholdTidslinje()

            // Assert
            val perioder = oppholdsadresseTidslinje.tilPerioder()
            assertThat(perioder).hasSize(3)

            assertThat(perioder[0].fom).isEqualTo(førsteFom)
            assertThat(perioder[0].tom).isEqualTo(andreFom.minusDays(1))

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(tredjeFom.minusDays(1))

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
        }

        @Test
        fun `skal lage perioder med tom dato dersom tom dato finnes`() {
            // Arrange
            val førsteFom = LocalDate.of(2021, 1, 1)
            val førsteTom = LocalDate.of(2022, 5, 4)
            val andreFom = LocalDate.of(2022, 5, 5)
            val andreTom = LocalDate.of(2023, 10, 9)
            val tredjeFom = LocalDate.of(2023, 10, 10)

            val grOppholdsadresser =
                listOf(
                    lagGrVegadresseOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = førsteFom, tom = førsteTom),
                    ),
                    lagGrMatrikkelOppholdsadresse(
                        kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                        periode = DatoIntervallEntitet(fom = andreFom, tom = andreTom),
                    ),
                    lagGrUtenlandskOppholdsadresse(
                        oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
                        periode = DatoIntervallEntitet(fom = tredjeFom, tom = null),
                    ),
                )

            // Act
            val oppholdsadresseTidslinje = grOppholdsadresser.tilSvalbardOppholdTidslinje()

            // Assert
            val perioder = oppholdsadresseTidslinje.tilPerioder()
            assertThat(perioder).hasSize(3)

            assertThat(perioder[0].fom).isEqualTo(førsteFom)
            assertThat(perioder[0].tom).isEqualTo(førsteTom)

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(andreTom)

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
        }
    }
}
