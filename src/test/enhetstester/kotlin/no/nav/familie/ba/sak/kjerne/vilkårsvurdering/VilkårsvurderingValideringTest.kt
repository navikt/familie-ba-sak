package no.nav.familie.ba.sak.kjerne.vilkårsvurdering

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonEnkel
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Regelverk
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering.DELT_BOSTED
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOR_MED_SØKER
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.BOSATT_I_RIKET
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår.UNDER_18_ÅR
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.slf4j.LoggerFactory
import java.time.LocalDate

class VilkårsvurderingValideringTest {
    @Nested
    inner class ValiderIkkeBlandetRegelverk {
        @Test
        fun `skal kaste feil hvis søker vurderes etter nasjonal og minst ett barn etter EØS`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val barn2 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.NASJONALE_REGLER, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn2 = byggPersonResultatForPersonEnkel(barn2, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                    personResultatBarn2,
                )

            assertThrows<FunksjonellFeil> {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                    behandling = lagBehandling(),
                )
            }
        }

        @ParameterizedTest(name = "skal ikke kaste feil hvis søker vurderes etter nasjonal og minst ett barn etter EØS om det er av årsak {0}")
        @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "FINNMARKSTILLEGG", "SVALBARDTILLEGG"])
        fun `skal ikke kaste feil hvis søker vurderes etter nasjonal og minst ett barn etter EØS om årsak er en av typene`(
            behandlingÅrsak: BehandlingÅrsak,
        ) {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val barn2 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.NASJONALE_REGLER, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn2 = byggPersonResultatForPersonEnkel(barn2, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                    personResultatBarn2,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1, barn2),
                    behandling = lagBehandling(årsak = behandlingÅrsak),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis både søker og barn vurderes etter eøs`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis søker vurderes etter eøs, men barn vurderes etter nasjonal`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn1 = lagPersonEnkel(PersonType.BARN)
            val personResultatSøker = byggPersonResultatForPersonEnkel(søker, Regelverk.EØS_FORORDNINGEN, vilkårsvurdering)
            val personResultatBarn1 = byggPersonResultatForPersonEnkel(barn1, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn1,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn1),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis både søker og barn vurderes etter nasjonal og eøs, men i samme perioder`() {
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)
            val barn = lagPersonEnkel(PersonType.BARN)

            val vilkårPerioder =
                listOf(
                    VilkårPeriode(
                        regelverk = Regelverk.EØS_FORORDNINGEN,
                        fom = LocalDate.now().minusMonths(9),
                        tom = LocalDate.now().minusMonths(1),
                    ),
                    VilkårPeriode(
                        regelverk = Regelverk.NASJONALE_REGLER,
                        fom = LocalDate.now().minusMonths(1).plusMonths(1),
                        tom = null,
                    ),
                )

            val personResultatSøker = byggPersonResultatForPersonIPerioder(søker, vilkårPerioder, vilkårsvurdering)
            val personResultatBarn = byggPersonResultatForPersonIPerioder(barn, vilkårPerioder, vilkårsvurdering)

            vilkårsvurdering.personResultater =
                setOf(
                    personResultatSøker,
                    personResultatBarn,
                )

            assertDoesNotThrow {
                validerIkkeBlandetRegelverk(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker, barn),
                    behandling = lagBehandling(),
                )
            }
        }
    }

    @Nested
    inner class Valider18ÅrsVilkårEksistererFraFødselsdato {
        @Test
        fun `skal ikke kaste feil hvis person ikke er barn`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = lagBehandling())
            val søker = lagPersonEnkel(PersonType.SØKER)

            // Act & assert
            assertDoesNotThrow {
                valider18ÅrsVilkårEksistererFraFødselsdato(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(søker),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal ikke kaste feil hvis barn har 18-års vilkår vurdert fra fødselsdato`() {
            // Arrange
            val vilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
            val barn = lagPersonEnkel(PersonType.BARN)
            val personResultatBarn = byggPersonResultatForPersonEnkel(barn, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            vilkårsvurdering.personResultater = setOf(personResultatBarn)

            // Act & assert
            assertDoesNotThrow {
                valider18ÅrsVilkårEksistererFraFødselsdato(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(barn),
                    behandling = lagBehandling(),
                )
            }
        }

        @Test
        fun `skal kaste feil hvis barn ikke har 18-års vilkår vurdert fra fødselsdato`() {
            // Arrange
            val vilkårsvurdering = lagVilkårsvurdering(behandling = lagBehandling())
            val barn = lagPersonEnkel(PersonType.BARN)
            val personResultatBarn = byggPersonResultatForPersonEnkel(barn, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            personResultatBarn.vilkårResultater.first { it.vilkårType == UNDER_18_ÅR }.periodeFom = barn.fødselsdato.minusDays(1)

            vilkårsvurdering.personResultater = setOf(personResultatBarn)

            // Act & assert
            assertThrows<FunksjonellFeil> {
                valider18ÅrsVilkårEksistererFraFødselsdato(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(barn),
                    behandling = lagBehandling(),
                )
            }
        }

        @ParameterizedTest
        @EnumSource(value = BehandlingÅrsak::class, names = ["SATSENDRING", "MÅNEDLIG_VALUTAJUSTERING", "FINNMARKSTILLEGG", "SVALBARDTILLEGG", "OMREGNING_18ÅR", "OMREGNING_SMÅBARNSTILLEGG"])
        fun `skal ikke kaste feil for gitte behandlingsårsaker selv om barn ikke har 18-års vilkår vurdert fra fødselsdato`(
            årsak: BehandlingÅrsak,
        ) {
            // Arrange
            val behandling = lagBehandling(årsak = årsak)
            val vilkårsvurdering = lagVilkårsvurdering(behandling = behandling)
            val barn = lagPersonEnkel(PersonType.BARN)
            val personResultatBarn = byggPersonResultatForPersonEnkel(barn, Regelverk.NASJONALE_REGLER, vilkårsvurdering)

            personResultatBarn.vilkårResultater.first { it.vilkårType == UNDER_18_ÅR }.periodeFom = barn.fødselsdato.minusDays(1)

            vilkårsvurdering.personResultater = setOf(personResultatBarn)

            // Act & assert
            assertDoesNotThrow {
                valider18ÅrsVilkårEksistererFraFødselsdato(
                    vilkårsvurdering = vilkårsvurdering,
                    søkerOgBarn = listOf(barn),
                    behandling = behandling,
                )
            }
        }
    }

    @Nested
    inner class ValiderAtManIkkeBorIBådeFinnmarkOgSvalbardSamtidigTest {
        @Test
        fun `skal kaste feil hvis man har satt bosatt i finnmark og bosatt på svalbard i samme vilkår periode`() {
            // Arrange
            val barn = lagPerson(type = PersonType.BARN, fødselsdato = LocalDate.of(2025, 1, 1))
            val personEnkelFraBarn = PersonEnkel(PersonType.BARN, barn.aktør, barn.fødselsdato, null, Målform.NB)
            val behandling = lagBehandling()

            val vilkårsvurdering =
                lagVilkårsvurdering(behandling = behandling) {
                    setOf(
                        lagPersonResultatBosattIRiketMedUtdypendeVilkårsvurdering(
                            behandling = behandling,
                            person = barn,
                            perioderMedUtdypendeVilkårsvurdering = listOf(LocalDate.of(2025, 10, 1) to null),
                            vilkårsvurdering = it,
                            utdypendeVilkårsvurderinger = listOf(BOSATT_I_FINNMARK_NORD_TROMS, BOSATT_PÅ_SVALBARD),
                        ),
                    )
                }

            // Act && Assert
            val feilmelding =
                assertThrows<FunksjonellFeil> {
                    validerAtManIkkeBorIBådeFinnmarkOgSvalbardSamtidig(
                        vilkårsvurdering = vilkårsvurdering,
                        søkerOgBarn = listOf(personEnkelFraBarn),
                    )
                }.melding

            assertThat(feilmelding).isEqualTo("Barn født 2025-01-01 kan ikke bo i Finnmark og på Svalbard samtidig.")
        }
    }

    @Nested
    inner class ValiderAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerIFinnmark {
        @Test
        fun `skal returnere true hvis barn har delt bosted og ikke bor med søker i Finnmark`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)

            val vilkårsvurdering =
                lagVilkårsvurdering {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søker.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = BOSATT_I_RIKET,
                                        utdypendeVilkårsvurderinger = listOf(BOSATT_I_FINNMARK_NORD_TROMS),
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.of(2025, 6, 1),
                                    ),
                                )
                            },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barn.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = UNDER_18_ÅR,
                                        periodeFom = barn.fødselsdato,
                                        periodeTom = barn.fødselsdato.plusYears(18),
                                    ),
                                    lagVilkårResultat(
                                        vilkårType = BOR_MED_SØKER,
                                        utdypendeVilkårsvurderinger = listOf(DELT_BOSTED),
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.of(2025, 6, 1),
                                    ),
                                )
                            },
                        ),
                    )
                }

            // Act && Assert
            assertThat(validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerIFinnmark(vilkårsvurdering)).isTrue()
        }

        @Test
        fun `skal returnere false hvis barn har delt bosted og ikke bor med søker i Finnmark i forskjellig periode`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)

            val vilkårsvurdering =
                lagVilkårsvurdering {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søker.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = BOSATT_I_RIKET,
                                        utdypendeVilkårsvurderinger = listOf(BOSATT_I_FINNMARK_NORD_TROMS),
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.of(2025, 2, 1),
                                    ),
                                )
                            },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barn.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = UNDER_18_ÅR,
                                        periodeFom = barn.fødselsdato,
                                        periodeTom = barn.fødselsdato.plusYears(18),
                                    ),
                                    lagVilkårResultat(
                                        vilkårType = BOR_MED_SØKER,
                                        utdypendeVilkårsvurderinger = listOf(DELT_BOSTED),
                                        periodeFom = LocalDate.of(2025, 3, 1),
                                        periodeTom = LocalDate.of(2025, 4, 1),
                                    ),
                                )
                            },
                        ),
                    )
                }

            // Act && Assert
            assertThat(validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerIFinnmark(vilkårsvurdering)).isFalse()
        }
    }

    @Nested
    inner class ValiderAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerPåSvalbard {
        @Test
        fun `skal returnere true hvis barn har delt bosted og ikke bor med søker på Svalbard`() {
            // Arrange
            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)

            val vilkårsvurdering =
                lagVilkårsvurdering {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søker.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = BOSATT_I_RIKET,
                                        utdypendeVilkårsvurderinger = listOf(BOSATT_PÅ_SVALBARD),
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.of(2025, 6, 1),
                                    ),
                                )
                            },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barn.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = UNDER_18_ÅR,
                                        periodeFom = barn.fødselsdato,
                                        periodeTom = barn.fødselsdato.plusYears(18),
                                    ),
                                    lagVilkårResultat(
                                        vilkårType = BOR_MED_SØKER,
                                        utdypendeVilkårsvurderinger = listOf(DELT_BOSTED),
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.of(2025, 6, 1),
                                    ),
                                )
                            },
                        ),
                    )
                }

            // Act && Assert
            assertThat(validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerPåSvalbard(vilkårsvurdering)).isTrue()
        }

        @Test
        fun `skal returnere false hvis barn har delt bosted og ikke bor med søker på Svalbard i forskjellig periode`() {
            // Arrange
            val listAppender = ListAppender<ILoggingEvent>().apply { start() }
            val logger = LoggerFactory.getLogger("VilkårsvurderingValidering.kt") as Logger
            logger.addAppender(listAppender)

            val søker = lagPerson(type = PersonType.SØKER)
            val barn = lagPerson(type = PersonType.BARN)

            val vilkårsvurdering =
                lagVilkårsvurdering {
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søker.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = BOSATT_I_RIKET,
                                        utdypendeVilkårsvurderinger = listOf(BOSATT_PÅ_SVALBARD),
                                        periodeFom = LocalDate.of(2025, 1, 1),
                                        periodeTom = LocalDate.of(2025, 2, 1),
                                    ),
                                )
                            },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barn.aktør,
                            lagVilkårResultater = {
                                setOf(
                                    lagVilkårResultat(
                                        vilkårType = UNDER_18_ÅR,
                                        periodeFom = barn.fødselsdato,
                                        periodeTom = barn.fødselsdato.plusYears(18),
                                    ),
                                    lagVilkårResultat(
                                        vilkårType = BOR_MED_SØKER,
                                        utdypendeVilkårsvurderinger = listOf(DELT_BOSTED),
                                        periodeFom = LocalDate.of(2025, 3, 1),
                                        periodeTom = LocalDate.of(2025, 4, 1),
                                    ),
                                )
                            },
                        ),
                    )
                }

            // Act && Assert
            assertThat(validerAtDetIkkeFinnesDeltBostedForBarnSomIkkeBorMedSøkerPåSvalbard(vilkårsvurdering)).isFalse
        }
    }

    private fun byggPersonResultatForPersonEnkel(
        person: PersonEnkel,
        regelverk: Regelverk,
        vilkårsvurdering: Vilkårsvurdering,
    ): PersonResultat {
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)
        val vilkårResultater =
            lagVilkårResultatForPersonIPeriode(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(type = person.type, aktør = person.aktør, fødselsdato = person.fødselsdato),
                periodeFom = LocalDate.now().minusMonths(2),
                periodeTom = LocalDate.now().minusMonths(1),
                vurderesEtter = regelverk,
                personResultat = personResultat,
            )

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }

    private data class VilkårPeriode(
        val fom: LocalDate,
        val tom: LocalDate?,
        val regelverk: Regelverk,
    )

    private fun byggPersonResultatForPersonIPerioder(
        person: PersonEnkel,
        perioder: List<VilkårPeriode>,
        vilkårsvurdering: Vilkårsvurdering,
    ): PersonResultat {
        val personResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = person.aktør)
        val vilkårResultater =
            perioder
                .flatMap {
                    lagVilkårResultatForPersonIPeriode(
                        vilkårsvurdering = vilkårsvurdering,
                        person = lagPerson(type = person.type, aktør = person.aktør),
                        periodeFom = it.fom,
                        periodeTom = it.tom,
                        vurderesEtter = it.regelverk,
                        personResultat = personResultat,
                    )
                }.toSet()

        personResultat.setSortedVilkårResultater(vilkårResultater)

        return personResultat
    }

    private fun lagVilkårResultatForPersonIPeriode(
        person: Person,
        personResultat: PersonResultat,
        vilkårsvurdering: Vilkårsvurdering,
        periodeFom: LocalDate,
        periodeTom: LocalDate?,
        vurderesEtter: Regelverk,
    ): Set<VilkårResultat> =
        Vilkår
            .hentVilkårFor(
                personType = person.type,
                fagsakType = FagsakType.NORMAL,
                behandlingUnderkategori = BehandlingUnderkategori.ORDINÆR,
            ).map {
                VilkårResultat(
                    personResultat = personResultat,
                    periodeFom = if (it.gjelderAlltidFraBarnetsFødselsdato()) person.fødselsdato else periodeFom,
                    periodeTom = periodeTom,
                    vilkårType = it,
                    resultat = Resultat.OPPFYLT,
                    begrunnelse = "",
                    sistEndretIBehandlingId = vilkårsvurdering.behandling.id,
                    vurderesEtter = vurderesEtter,
                )
            }.toSet()

    private fun lagPersonEnkel(personType: PersonType): PersonEnkel =
        PersonEnkel(
            type = personType,
            aktør = randomAktør(),
            dødsfallDato = null,
            fødselsdato =
                if (personType == PersonType.SØKER) {
                    LocalDate.now().minusYears(34)
                } else {
                    LocalDate
                        .now()
                        .minusYears(4)
                },
            målform = Målform.NB,
        )
}
