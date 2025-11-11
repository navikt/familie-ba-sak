package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.SystemOnlyIntegrasjonKlient
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Ansettelsesperiode
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Arbeidsforhold
import no.nav.familie.ba.sak.integrasjoner.familieintegrasjoner.domene.Periode
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.StatsborgerskapService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllLovligOppholdServiceTest {
    @Nested
    inner class GenererLovligOppholdVilkårResultatTest {
        private val pdlRestKlient: SystemOnlyPdlRestKlient = mockk(relaxed = true)
        private val statsborgerskapService = mockk<StatsborgerskapService>(relaxed = true)
        private val systemOnlyIntegrasjonKlient: SystemOnlyIntegrasjonKlient = mockk(relaxed = true)
        private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
        private val preutfyllLovligOppholdService: PreutfyllLovligOppholdService = PreutfyllLovligOppholdService(pdlRestKlient, statsborgerskapService, systemOnlyIntegrasjonKlient, persongrunnlagService)

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår basert på norsk eller nordisk statsborgerskap`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("SWE", LocalDate.now().minusYears(10), null, null),
                )

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering)

            // Assert
            val lovligOppholdVilkår =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdVilkår.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal preutfylle lovlig opphold med ikke-oppfylte perioder når statsborgerskap ikke er norsk eller nordisk, og ingen arbeidsforhold`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("ES", LocalDate.now().minusYears(5), LocalDate.now().minusYears(2), null),
                    Statsborgerskap("NOR", LocalDate.now().minusYears(2).plusDays(1), null, null),
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater).hasSize(2)
            assertThat(lovligOppholdResultater.first { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.last { it.resultat == Resultat.OPPFYLT }).isNotNull
        }

        @Test
        fun `skal gi riktig fom og tom på lovlig opphold vilkår på nordisk statsborger`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("ES", LocalDate.now().minusYears(10), LocalDate.now().minusYears(5), null),
                    Statsborgerskap("NOR", LocalDate.now().minusYears(5).plusDays(1), null, null),
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .filter { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater).hasSize(2)
            assertThat(lovligOppholdResultater.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.find { it.resultat == Resultat.OPPFYLT }).isNotNull
            assertThat(lovligOppholdResultater.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(lovligOppholdResultater.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(lovligOppholdResultater.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5).plusDays(1))
            assertThat(lovligOppholdResultater.last().periodeTom).isNull()
        }

        @Test
        fun `skal sette fom på lovlig opphold vilkår lik første bostedsadresse i Norge, om fom ikke finnes på statsborgerskap`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("SWE", null, null, null),
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(lovligOppholdResultater.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis nordisk statsborger`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("NOR", LocalDate.now().minusYears(10), null, null),
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.begrunnelse)
                .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- Norsk/nordisk statsborgerskap.")
        }

        @Test
        fun `skal preutfylle oppfylt lovlig opphold vilkår hvis EØS borger og har arbeidsforhold`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null),
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            every { statsborgerskapService.hentSterkesteMedlemskap(Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null)) } returns Medlemskap.EØS

            every { systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(any(), LocalDate.now().minusYears(10)) } returns
                listOf(Arbeidsforhold(arbeidsgiver = null, ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(10), null))))

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(LocalDate.now().minusYears(10))
            assertThat(lovligOppholdResultater?.periodeTom).isNull()
            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
        }

        @Test
        fun `skal gi riktig begrunnelse for oppfylt lovlig opphold vilkår hvis EØS borger og har arbeidsforhold`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentStatsborgerskap(aktør, historikk = true) } returns
                listOf(
                    Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null),
                )

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            every { statsborgerskapService.hentSterkesteMedlemskap(Statsborgerskap("BE", LocalDate.now().minusYears(20), null, null)) } returns Medlemskap.EØS

            every { systemOnlyIntegrasjonKlient.hentArbeidsforholdMedSystembruker(any(), LocalDate.now().minusYears(10)) } returns
                listOf(Arbeidsforhold(arbeidsgiver = null, ansettelsesperiode = Ansettelsesperiode(Periode(LocalDate.now().minusYears(10), null))))

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .single { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater.begrunnelse)
                .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- EØS-borger og har arbeidsforhold i Norge.")
        }

        @Test
        fun `skal preutfylle lovlig opphold vilkår hvis oppholdstillatelse`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentOppholdstillatelse(aktør, true) } returns
                listOf(Opphold(OPPHOLDSTILLATELSE.PERMANENT, null, null))

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        }

        @Test
        fun `siste periode skal være løpende om tom er satt på oppholdstillatelse`() {
            // Arrange
            val aktør = randomAktør()
            val vilkårsvurdering =
                lagVilkårsvurdering(
                    lagPersonResultater = {
                        setOf(
                            lagPersonResultat(
                                vilkårsvurdering = it,
                                aktør = aktør,
                                lagVilkårResultater = { emptySet() },
                                lagAnnenVurderinger = { emptySet() },
                            ),
                        )
                    },
                )

            every { pdlRestKlient.hentOppholdstillatelse(aktør, true) } returns
                listOf(Opphold(OPPHOLDSTILLATELSE.MIDLERTIDIG, LocalDate.now().minusYears(5), LocalDate.now().plusYears(5)))

            every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
                val identer = firstArg<List<String>>()
                identer.associateWith {
                    PdlAdresserPerson(
                        bostedsadresser =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(5),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

            // Act
            preutfyllLovligOppholdService.preutfyllLovligOpphold(vilkårsvurdering = vilkårsvurdering)

            // Assert
            val lovligOppholdResultater =
                vilkårsvurdering.personResultater
                    .first { it.aktør == aktør }
                    .vilkårResultater
                    .find { it.vilkårType == Vilkår.LOVLIG_OPPHOLD }

            assertThat(lovligOppholdResultater?.resultat).isEqualTo(Resultat.OPPFYLT)
            assertThat(lovligOppholdResultater?.periodeFom).isEqualTo(LocalDate.now().minusYears(5))
            assertThat(lovligOppholdResultater?.periodeTom).isNull()
        }
    }
}
