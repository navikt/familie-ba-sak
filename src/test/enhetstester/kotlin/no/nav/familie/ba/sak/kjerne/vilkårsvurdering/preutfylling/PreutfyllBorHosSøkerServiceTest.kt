package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllBorHosSøkerServiceTest {
    private val pdlRestKlient: SystemOnlyPdlRestKlient = mockk(relaxed = true)
    private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
    private val preutfyllBorHosSøkerService: PreutfyllBorHosSøkerService = PreutfyllBorHosSøkerService(pdlRestKlient)

    @Test
    fun `skal preutfylle bor fast hos søker vilkår til oppfylt om barn bor på samme adresse som søker`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                overstyrendeVilkårResultater = emptyMap(),
            )

        val nåDato = LocalDate.now()

        every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
            val identer = firstArg<List<String>>()
            identer.associateWith {
                PdlAdresserPerson(
                    bostedsadresse =
                        listOf(
                            Bostedsadresse(
                                gyldigFraOgMed = nåDato.minusYears(10),
                                gyldigTilOgMed = null,
                                vegadresse = lagVegadresse(12345L),
                            ),
                        ),
                    deltBosted = emptyList(),
                )
            }
        }

        // Act
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }
        assertThat(borFastHosSøkerVilkår.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(nåDato.minusYears(10))
        assertThat(borFastHosSøkerVilkår.periodeTom).isNull()
    }

    @Test
    fun `skal preutfylle bor fast hos søker vilkår til ikke oppfylt om barn ikke bor på samme adresse som søker`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()
        val fødselsdatoSøker = LocalDate.now().minusYears(30)
        val fødselsdatoBarn = LocalDate.now().minusYears(10)

        val under18ÅrVilkårResultat = lagVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR, resultat = Resultat.OPPFYLT, periodeFom = fødselsdatoBarn, periodeTom = fødselsdatoBarn.plusYears(18))

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker, fødselsdato = fødselsdatoSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn, fødselsdato = fødselsdatoBarn)),
                overstyrendeVilkårResultater =
                    mapOf(
                        aktørBarn.aktørId to listOf(under18ÅrVilkårResultat),
                    ),
            )

        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }

        every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(identer) } returns
            identer.associateWith { ident ->
                if (ident == aktørSøker.aktivFødselsnummer()) {
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = fødselsdatoSøker,
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                } else {
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = fødselsdatoBarn,
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(98765L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

        // Act
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single { it.vilkårType == Vilkår.BOR_MED_SØKER }

        assertThat(borFastHosSøkerVilkår.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `skal preutfylle bor fast hos søker vilkår til ikke oppfylt om barn har bodd på samme adresse i mindre enn 3 mnd`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()
        val fødselsdatoSøker = LocalDate.now().minusYears(30)
        val fødselsdatoBarn = LocalDate.now().minusYears(5)

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker, fødselsdato = fødselsdatoSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn, fødselsdato = fødselsdatoBarn)),
                overstyrendeVilkårResultater = emptyMap(),
            )

        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }

        every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(identer) } returns
            identer.associateWith { ident ->
                if (ident == aktørSøker.aktivFødselsnummer()) {
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(1),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                } else {
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusMonths(2),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

        // Act
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOR_MED_SØKER }

        assertThat(borFastHosSøkerVilkår).allMatch { it.resultat == Resultat.IKKE_OPPFYLT }
    }

    @Test
    fun `skal gi riktig fom og tom på forskjellige perioder for bor fast hos søker vilkår`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()
        val fødselsdatoSøker = LocalDate.now().minusYears(30)
        val fødselsdatoBarn = LocalDate.now().minusYears(10)
        val behandling = lagBehandling()
        val søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker, fødselsdato = fødselsdatoSøker)
        val barn = lagPerson(type = PersonType.BARN, aktør = aktørBarn, fødselsdato = fødselsdatoBarn)

        val under18ÅrVilkårResultat = lagVilkårResultat(vilkårType = Vilkår.UNDER_18_ÅR, resultat = Resultat.OPPFYLT, periodeFom = fødselsdatoBarn, periodeTom = fødselsdatoBarn.plusYears(18))

        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandlingId = behandling.id, personer = listOf(søker, barn).toTypedArray())

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                behandling = behandling,
                søker = søker,
                barna = listOf(barn),
                overstyrendeVilkårResultater =
                    mapOf(
                        aktørBarn.aktørId to listOf(under18ÅrVilkårResultat),
                    ),
            )

        val identer = vilkårsvurdering.personResultater.map { it.aktør.aktivFødselsnummer() }

        every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(identer) } returns
            identer.associateWith { ident ->
                if (ident == aktørSøker.aktivFødselsnummer()) {
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = fødselsdatoSøker,
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(15),
                                    gyldigTilOgMed = null,
                                    matrikkeladresse = lagMatrikkeladresse(6789L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                } else {
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = fødselsdatoBarn,
                                    gyldigTilOgMed = null,
                                    matrikkeladresse = lagMatrikkeladresse(6789L),
                                ),
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(5),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(98765L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    )
                }
            }

        every { persongrunnlagService.hentAktivThrows(behandlingId = behandling.id) } returns persongrunnlag

        // Act
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOR_MED_SØKER }

        assertThat(borFastHosSøkerVilkår).hasSize(2)
        assertThat(borFastHosSøkerVilkår.first { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(borFastHosSøkerVilkår.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        assertThat(borFastHosSøkerVilkår.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5).minusDays(1))
        assertThat(borFastHosSøkerVilkår.last { it.resultat == Resultat.OPPFYLT }).isNotNull
        assertThat(borFastHosSøkerVilkår.last().periodeFom).isEqualTo(LocalDate.now().minusYears(5))
        assertThat(borFastHosSøkerVilkår.last().periodeTom).isNull()
    }

    @Test
    fun `skal gi riktig begrunnelse for oppfylt bor fast hos søker vilkår`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                overstyrendeVilkårResultater = emptyMap(),
            )

        every { pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(any()) } answers {
            val identer = firstArg<List<String>>()
            identer.associateWith {
                PdlAdresserPerson(
                    bostedsadresse =
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
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.begrunnelse)
            .isEqualTo("$PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT- Har samme bostedsadresse som søker.")
    }

    @Test
    fun `Skal filtrere vekk perioder før barn har sin første adresse i Norge`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                overstyrendeVilkårResultater = emptyMap(),
            )

        every {
            pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(
                any(),
            )
        } answers {
            mapOf(
                aktørSøker.aktivFødselsnummer() to
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    ),
                aktørBarn.aktivFødselsnummer() to
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(2),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    ),
            )
        }

        // Act
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(LocalDate.now().minusYears(2))
    }

    @Test
    fun `Skal filtrere vekk perioder før søker har sin første adresse i Norge`() {
        // Arrange
        val aktørSøker = randomAktør()
        val aktørBarn = randomAktør()

        val vilkårsvurdering =
            lagVilkårsvurderingMedOverstyrendeResultater(
                søker = lagPerson(type = PersonType.SØKER, aktør = aktørSøker),
                barna = listOf(lagPerson(type = PersonType.BARN, aktør = aktørBarn)),
                overstyrendeVilkårResultater = emptyMap(),
            )

        every {
            pdlRestKlient.hentBostedsadresseOgDeltBostedForPersoner(
                any(),
            )
        } answers {
            mapOf(
                aktørSøker.aktivFødselsnummer() to
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(2),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    ),
                aktørBarn.aktivFødselsnummer() to
                    PdlAdresserPerson(
                        bostedsadresse =
                            listOf(
                                Bostedsadresse(
                                    gyldigFraOgMed = LocalDate.now().minusYears(10),
                                    gyldigTilOgMed = null,
                                    vegadresse = lagVegadresse(12345L),
                                ),
                            ),
                        deltBosted = emptyList(),
                    ),
            )
        }

        // Act
        preutfyllBorHosSøkerService.preutfyllBorFastHosSøkerVilkårResultat(vilkårsvurdering)

        // Assert
        val borFastHosSøkerVilkår =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktørBarn }
                .vilkårResultater
                .single {
                    it.vilkårType == Vilkår.BOR_MED_SØKER
                }

        assertThat(borFastHosSøkerVilkår.periodeFom).isEqualTo(LocalDate.now().minusYears(2))
    }
}
