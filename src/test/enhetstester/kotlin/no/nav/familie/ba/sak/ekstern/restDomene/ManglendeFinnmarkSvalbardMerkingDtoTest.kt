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
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat.OPPFYLT
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.ba.tss.Gyldighetsperiode
import no.nav.familie.kontrakter.ba.tss.SamhandlerAdresse
import no.nav.familie.kontrakter.ba.tss.SamhandlerInfo
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import no.nav.familie.tidslinje.utvidelser.tilPerioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ManglendeFinnmarkSvalbardMerkingDtoTest {
    @Nested
    inner class TilManglendeSvalbardmerkingPerioder {
        @Test
        fun `skal finne alle perioder for alle personer som ikke er markert med 'Bosatt på Svalbard' men har oppholdsadresse på Svalbard`() {
            // Arrange
            val bosattPåSvalbardPeriode = DatoIntervallEntitet(fom = LocalDate.of(2025, 9, 1), tom = null)

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
            assertThat(manglendeSvalbardmerking.all { it.manglendeFinnmarkSvalbardMerkingPerioder.size == 1 }).isTrue
            val manglendePerioder = manglendeSvalbardmerking.flatMap { it.manglendeFinnmarkSvalbardMerkingPerioder }
            assertThat(manglendePerioder.all { manglendePeriode -> manglendePeriode.fom == bosattPåSvalbardPeriode.fom && manglendePeriode.tom == bosattPåSvalbardPeriode.tom }).isTrue
        }

        @Test
        fun `skal returnere tom liste dersom alle perioder for alle personer med oppholdsadresse på Svalbard har blitt markert med "Bosatt på Svalbard"`() {
            // Arrange
            val bosattPåSvalbardPeriode = DatoIntervallEntitet(fom = LocalDate.of(2025, 9, 1), tom = null)

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

        @Test
        fun `skal ikke vise perioder som slutter før 30 september 2025`() {
            // Arrange
            val behandling = lagBehandling()
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandlingId = behandling.id)

            val søker =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    type = PersonType.SØKER,
                    fødselsdato = LocalDate.of(1980, 1, 1),
                    oppholdsadresser = {
                        listOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(1980, 1, 1),
                                        tom = LocalDate.of(2025, 9, 29),
                                    ),
                            ),
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = KommunerIFinnmarkOgNordTroms.NORDREISA.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(2025, 9, 30),
                                        tom = null,
                                    ),
                            ),
                        )
                    },
                )
            personopplysningGrunnlag.personer.add(søker)

            val barn =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    type = PersonType.BARN,
                    fødselsdato = LocalDate.of(2015, 1, 1),
                    oppholdsadresser = {
                        listOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(2015, 1, 1),
                                        tom = LocalDate.of(2025, 9, 29),
                                    ),
                            ),
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = KommunerIFinnmarkOgNordTroms.NORDREISA.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(2025, 9, 30),
                                        tom = null,
                                    ),
                            ),
                        )
                    },
                )
            personopplysningGrunnlag.personer.add(barn)

            val vilkårsvurdering =
                no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(1980, 1, 1),
                                                    tom = LocalDate.of(2025, 9, 29),
                                                ),
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(2025, 9, 30),
                                                    tom = null,
                                                ),
                                            utdypendeVilkårsvurderinger =
                                                listOf(
                                                    UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS,
                                                ),
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = barn.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(2015, 1, 1),
                                                    tom = LocalDate.of(2025, 9, 29),
                                                ),
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(2025, 9, 30),
                                                    tom = null,
                                                ),
                                            utdypendeVilkårsvurderinger =
                                                listOf(
                                                    UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS,
                                                ),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val personer = listOf(søker, barn)

            // Act
            val manglendeSvalbardmerking = personer.tilManglendeSvalbardmerkingPerioder(personResultater = vilkårsvurdering.personResultater)

            // Assert
            assertThat(manglendeSvalbardmerking).isEmpty()
        }

        @Test
        fun `skal vise perioder som har fom dato før 30 september 2025 hvor tom dato er null`() {
            // Arrange
            val behandling = lagBehandling()
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandlingId = behandling.id)

            val søker =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    type = PersonType.SØKER,
                    fødselsdato = LocalDate.of(1980, 1, 1),
                    oppholdsadresser = {
                        listOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(1980, 1, 1),
                                        tom = null,
                                    ),
                            ),
                        )
                    },
                )
            personopplysningGrunnlag.personer.add(søker)

            val barn =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    type = PersonType.BARN,
                    fødselsdato = LocalDate.of(2025, 9, 29),
                    oppholdsadresser = {
                        listOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(2025, 9, 29),
                                        tom = null,
                                    ),
                            ),
                        )
                    },
                )
            personopplysningGrunnlag.personer.add(barn)

            val vilkårsvurdering =
                no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(1980, 1, 1),
                                                    tom = null,
                                                ),
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = barn.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(2025, 9, 29),
                                                    tom = null,
                                                ),
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val personer = listOf(søker, barn)

            // Act
            val manglendeSvalbardmerking = personer.tilManglendeSvalbardmerkingPerioder(personResultater = vilkårsvurdering.personResultater)

            // Assert
            assertThat(manglendeSvalbardmerking).hasSize(2)
            assertThat(manglendeSvalbardmerking).anySatisfy {
                assertThat(it.ident).isEqualTo(søker.aktør.aktivFødselsnummer())
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().fom).isEqualTo(LocalDate.of(1980, 1, 1))
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().tom).isNull()
            }
            assertThat(manglendeSvalbardmerking).anySatisfy {
                assertThat(it.ident).isEqualTo(barn.aktør.aktivFødselsnummer())
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().fom).isEqualTo(LocalDate.of(2025, 9, 29))
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().tom).isNull()
            }
        }

        @Test
        fun `skal vise perioder som har tom dato lik 30 september 2025`() {
            // Arrange
            val behandling = lagBehandling()
            val personopplysningGrunnlag = lagPersonopplysningGrunnlag(behandlingId = behandling.id)

            val søker =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    type = PersonType.SØKER,
                    fødselsdato = LocalDate.of(1980, 1, 1),
                    oppholdsadresser = {
                        listOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(1980, 1, 1),
                                        tom = LocalDate.of(2025, 9, 30),
                                    ),
                            ),
                        )
                    },
                )
            personopplysningGrunnlag.personer.add(søker)

            val barn =
                lagPerson(
                    personopplysningGrunnlag = personopplysningGrunnlag,
                    type = PersonType.BARN,
                    fødselsdato = LocalDate.of(2015, 1, 1),
                    oppholdsadresser = {
                        listOf(
                            lagGrVegadresseOppholdsadresse(
                                kommunenummer = SvalbardKommune.SVALBARD.kommunenummer,
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.of(2015, 1, 1),
                                        tom = LocalDate.of(2025, 9, 30),
                                    ),
                            ),
                        )
                    },
                )
            personopplysningGrunnlag.personer.add(barn)

            val vilkårsvurdering =
                no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering(
                    behandling = behandling,
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = søker.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(1980, 1, 1),
                                                    tom = LocalDate.of(2025, 9, 30),
                                                ),
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                    )
                                },
                            ),
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = barn.aktør,
                                lagVilkårResultater = { personResultat ->
                                    setOf(
                                        lagBosattIRiketVilkårForPersonResultat(
                                            personResultat = personResultat,
                                            behandling = behandling,
                                            periode =
                                                DatoIntervallEntitet(
                                                    fom = LocalDate.of(2015, 1, 1),
                                                    tom = LocalDate.of(2025, 9, 30),
                                                ),
                                            utdypendeVilkårsvurderinger = emptyList(),
                                        ),
                                    )
                                },
                            ),
                        )
                    },
                )

            val personer = listOf(søker, barn)

            // Act
            val manglendeSvalbardmerking = personer.tilManglendeSvalbardmerkingPerioder(personResultater = vilkårsvurdering.personResultater)

            // Assert
            assertThat(manglendeSvalbardmerking).hasSize(2)
            assertThat(manglendeSvalbardmerking).anySatisfy {
                assertThat(it.ident).isEqualTo(søker.aktør.aktivFødselsnummer())
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().fom).isEqualTo(LocalDate.of(1980, 1, 1))
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().tom).isEqualTo(LocalDate.of(2025, 9, 30))
            }
            assertThat(manglendeSvalbardmerking).anySatisfy {
                assertThat(it.ident).isEqualTo(barn.aktør.aktivFødselsnummer())
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().fom).isEqualTo(LocalDate.of(2015, 1, 1))
                assertThat(it.manglendeFinnmarkSvalbardMerkingPerioder.first().tom).isEqualTo(LocalDate.of(2025, 9, 30))
            }
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
                periodeTom = periode.tom,
                begrunnelse = "",
                utdypendeVilkårsvurderinger = utdypendeVilkårsvurderinger,
            )
    }

    @Nested
    inner class TilManglendeFinnmarkmerkingPerioder {
        @Test
        fun `skal finne alle perioder for alle personer som ikke er markert med 'Bosatt på Svalbard' men har oppholdsadresse på Svalbard`() {
            // Arrange
            val forretningsadresseIFinnmarkEllerNordTromsPeriode = Gyldighetsperiode(fom = LocalDate.of(2025, 9, 1), tom = null)

            val samhandlerInfo =
                SamhandlerInfo(
                    tssEksternId = "123",
                    navn = "Testinstitusjon",
                    adresser =
                        listOf(
                            SamhandlerAdresse(
                                adresselinjer = listOf(""),
                                postNr = "",
                                postSted = "",
                                adresseType = "",
                                kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                                gyldighetsperiode = forretningsadresseIFinnmarkEllerNordTromsPeriode,
                            ),
                        ),
                )

            val barn = lagPerson(type = PersonType.BARN)

            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = barn.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = DatoIntervallEntitet(fom = forretningsadresseIFinnmarkEllerNordTromsPeriode.fom)),
                            )
                        },
                    ),
                )

            // Act
            val manglendeFinnmarkmerking = samhandlerInfo.tilManglendeFinnmarkmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeFinnmarkmerking).isNotNull
            assertThat(manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
            val manglendePeriode = manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder?.single()
            assertThat(manglendePeriode?.fom).isEqualTo(LocalDate.of(2025, 9, 1))
            assertThat(manglendePeriode?.tom).isNull()
        }

        @Test
        fun `skal returnere tom liste dersom alle perioder for alle personer med oppholdsadresse på Svalbard har blitt markert med "Bosatt på Svalbard"`() {
            // Arrange
            val forretningsadresseIFinnmarkEllerNordTromsPeriode = Gyldighetsperiode(fom = LocalDate.of(2025, 9, 1), tom = null)

            val samhandlerInfo =
                SamhandlerInfo(
                    tssEksternId = "123",
                    navn = "Testinstitusjon",
                    adresser =
                        listOf(
                            SamhandlerAdresse(
                                adresselinjer = listOf(""),
                                postNr = "",
                                postSted = "",
                                adresseType = "",
                                kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                                gyldighetsperiode = forretningsadresseIFinnmarkEllerNordTromsPeriode,
                            ),
                        ),
                )

            val barn =
                lagPerson(type = PersonType.BARN)

            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = barn.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(personResultat = personResultat, behandling = behandling, periode = DatoIntervallEntitet(fom = forretningsadresseIFinnmarkEllerNordTromsPeriode.fom), utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)),
                            )
                        },
                    ),
                )

            // Act
            val manglendeFinnmarkmerking = samhandlerInfo.tilManglendeFinnmarkmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeFinnmarkmerking).isNull()
        }

        @Test
        fun `skal ikke vise perioder som slutter før 30 september 2025`() {
            // Arrange
            val samhandlerInfo =
                SamhandlerInfo(
                    tssEksternId = "123",
                    navn = "Testinstitusjon",
                    adresser =
                        listOf(
                            SamhandlerAdresse(
                                adresselinjer = listOf(""),
                                postNr = "",
                                postSted = "",
                                adresseType = "",
                                kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                                gyldighetsperiode =
                                    Gyldighetsperiode(
                                        fom = LocalDate.of(2010, 2, 5),
                                        tom = LocalDate.of(2025, 9, 29),
                                    ),
                            ),
                        ),
                )

            val barn = lagPerson(type = PersonType.BARN)

            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = barn.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(
                                    personResultat = personResultat,
                                    behandling = behandling,
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.of(2010, 2, 5),
                                            tom = LocalDate.of(2025, 9, 29),
                                        ),
                                    utdypendeVilkårsvurderinger = emptyList(),
                                ),
                            )
                        },
                    ),
                )

            // Act
            val manglendeFinnmarkmerking = samhandlerInfo.tilManglendeFinnmarkmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder).isNull()
        }

        @Test
        fun `skal vise perioder med en fom dato før 30 september 2025 hvor tom dato er null`() {
            // Arrange
            val samhandlerInfo =
                SamhandlerInfo(
                    tssEksternId = "123",
                    navn = "Testinstitusjon",
                    adresser =
                        listOf(
                            SamhandlerAdresse(
                                adresselinjer = listOf(""),
                                postNr = "",
                                postSted = "",
                                adresseType = "",
                                kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                                gyldighetsperiode =
                                    Gyldighetsperiode(
                                        fom = LocalDate.of(2025, 9, 29),
                                        tom = null,
                                    ),
                            ),
                        ),
                )

            val barn = lagPerson(type = PersonType.BARN)

            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = barn.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(
                                    personResultat = personResultat,
                                    behandling = behandling,
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.of(2025, 9, 29),
                                            tom = null,
                                        ),
                                    utdypendeVilkårsvurderinger = emptyList(),
                                ),
                            )
                        },
                    ),
                )

            // Act
            val manglendeFinnmarkmerking = samhandlerInfo.tilManglendeFinnmarkmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeFinnmarkmerking).isNotNull()
            assertThat(manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
            val manglendePeriode = manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder?.single()
            assertThat(manglendePeriode?.fom).isEqualTo(LocalDate.of(2025, 9, 29))
            assertThat(manglendePeriode?.tom).isNull()
        }

        @Test
        fun `skal vise perioder med tom dato lik 30 september 2025`() {
            // Arrange
            val samhandlerInfo =
                SamhandlerInfo(
                    tssEksternId = "123",
                    navn = "Testinstitusjon",
                    adresser =
                        listOf(
                            SamhandlerAdresse(
                                adresselinjer = listOf(""),
                                postNr = "",
                                postSted = "",
                                adresseType = "",
                                kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                                gyldighetsperiode =
                                    Gyldighetsperiode(
                                        fom = LocalDate.of(2010, 2, 5),
                                        tom = LocalDate.of(2025, 9, 30),
                                    ),
                            ),
                        ),
                )

            val barn = lagPerson(type = PersonType.BARN)

            val behandling = lagBehandling()
            val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = barn.aktør.aktivFødselsnummer(), barnasIdenter = listOf(barn.aktør.aktivFødselsnummer()))
            val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
            val personResultater =
                setOf(
                    lagPersonResultat(
                        vilkårsvurdering = vilkårsvurdering,
                        aktør = barn.aktør,
                        lagVilkårResultater = { personResultat ->
                            setOf(
                                lagBosattIRiketVilkårForPersonResultat(
                                    personResultat = personResultat,
                                    behandling = behandling,
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.of(2010, 2, 5),
                                            tom = LocalDate.of(2025, 9, 30),
                                        ),
                                    utdypendeVilkårsvurderinger = emptyList(),
                                ),
                            )
                        },
                    ),
                )

            // Act
            val manglendeFinnmarkmerking = samhandlerInfo.tilManglendeFinnmarkmerkingPerioder(personResultater = personResultater)

            // Assert
            assertThat(manglendeFinnmarkmerking).isNotNull()
            assertThat(manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder).hasSize(1)
            val manglendePeriode = manglendeFinnmarkmerking?.manglendeFinnmarkSvalbardMerkingPerioder?.single()
            assertThat(manglendePeriode?.fom).isEqualTo(LocalDate.of(2010, 2, 5))
            assertThat(manglendePeriode?.tom).isEqualTo(LocalDate.of(2025, 9, 30))
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
                        kommunenummer = "0000",
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
            assertThat(perioder[0].verdi).isTrue

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(tredjeFom.minusDays(1))
            assertThat(perioder[1].verdi).isFalse

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
            assertThat(perioder[2].verdi).isTrue
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
                        kommunenummer = "0000",
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
            assertThat(perioder[0].verdi).isTrue

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(andreTom)
            assertThat(perioder[1].verdi).isFalse

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
            assertThat(perioder[2].verdi).isTrue
        }
    }

    @Nested
    inner class TilFinnmmarkEllerNordTromsOppholdTidslinje {
        @Test
        fun `skal lage perioder med tom dato satt til fom dato til neste element dersom tom dato ikke finnes`() {
            // Arrange
            val førsteFom = LocalDate.of(2021, 1, 1)
            val andreFom = LocalDate.of(2022, 5, 5)
            val tredjeFom = LocalDate.of(2023, 10, 10)

            val samhandlerAdresser =
                listOf(
                    SamhandlerAdresse(
                        kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                        gyldighetsperiode = Gyldighetsperiode(fom = førsteFom, tom = null),
                        adresselinjer = emptyList(),
                        postNr = "",
                        postSted = "",
                        adresseType = "",
                    ),
                    SamhandlerAdresse(
                        kommunenummer = "0000",
                        gyldighetsperiode = Gyldighetsperiode(fom = andreFom, tom = null),
                        adresselinjer = emptyList(),
                        postNr = "",
                        postSted = "",
                        adresseType = "",
                    ),
                    SamhandlerAdresse(
                        kommunenummer = KommunerIFinnmarkOgNordTroms.NORDKAPP.kommunenummer,
                        gyldighetsperiode = Gyldighetsperiode(fom = tredjeFom, tom = null),
                        adresselinjer = emptyList(),
                        postNr = "",
                        postSted = "",
                        adresseType = "",
                    ),
                )

            // Act
            val oppholdsadresseTidslinje = samhandlerAdresser.tilFinnmmarkEllerNordTromsOppholdTidslinje()

            // Assert
            val perioder = oppholdsadresseTidslinje.tilPerioder()
            assertThat(perioder).hasSize(3)

            assertThat(perioder[0].fom).isEqualTo(førsteFom)
            assertThat(perioder[0].tom).isEqualTo(andreFom.minusDays(1))
            assertThat(perioder[0].verdi).isTrue

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(tredjeFom.minusDays(1))
            assertThat(perioder[1].verdi).isFalse

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
            assertThat(perioder[2].verdi).isTrue
        }

        @Test
        fun `skal lage perioder med tom dato dersom tom dato finnes`() {
            // Arrange
            val førsteFom = LocalDate.of(2021, 1, 1)
            val førsteTom = LocalDate.of(2022, 5, 4)
            val andreFom = LocalDate.of(2022, 5, 5)
            val andreTom = LocalDate.of(2023, 10, 9)
            val tredjeFom = LocalDate.of(2023, 10, 10)

            val samhandlerAdresser =
                listOf(
                    SamhandlerAdresse(
                        kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer,
                        gyldighetsperiode = Gyldighetsperiode(fom = førsteFom, tom = førsteTom),
                        adresselinjer = emptyList(),
                        postNr = "",
                        postSted = "",
                        adresseType = "",
                    ),
                    SamhandlerAdresse(
                        kommunenummer = "0000",
                        gyldighetsperiode = Gyldighetsperiode(fom = andreFom, tom = andreTom),
                        adresselinjer = emptyList(),
                        postNr = "",
                        postSted = "",
                        adresseType = "",
                    ),
                    SamhandlerAdresse(
                        kommunenummer = KommunerIFinnmarkOgNordTroms.NORDKAPP.kommunenummer,
                        gyldighetsperiode = Gyldighetsperiode(fom = tredjeFom, tom = null),
                        adresselinjer = emptyList(),
                        postNr = "",
                        postSted = "",
                        adresseType = "",
                    ),
                )

            // Act
            val oppholdsadresseTidslinje = samhandlerAdresser.tilFinnmmarkEllerNordTromsOppholdTidslinje()

            // Assert
            val perioder = oppholdsadresseTidslinje.tilPerioder()
            assertThat(perioder).hasSize(3)

            assertThat(perioder[0].fom).isEqualTo(førsteFom)
            assertThat(perioder[0].tom).isEqualTo(førsteTom)
            assertThat(perioder[0].verdi).isTrue

            assertThat(perioder[1].fom).isEqualTo(andreFom)
            assertThat(perioder[1].tom).isEqualTo(andreTom)
            assertThat(perioder[1].verdi).isFalse

            assertThat(perioder[2].fom).isEqualTo(tredjeFom)
            assertThat(perioder[2].tom).isNull()
            assertThat(perioder[2].verdi).isTrue
        }
    }
}
