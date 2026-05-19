package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagAktør
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagGrUkjentBostedBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårIkkeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.personident.Personident
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllBosattIRiketServiceTest {
    private val søknadService: SøknadService = mockk(relaxed = true)
    private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)

    private val preutfyllBosattIRiketService =
        PreutfyllBosattIRiketService(
            søknadService = søknadService,
            persongrunnlagService = persongrunnlagService,
        )

    @Test
    fun `skal lage preutfylt vilkårresultat basert på data fra pdl`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also {
                    it.barna.forEach { barn ->
                        barn.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusYears(4),
                                            tom = LocalDate.now().minusYears(3),
                                        ),
                                    matrikkelId = 12345L,
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusYears(3).plusDays(1),
                                            tom = LocalDate.now().minusYears(2),
                                        ),
                                    matrikkelId = 54321L,
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusYears(1),
                                            tom = null,
                                        ),
                                    matrikkelId = 98765L,
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusMonths(6),
                                            tom = null,
                                        ),
                                    matrikkelId = 98764L,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør))
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()

        assertThat(vilkårResultat).hasSize(3)
        assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(4))
        assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(2))
        assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
            assertThat(it.erOpprinneligPreutfyltIBehandling).isEqualTo(behandling.id)
        }
    }

    @Test
    fun `skal gi vilkårsresultat IKKE_OPPFYLT hvis bosatt i riket er mindre enn 12 mnd`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr())).also { grunnlag ->
                grunnlag.personer.forEach { person ->
                    person.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.now().minusMonths(6),
                                        tom = LocalDate.now().minusMonths(4),
                                    ),
                                matrikkelId = 12345L,
                            ),
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.now().minusMonths(4).plusDays(1),
                                        tom = LocalDate.now().minusMonths(2),
                                    ),
                                matrikkelId = 54321L,
                            ),
                        )
                }
            }
        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = persongrunnlag.barna.first().aktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        vilkårsvurdering.personResultater.forEach { personResultat ->
            val bosattIRiketVilkårResultat = personResultat.vilkårResultater.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
            assertThat(bosattIRiketVilkårResultat.first().resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
            assertThat(bosattIRiketVilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
            assertThat(bosattIRiketVilkårResultat).allSatisfy {
                assertThat(it.begrunnelseForManuellKontroll).isNull()
            }
        }
    }

    @Test
    fun `skal filterer bort adresser hvor fom og tom er null`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
                .also { grunnlag ->
                    grunnlag.personer.forEach { person ->
                        person.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.of(1980, 1, 1),
                                            tom = null,
                                        ),
                                    matrikkelId = 1L,
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = null,
                                            tom = null,
                                        ),
                                    matrikkelId = 2L,
                                ),
                            )
                    }
                }
        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = persongrunnlag.barna.first().aktør))
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .first()
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultat).hasSize(1)
        assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `skal ikke gi oppfylt i siste periode hvis den er under 12 mnd og søker ikke planlegger å bo i Norge neste 12`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also {
                    it.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
                    it.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            LocalDate.now().minusMonths(6),
                                            LocalDate.now().minusMonths(4),
                                        ),
                                    matrikkelId = 12345L,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør),
                    )
            }
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false)

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal gi oppfylt i siste periode hvis den er under 12 mnd og søker planlegger å bo i Norge neste 12`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2)),
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
            ).also {
                it.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    it.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode = DatoIntervallEntitet(LocalDate.now().minusMonths(8), null),
                                matrikkelId = 12345L,
                            ),
                        )
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = persongrunnlag.søker.aktør))
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = true)

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.erSøkersResultater() }
                ?.vilkårResultater
                ?.find { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!
        assertThat(vilkårResultat.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultat.begrunnelseForManuellKontroll).isEqualTo(INFORMASJON_FRA_SØKNAD)
    }

    @Test
    fun `skal gi oppfylt hvis søker planlegger å bo i Norge neste 12 månedene`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2)),
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            ).also {
                it.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    it.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode = DatoIntervallEntitet(LocalDate.now().minusMonths(2), null),
                                matrikkelId = 12345L,
                            ),
                        )
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = lagPerson(personIdent = PersonIdent(barnAktør.aktivFødselsnummer()), type = PersonType.BARN),
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.now().minusMonths(2),
                            periodeTom = null,
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            erDeltBosted = false,
                            erDeltBostedSkalIkkeDeles = false,
                            erEksplisittAvslagPåSøknad = false,
                        ),
                    )
            }
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false, barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnAktør.aktivFødselsnummer() to true))

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.first { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!
        assertThat(vilkårResultat.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultat.begrunnelseForManuellKontroll).isEqualTo(INFORMASJON_FRA_SØKNAD)
    }

    @Test
    fun `ved ingen digital søknad skal vurdering settes til ikke vurdert ved adresse som ikke har vart lengre enn 12 mnd omfatter fødselsdato`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2)),
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                barnAktør = listOf(barnAktør),
            ).also {
                it.personer.forEach {
                    it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                    it.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode = DatoIntervallEntitet(LocalDate.now().minusMonths(2), null),
                                matrikkelId = 12345L,
                            ),
                        )
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            person = lagPerson(personIdent = PersonIdent(barnAktør.aktivFødselsnummer()), type = PersonType.BARN),
                            resultat = Resultat.OPPFYLT,
                            periodeFom = LocalDate.now().minusMonths(2),
                            periodeTom = null,
                            lagFullstendigVilkårResultat = true,
                            personType = PersonType.BARN,
                            vilkårType = Vilkår.BOSATT_I_RIKET,
                            erDeltBosted = false,
                            erDeltBostedSkalIkkeDeles = false,
                            erEksplisittAvslagPåSøknad = false,
                        ),
                    )
            }
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        every { søknadService.finnDigitalSøknad(behandling.id) } returns null

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.first { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!
        assertThat(vilkårResultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
        assertThat(vilkårResultat.evalueringÅrsaker.first()).isEqualTo(VilkårKanskjeOppfyltÅrsak.BOSATT_I_RIKET_IKKE_MULIG_Å_FASTSETTE_SKAL_BO_LENGRE_ENN_12_MND.hentNavn())
    }

    @Test
    fun `skal ikke ta med perioder før angitt dato`() {
        // Arrange
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val behandling = lagBehandling()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(10)),
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        LocalDate.now().minusYears(20),
                                        LocalDate.now().minusYears(15),
                                    ),
                                matrikkelId = 12345L,
                            ),
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        LocalDate.now().minusYears(12),
                                        LocalDate.now().minusYears(5),
                                    ),
                                matrikkelId = 54321L,
                            ),
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        LocalDate.now().minusYears(1),
                                        null,
                                    ),
                                matrikkelId = 98765L,
                            ),
                        )
                }
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = persongrunnlag.søker.aktør),
                    )
            }

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.erSøkersResultater() }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()
        assertThat(vilkårResultat).hasSize(3)
        assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5))
        assertThat(vilkårResultat.first().vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)

        assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal gi oppfylt på 12 måneders krav for nordiske statsborgere`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(10)),
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(
                                landkode = "DNK",
                                gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                id = 0,
                                person = it,
                            ),
                        )
                    it.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode = DatoIntervallEntitet(LocalDate.now().minusMonths(12), LocalDate.now().minusMonths(5)),
                                matrikkelId = 12345L,
                            ),
                            lagGrVegadresseBostedsadresse(
                                periode = DatoIntervallEntitet(LocalDate.now().minusMonths(1), null),
                                matrikkelId = 12345L,
                            ),
                        )
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = persongrunnlag.søker.aktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad()

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat = vilkårsvurdering.personResultater.find { it.erSøkersResultater() }?.vilkårResultater ?: emptyList()
        val ikkeOppfyltPeriode = vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }
        assertThat(ikkeOppfyltPeriode).`as`("Forventer én IKKE_OPPFYLT periode").isNotNull
        assertThat(ikkeOppfyltPeriode?.periodeFom).`as`("Ikke oppfylt periode fom").isEqualTo(LocalDate.now().minusMonths(5).plusDays(1))
        assertThat(ikkeOppfyltPeriode?.periodeTom).`as`("Ikke oppfylt periode tom").isEqualTo(LocalDate.now().minusMonths(1).minusDays(1))

        val oppfyltPeriode1 = vilkårResultat.find { it.periodeFom == LocalDate.now().minusMonths(12) }
        assertThat(oppfyltPeriode1?.resultat).`as`("Forventer OPPFYLT periode for 12 måneder siden").isEqualTo(Resultat.OPPFYLT)
        assertThat(oppfyltPeriode1?.periodeTom).`as`("Oppfylt periode for 12 måneder siden tom").isEqualTo(LocalDate.now().minusMonths(5))

        val oppfyltPeriode2 = vilkårResultat.find { it.periodeFom == LocalDate.now().minusMonths(1) }
        assertThat(oppfyltPeriode2?.resultat).`as`("Forventer OPPFYLT periode for 1 måned siden").isEqualTo(Resultat.OPPFYLT)
        assertThat(oppfyltPeriode2?.periodeTom).`as`("Oppfylt periode for 1 måned siden tom").isNull()

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal gi oppfylt på 12 måneders krav for nordiske statsborgere selv hvis det er dobbelt statsborgerskap`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
            ).also { grunnlag ->
                grunnlag.personer.forEach {
                    it.statsborgerskap =
                        mutableListOf(
                            GrStatsborgerskap(landkode = "DNK", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null), person = it),
                            GrStatsborgerskap(landkode = "NOR", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(1), tom = null), person = it),
                            GrStatsborgerskap(landkode = "AUT", gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(5), tom = LocalDate.now().minusYears(1)), person = it),
                        )
                    it.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        LocalDate.now().minusYears(5),
                                        LocalDate.now().minusYears(4).minusMonths(2),
                                    ),
                                matrikkelId = 12345L,
                            ),
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        LocalDate.now().minusYears(1),
                                        null,
                                    ),
                                matrikkelId = 12345L,
                            ),
                        )
                }
            }
        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = persongrunnlag.søker.aktør))
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        every { søknadService.finnDigitalSøknad(behandling.id) } returns lagSøknad()

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.erSøkersResultater() }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()
        val ikkeOppfyltPeriode = vilkårResultat.filter { it.resultat == Resultat.IKKE_OPPFYLT }
        assertThat(ikkeOppfyltPeriode.size).isEqualTo(2)

        val oppfyltPeriode = vilkårResultat.find { it.resultat == Resultat.OPPFYLT }
        assertThat(oppfyltPeriode).`as`("Forventer én OPPFYLT periode").isNotNull
        assertThat(oppfyltPeriode?.periodeFom).`as`("Oppfylt periode fom").isEqualTo(LocalDate.now().minusYears(1))
        assertThat(oppfyltPeriode?.periodeTom).`as`("Oppfylt periode tom").isNull()
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal oppfylle vilkår hvis barn er født i Norge og er bosatt i Norge i mindre enn 12 mnd`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also {
                    it.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(2)),
                                    matrikkelId = 12345L,
                                ),
                            )
                    }
                }
        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør))
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()
        assertThat(vilkårResultat).hasSize(1)

        val barnsVilkårResultat = vilkårResultat.singleOrNull()
        assertThat(barnsVilkårResultat?.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal gi begrunnelse Norsk bostedsadresse i 12 måneder for oppfylt periode`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            LocalDate.now().minusYears(4),
                                            LocalDate.now().minusYears(3),
                                        ),
                                    matrikkelId = 12345L,
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            LocalDate.now().minusYears(2).plusDays(1),
                                            null,
                                        ),
                                    matrikkelId = 54321L,
                                ),
                            )
                    }
                }
        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()
        val begrunnelse = vilkårResultat.firstOrNull { it.resultat == Resultat.OPPFYLT }?.begrunnelse

        assertThat(begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT + "- Norsk bostedsadresse i minst 12 måneder.")
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal ikke gi oppfylt delvilkår dersom det eksisterer opphold samtidig som man har adresse i norge for oppfylt periode når behandlingsårsak ikke er fødselshendelse`() {
        // Arrange
        val behandling = lagBehandling(årsak = BehandlingÅrsak.SØKNAD)
        val barnAktør = lagAktør()
        val søkerPersonIdent = randomFnr()

        val søkerAktør =
            lagAktør(søkerPersonIdent).also {
                it.personidenter.add(
                    Personident(
                        fødselsnummer = søkerPersonIdent,
                        aktør = it,
                        aktiv = søkerPersonIdent == it.personidenter.first().fødselsnummer,
                    ),
                )
            }

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = søkerPersonIdent, barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør), søkerAktør = søkerAktør)
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusMonths(5),
                                        ),
                                    matrikkelId = 12345L,
                                ),
                            )
                        it.opphold =
                            mutableListOf(
                                GrOpphold(
                                    type = OPPHOLDSTILLATELSE.PERMANENT,
                                    gyldigPeriode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusMonths(6),
                                        ),
                                    person = it,
                                ),
                            )
                    }
                }
        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør),
                        lagPersonResultat(vilkårsvurdering = it, aktør = søkerAktør),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val søkerBosattIRiketVilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == søkerAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }!!
                .single()

        assertThat(søkerBosattIRiketVilkårResultat.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)

        assertThat(søkerBosattIRiketVilkårResultat.evalueringÅrsaker).containsExactly(VilkårIkkeOppfyltÅrsak.HAR_IKKE_BODD_I_RIKET_12_MND.name)
    }

    @Test
    fun `skal gi begrunnelse Bosatt i Norge siden fødsel for oppfylt periode`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(5)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList()
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode =
                                        DatoIntervallEntitet(
                                            LocalDate.now().minusMonths(10),
                                            null,
                                        ),
                                    matrikkelId = 12345L,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling).also {
                lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET } ?: emptyList()
        val begrunnelse = vilkårResultat.firstOrNull { it.resultat == Resultat.OPPFYLT }?.begrunnelse
        assertThat(begrunnelse).isEqualTo(
            PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT +
                "- Bosatt i Norge siden fødsel.",
        )
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom vegadresse er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(2)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "5601",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf<GrStatsborgerskap>(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering dersom vegadresse er på svalbard`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    matrikkelId = 12345L,
                                ),
                            )
                        it.oppholdsadresser =
                            mutableListOf(
                                lagGrVegadresseOppholdsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(13)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "2100",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf<GrStatsborgerskap>(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering dersom vegadresse er på svalbard selv uten bostedsadresse`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.oppholdsadresser =
                            mutableListOf(
                                lagGrVegadresseOppholdsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(13)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "2100",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf<GrStatsborgerskap>(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `Skal bare ta hensyn til perioder i svalbard når vi sjekker på oppholdsadresse`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.oppholdsadresser =
                            mutableListOf(
                                lagGrVegadresseOppholdsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(12)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "2100",
                                ),
                                lagGrVegadresseOppholdsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(13), LocalDate.now().minusMonths(3)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "0001",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf<GrStatsborgerskap>(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom matrikkeladresse er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "5601",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf<GrStatsborgerskap>(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom ukjent bosted er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrUkjentBostedBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    bostedskommune = "5601",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isTrue()
        }
    }

    @Test
    fun `Skal ikke automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom matrikkeladresse ikke er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    kommunenummer = "404",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isFalse()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom vilkår er oppfylt basert på øvrige vilkår`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    kommunenummer = "5601",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "VNM",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal oppfylle vilkår hvis barn har to delt bosted-adresser med lik fom og tom i Finnmark`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(3)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.deltBosted =
                            mutableListOf(
                                lagGrVegadresseDeltBosted(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusYears(2)),
                                    kommunenummer = "5601",
                                ),
                                lagGrVegadresseDeltBosted(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusYears(2)),
                                    kommunenummer = "5603",
                                ),
                            )
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(12)),
                                    kommunenummer = "0301",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).isNotEmpty
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal prioritere adresse i Finnmark ved like perioder og automatisk sette bosatt i finnmark i utdypendevilkårsvurdering`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val periodeFom = LocalDate.now().minusYears(2)
        val periodeTom = null

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(3)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(periodeFom, periodeTom),
                                    kommunenummer = "5601",
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(periodeFom, periodeTom),
                                    kommunenummer = "0301",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).hasSize(1)
        assertThat(vilkårResultat?.single()?.periodeFom).isEqualTo(periodeFom)
        assertThat(vilkårResultat?.single()?.periodeTom).isNull()
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal justere tom-dato hvis den er lik fom-dato på neste adresse`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val dato1 = LocalDate.now().minusYears(3)
        val dato2 = LocalDate.now().minusYears(1)

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(3)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(dato1, dato2),
                                    kommunenummer = "5601",
                                ),
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(dato2, null),
                                    kommunenummer = "0301",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).hasSize(2)

        val førstePeriode = vilkårResultat?.first()
        assertThat(førstePeriode?.periodeFom).isEqualTo(dato1)
        assertThat(førstePeriode?.periodeTom).isEqualTo(dato2.minusDays(1))

        val andrePeriode = vilkårResultat?.last()

        assertThat(andrePeriode?.periodeFom).isEqualTo(dato2)
        assertThat(andrePeriode?.periodeTom).isNull()
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering dersom vilkår er oppfylt basert på øvrige vilkår`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    matrikkelId = 12345L,
                                ),
                            )
                        it.oppholdsadresser =
                            mutableListOf(
                                lagGrVegadresseOppholdsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(4)),
                                    kommunenummer = "2100",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "VNM",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }

        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
        }
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering om person har bostedsadresse i finnmark og oppholdsadresse på svalbard`() {
        // Arrange
        val behandling = lagBehandling()
        val barnAktør = lagAktør()

        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(barnAktør.aktivFødselsnummer()), barnAktør = listOf(barnAktør))
                .also { grunnlag ->
                    grunnlag.personer.forEach {
                        it.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusYears(4)),
                                    kommunenummer = "5601",
                                ),
                            )
                        it.oppholdsadresser =
                            mutableListOf(
                                lagGrVegadresseOppholdsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusMonths(13)),
                                    matrikkelId = 12345L,
                                    kommunenummer = "2100",
                                ),
                            )
                        it.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "NOR",
                                    gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                                    person = it,
                                ),
                            )
                    }
                }

        val vilkårsvurdering =
            lagVilkårsvurdering(persongrunnlag, behandling)
                .also { it.personResultater = setOf(lagPersonResultat(vilkårsvurdering = it, aktør = barnAktør)) }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val vilkårResultat =
            vilkårsvurdering.personResultater
                .find { it.aktør == barnAktør }
                ?.vilkårResultater
                ?.filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `skal ikke preutfylle bosatt i riket om personen har ukrainsk statsborgerskap`() {
        // Arrange
        val aktør = randomAktør()
        val persongrunnlag =
            lagPersonopplysningGrunnlag {
                setOf(
                    lagPerson(
                        personIdent = PersonIdent(aktør.aktivFødselsnummer()),
                        aktør = aktør,
                    ).also { person ->
                        person.bostedsadresser =
                            mutableListOf(
                                lagGrVegadresseBostedsadresse(
                                    periode = DatoIntervallEntitet(LocalDate.now().minusYears(1)),
                                    matrikkelId = 12345L,
                                ),
                            )
                        person.statsborgerskap =
                            mutableListOf(
                                GrStatsborgerskap(
                                    landkode = "UKR",
                                    gyldigPeriode =
                                        DatoIntervallEntitet(
                                            fom = LocalDate.now().minusYears(10),
                                            tom = null,
                                        ),
                                    person = person,
                                ),
                            )
                    },
                )
            }

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

        every { persongrunnlagService.hentAktivThrows(vilkårsvurdering.behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering = vilkårsvurdering, aktørerVilkårSkalPreutfyllesFor = vilkårsvurdering.personResultater.map { it.aktør })

        // Assert
        val bosattIRiketResultater =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktør }
                .vilkårResultater

        assertThat(bosattIRiketResultater).isEmpty()
    }

    @Test
    fun `skal kun preutfylle bosatt i riket for aktører i aktørerVilkårSkalPreutfyllesFor`() {
        // Arrange
        val behandling = lagBehandling()
        val søkerAktør = randomAktør()
        val barnAktør = randomAktør()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerAktør.aktivFødselsnummer(),
                barnasIdenter = listOf(barnAktør.aktivFødselsnummer()),
                søkerAktør = søkerAktør,
                barnAktør = listOf(barnAktør),
            ).also {
                it.personer.forEach { person ->
                    person.bostedsadresser =
                        mutableListOf(
                            lagGrVegadresseBostedsadresse(
                                periode =
                                    DatoIntervallEntitet(
                                        fom = LocalDate.now().minusYears(5),
                                        tom = null,
                                    ),
                                matrikkelId = 12345L,
                            ),
                        )
                }
            }

        val vilkårsvurdering =
            lagVilkårsvurdering(behandling = behandling).also {
                it.personResultater =
                    setOf(
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = søkerAktør,
                            lagVilkårResultater = { emptySet() },
                            lagAnnenVurderinger = { emptySet() },
                        ),
                        lagPersonResultat(
                            vilkårsvurdering = it,
                            aktør = barnAktør,
                            lagVilkårResultater = { emptySet() },
                            lagAnnenVurderinger = { emptySet() },
                        ),
                    )
            }

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(
            vilkårsvurdering = vilkårsvurdering,
            aktørerVilkårSkalPreutfyllesFor = listOf(barnAktør),
        )

        // Assert
        val søkerBosattIRiket =
            vilkårsvurdering.personResultater
                .first { it.aktør == søkerAktør }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(søkerBosattIRiket).isEmpty()

        val barnBosattIRiket =
            vilkårsvurdering.personResultater
                .first { it.aktør == barnAktør }
                .vilkårResultater
                .filter { it.vilkårType == Vilkår.BOSATT_I_RIKET }
        assertThat(barnBosattIRiket).isNotEmpty
        assertThat(barnBosattIRiket).allMatch { it.erAutomatiskVurdert }
    }
}
