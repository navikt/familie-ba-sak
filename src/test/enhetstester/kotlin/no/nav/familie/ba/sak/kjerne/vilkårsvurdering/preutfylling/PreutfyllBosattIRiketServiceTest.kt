package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggle
import no.nav.familie.ba.sak.config.featureToggle.FeatureToggleService
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagAdresse
import no.nav.familie.ba.sak.datagenerator.lagAdresser
import no.nav.familie.ba.sak.datagenerator.lagAndelTilkjentYtelse
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.lagVilkårResultat
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.randomAktør
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.SystemOnlyPdlRestKlient
import no.nav.familie.ba.sak.integrasjoner.pdl.domene.PdlAdresserPerson
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.Adresser
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.bostedsadresse.GrBostedsadresse
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.UtdypendeVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.PreutfyllVilkårService.Companion.PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT
import no.nav.familie.kontrakter.ba.finnmarkstillegg.KommunerIFinnmarkOgNordTroms
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import no.nav.familie.kontrakter.felles.personopplysning.UkjentBosted
import no.nav.familie.kontrakter.felles.svalbard.SvalbardKommune
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class PreutfyllBosattIRiketServiceTest {
    private val systemOnlyPdlRestKlient: SystemOnlyPdlRestKlient = mockk(relaxed = true)
    private val søknadService: SøknadService = mockk(relaxed = true)
    private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
    private val featureToggleService = mockk<FeatureToggleService>()
    private val andelTilkjentYtelseRepository = mockk<AndelTilkjentYtelseRepository>()

    private val preutfyllBosattIRiketService =
        PreutfyllBosattIRiketService(
            pdlRestKlient = systemOnlyPdlRestKlient,
            søknadService = søknadService,
            persongrunnlagService = persongrunnlagService,
            featureToggleService = featureToggleService,
            andelTilkjentYtelseRepository = andelTilkjentYtelseRepository,
        )

    @BeforeEach
    fun setup() {
        every { featureToggleService.isEnabled(FeatureToggle.NY_PREUTFYLLING_FOR_BOSATT_I_RIKET_VILKÅR_VED_AUTOVEDTAK_FINNMARK_SVALBARD) } returns true
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(any(), any()) } returns emptyList()
    }

    @Test
    fun `skal lage preutfylt vilkårresultat basert på data fra pdl`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.single().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(4),
                            gyldigTilOgMed = LocalDate.now().minusYears(3),
                            vegadresse = lagVegadresse(12345L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(3).plusDays(1),
                            gyldigTilOgMed = LocalDate.now().minusYears(2),
                            matrikkeladresse = lagMatrikkeladresse(54321L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(1),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(98765L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(6),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(98764L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).hasSize(3)
        assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(4))
        assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(2))
        assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårResultat.first().vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal ikke få noen vilkårresultat når pdl ikke returnerer noen bostedsadresser`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        persongrunnlag.søkerOgBarn.forEach { barn -> barn.bostedsadresser = emptyList<GrBostedsadresse>().toMutableList() }

        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.single().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns emptyList()

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson =
                    Adresser(
                        bostedsadresser = emptyList(),
                        delteBosteder = emptyList(),
                        oppholdsadresse = emptyList(),
                    ),
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).hasSize(1)
        assertThat(vilkårResultat).allSatisfy { it.resultat == Resultat.IKKE_OPPFYLT }
    }

    @Test
    fun `skal gi vilkårsresultat IKKE_OPPFYLT hvis bosatt i riket er mindre enn 12 mnd`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(6),
                            gyldigTilOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(12345L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4).plusDays(1),
                            gyldigTilOgMed = LocalDate.now().minusMonths(2),
                            matrikkeladresse = lagMatrikkeladresse(54321L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal filterer bort adresser hvor fom og tom er null`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.of(1980, 1, 1),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(1L),
                        ),
                        Adresse(
                            gyldigFraOgMed = null,
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(2L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).hasSize(1)
        assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `skal ikke gi oppfylt i siste periode hvis den er under 12 mnd og søker ikke planlegger å bo i Norge neste 12`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(6),
                            gyldigTilOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false)

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
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
            )
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(8),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = true)

        // Act
        val vilkårResultater =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(2),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        val vilkårResultat = vilkårResultater.single()
        assertThat(vilkårResultat.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultat.begrunnelseForManuellKontroll).isEqualTo(INFORMASJON_FRA_SØKNAD)
    }

    @Test
    fun `skal gi oppfylt hvis søker planlegger å bo i Norge neste 12 månedene`() {
        // Arrange
        val behandling = lagBehandling()
        val barnFnr = randomFnr()
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                barnasFødselsdatoer = listOf(LocalDate.now().minusYears(2)),
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(barnFnr),
            )
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat =
            lagPersonResultat(
                vilkårsvurdering = vilkårsvurdering,
                person = lagPerson(personIdent = PersonIdent(barnFnr), type = PersonType.BARN),
                resultat = Resultat.OPPFYLT,
                periodeFom = LocalDate.now().minusMonths(2),
                periodeTom = null,
                lagFullstendigVilkårResultat = true,
                personType = PersonType.BARN,
                vilkårType = Vilkår.BOSATT_I_RIKET,
                erDeltBosted = false,
                erDeltBostedSkalIkkeDeles = false,
                erEksplisittAvslagPåSøknad = false,
            )

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(2),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false, barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnFnr to true))

        // Act
        val vilkårResultater =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(2),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        val vilkårResultat = vilkårResultater.single()
        assertThat(vilkårResultat.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultat.begrunnelseForManuellKontroll).isEqualTo(INFORMASJON_FRA_SØKNAD)
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
            )

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(20),
                            gyldigTilOgMed = LocalDate.now().minusYears(15),
                            vegadresse = lagVegadresse(12345L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(12),
                            gyldigTilOgMed = LocalDate.now().minusYears(5),
                            matrikkeladresse = lagMatrikkeladresse(54321L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(1),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(98765L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(10),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
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
            )

        persongrunnlag.personer.forEach {
            it.statsborgerskap =
                mutableListOf(
                    GrStatsborgerskap(
                        landkode = "DNK",
                        gyldigPeriode = DatoIntervallEntitet(fom = LocalDate.now().minusYears(3), tom = null),
                        id = 0,
                        person = it,
                    ),
                )
        }

        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(12),
                            gyldigTilOgMed = LocalDate.now().minusMonths(5),
                            vegadresse = lagVegadresse(12345L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(1),
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad()

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(10),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
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
            )
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(5),
                            gyldigTilOgMed = LocalDate.now().minusYears(4).minusMonths(2),
                            vegadresse = lagVegadresse(12345L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(1),
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad()

        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "DNK", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(1), gyldigTilOgMed = null, bekreftelsesdato = null),
                Statsborgerskap(land = "AUT", gyldigFraOgMed = LocalDate.now().minusYears(5), gyldigTilOgMed = LocalDate.now().minusYears(1), bekreftelsesdato = null),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(5),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        val ikkeOppfyltPeriode = vilkårResultat.filter { it.resultat == Resultat.IKKE_OPPFYLT }
        assertThat(ikkeOppfyltPeriode.size).isEqualTo(1)

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
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(2),
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusMonths(2),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).hasSize(1)
        val barnsVilkårResultat = vilkårResultat.find { it.personResultat?.id == personResultat.id }
        assertThat(barnsVilkårResultat?.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal gi begrunnelse Norsk bostedsadresse i 12 måneder for oppfylt periode`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(4),
                            gyldigTilOgMed = LocalDate.now().minusYears(3),
                            vegadresse = lagVegadresse(12345L),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(2).plusDays(1),
                            matrikkeladresse = lagMatrikkeladresse(54321L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        val begrunnelse = vilkårResultat.firstOrNull { it.resultat == Resultat.OPPFYLT }?.begrunnelse
        assertThat(vilkårResultat).hasSize(3)
        assertThat(begrunnelse).isEqualTo(
            PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT +
                "- Norsk bostedsadresse i minst 12 måneder.",
        )
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal gi begrunnelse Bosatt i Norge siden fødsel for oppfylt periode`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        persongrunnlag.personer.forEach { it.statsborgerskap = emptyList<GrStatsborgerskap>().toMutableList() }

        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
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
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(13),
                            vegadresse = lagVegadresse(matrikkelId = 12345L, kommunenummer = "5601"),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), kalkulertUtbetalingsbeløp = 1000))

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering dersom vegadresse er på svalbard`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(4),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(13),
                            vegadresse = lagVegadresse(matrikkelId = 12345L, kommunenummer = "2100"),
                        ),
                    ),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering dersom vegadresse er på svalbard selv uten bostedsadresse`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser = emptyList(),
                delteBosteder = emptyList(),
                oppholdsadresse =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(13),
                            vegadresse = lagVegadresse(matrikkelId = 12345L, kommunenummer = "2100"),
                        ),
                    ),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `Skal bare ta hensyn til perioder i svalbard når vi sjekker på oppholdsadresse`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        val bostedsadresser =
            Adresser(
                bostedsadresser = emptyList(),
                delteBosteder = emptyList(),
                oppholdsadresse =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(12),
                            vegadresse = lagVegadresse(matrikkelId = 12345L, kommunenummer = "2100"),
                        ),
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(13),
                            gyldigTilOgMed = LocalDate.now().minusMonths(3),
                            vegadresse = lagVegadresse(matrikkelId = 12345L, kommunenummer = "0001"),
                        ),
                    ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom matrikkeladresse er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            matrikkeladresse = lagMatrikkeladresse(matrikkelId = 12345L, kommunenummer = "5601"),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isTrue()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom ukjent bosted er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            ukjentBosted = UkjentBosted(bostedskommune = "5601"),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns emptyList()

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isTrue()
        }
    }

    @Test
    fun `Skal ikke automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom matrikkeladresse ikke er i en av de relevante kommunene`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(kommunenummer = "404"),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns emptyList()

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)).isFalse()
        }
    }

    @Test
    fun `Skal automatisk sette bosatt i finnmark i utdypendevilkårsvurdering dersom vilkår er oppfylt basert på øvrige vilkår`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "VNM", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(kommunenummer = "5601"),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal oppfylle vilkår hvis barn har to delt bosted-adresser med lik fom og tom i Finnmark`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(3)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        val deltBostedAdresse1 =
            Adresse(
                gyldigFraOgMed = LocalDate.now().minusYears(2),
                vegadresse = lagVegadresse(kommunenummer = "5601"),
            )
        val deltBostedAdresse2 =
            Adresse(
                gyldigFraOgMed = LocalDate.now().minusYears(2),
                vegadresse = lagVegadresse(kommunenummer = "5603"),
            )

        val bostedadresse =
            listOf(
                Adresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(12),
                    vegadresse = lagVegadresse(kommunenummer = "0301"),
                ),
            )

        val adresser =
            Adresser(
                bostedsadresser = bostedadresse,
                delteBosteder = listOf(deltBostedAdresse1, deltBostedAdresse2),
                oppholdsadresse = emptyList(),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(3),
                adresserForPerson = adresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).isNotEmpty
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal prioritere adresse i Finnmark ved like perioder og automatisk sette bosatt i finnmark i utdypendevilkårsvurdering`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(3)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val periodeFom = LocalDate.now().minusYears(2)
        val periodeTom = null

        val adresseFinnmark =
            Adresse(
                gyldigFraOgMed = periodeFom,
                gyldigTilOgMed = periodeTom,
                vegadresse = lagVegadresse(kommunenummer = "5601"),
            )

        val adresseOslo =
            Adresse(
                gyldigFraOgMed = periodeFom,
                gyldigTilOgMed = periodeTom,
                vegadresse = lagVegadresse(kommunenummer = "0301"),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser = listOf(adresseOslo, adresseFinnmark),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns emptyList()

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).hasSize(1)
        assertThat(vilkårResultat.single().periodeFom).isEqualTo(periodeFom)
        assertThat(vilkårResultat.single().periodeTom).isNull()
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
        }
    }

    @Test
    fun `skal justere tom-dato hvis den er lik fom-dato på neste adresse`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusYears(3)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        val dato1 = LocalDate.now().minusYears(3)
        val dato2 = LocalDate.now().minusYears(1)

        val adresse1 =
            Adresse(
                gyldigFraOgMed = dato1,
                gyldigTilOgMed = dato2,
                vegadresse = lagVegadresse(kommunenummer = "5601"),
            )

        val adresse2 =
            Adresse(
                gyldigFraOgMed = dato2,
                gyldigTilOgMed = null,
                vegadresse = lagVegadresse(kommunenummer = "0301"),
            )

        val bostedsadresser =
            Adresser(
                bostedsadresser = listOf(adresse1, adresse2),
                delteBosteder = emptyList(),
                oppholdsadresse = emptyList(),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns emptyList()

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).hasSize(2)

        val førstePeriode = vilkårResultat.first()
        assertThat(førstePeriode.periodeFom).isEqualTo(dato1)
        assertThat(førstePeriode.periodeTom).isEqualTo(dato2.minusDays(1))

        val andrePeriode = vilkårResultat.last()

        assertThat(andrePeriode.periodeFom).isEqualTo(dato2)
        assertThat(andrePeriode.periodeTom).isNull()
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering dersom vilkår er oppfylt basert på øvrige vilkår`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "VNM", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )
        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(12345L),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(4),
                            vegadresse = lagVegadresse(kommunenummer = "2100"),
                        ),
                    ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.utdypendeVilkårsvurderinger).contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
        }
    }

    @Test
    fun `Skal automatisk sette bosatt på svalbard i utdypendevilkårsvurdering om person har bostedsadresse i finnmark og oppholdsadresse på svalbard`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, barnasFødselsdatoer = listOf(LocalDate.now().minusMonths(2)), søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "NOR", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, personResultat.aktør) } returns listOf(lagAndelTilkjentYtelse(fom = YearMonth.now().minusYears(4), tom = YearMonth.now().plusYears(5), differanseberegnetPeriodebeløp = 1000))

        val bostedsadresser =
            Adresser(
                bostedsadresser =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusYears(4),
                            gyldigTilOgMed = null,
                            vegadresse = lagVegadresse(kommunenummer = "5601"),
                        ),
                    ),
                delteBosteder = emptyList(),
                oppholdsadresse =
                    listOf(
                        Adresse(
                            gyldigFraOgMed = LocalDate.now().minusMonths(13),
                            vegadresse = lagVegadresse(matrikkelId = 12345L, kommunenummer = "2100"),
                        ),
                    ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                adresserForPerson = bostedsadresser,
                behandling = behandling,
            )

        // Assert
        assertThat(vilkårResultat).anySatisfy {
            assertThat(it.utdypendeVilkårsvurderinger.contains(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)).isTrue()
        }
    }

    @Test
    fun `skal ikke preutfylle bosatt i riket om personen har ukrainsk statsborgerskap`() {
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

        every { systemOnlyPdlRestKlient.hentStatsborgerskap(any()) } returns
            listOf(
                Statsborgerskap(land = "UKR", gyldigFraOgMed = LocalDate.now().minusYears(10), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        every { systemOnlyPdlRestKlient.hentAdresserForPersoner(any()) } answers {
            val identer = firstArg<List<String>>()
            identer.associateWith {
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
            }
        }

        // Act
        preutfyllBosattIRiketService.preutfyllBosattIRiket(vilkårsvurdering = vilkårsvurdering)

        // Assert
        val bosattIRiketResultater =
            vilkårsvurdering.personResultater
                .first { it.aktør == aktør }
                .vilkårResultater

        assertThat(bosattIRiketResultater).isEmpty()
    }

    @Nested
    inner class OppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat {
        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret for person som har oppholdsadresse på Svalbard`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2022, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(any(), any()) } returns emptyList()

            // Act
            val vilkårresultat =
                preutfyllBosattIRiketService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(2)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD)
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret for person som har bostedsadresse i Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    bostedsadresser =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2022, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                preutfyllBosattIRiketService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(2)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret for person som har delt bosted i Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    delteBosteder =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.KARASJOK.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                preutfyllBosattIRiketService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(2)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).containsOnly(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS)
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret om man har flyttet bort fra Svalbard`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2020, 1, 1),
                                periodeTom = LocalDate.of(2020, 12, 31),
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD),
                                begrunnelse = "Begrunnelse som skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    oppholdsadresse =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = LocalDate.of(2025, 10, 15),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = SvalbardKommune.SVALBARD.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                preutfyllBosattIRiketService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(4)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo("Begrunnelse som ikke skal endres")
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2020, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2020, 12, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 10, 15))
                assertThat(it.utdypendeVilkårsvurderinger).isEqualTo(listOf(UtdypendeVilkårsvurdering.BOSATT_PÅ_SVALBARD))
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 10, 16))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
        }

        @Test
        fun `skal oppdatere utvidet vilkårsvurderinger for 'Bosatt i riket'-vilkåret om man har flyttet bort fra Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2020, 1, 1),
                                periodeTom = LocalDate.of(2020, 12, 31),
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS),
                                begrunnelse = "Begrunnelse som skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    bostedsadresser =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = LocalDate.of(2025, 10, 15),
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = KommunerIFinnmarkOgNordTroms.ALTA.kommunenummer),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                preutfyllBosattIRiketService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(4)
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo("Begrunnelse som ikke skal endres")
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2020, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2020, 12, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2021, 1, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 8, 31))
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 9, 1))
                assertThat(it.periodeTom).isEqualTo(LocalDate.of(2025, 10, 15))
                assertThat(it.utdypendeVilkårsvurderinger).isEqualTo(listOf(UtdypendeVilkårsvurdering.BOSATT_I_FINNMARK_NORD_TROMS))
            }
            assertThat(vilkårresultat).anySatisfy {
                assertThat(it.vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
                assertThat(it.resultat).isEqualTo(Resultat.OPPFYLT)
                assertThat(it.begrunnelse).isEqualTo(PREUTFYLT_VILKÅR_BEGRUNNELSE_OVERSKRIFT)
                assertThat(it.periodeFom).isEqualTo(LocalDate.of(2025, 10, 16))
                assertThat(it.periodeTom).isNull()
                assertThat(it.utdypendeVilkårsvurderinger).isEmpty()
            }
        }

        @Test
        fun `skal ikke endre 'Bosatt i riket'-vilkåret hvis personen hverken bor på Svalbard eller i Finnmark eller Nord-Troms`() {
            // Arrange
            val behandling = lagBehandling()

            val personResultat =
                lagPersonResultat(
                    vilkårsvurdering = lagVilkårsvurdering(behandling = behandling),
                    lagVilkårResultater = {
                        setOf(
                            lagVilkårResultat(
                                personResultat = it,
                                vilkårType = Vilkår.BOSATT_I_RIKET,
                                resultat = Resultat.OPPFYLT,
                                periodeFom = LocalDate.of(2021, 1, 1),
                                periodeTom = null,
                                behandlingId = behandling.id,
                                utdypendeVilkårsvurderinger = emptyList(),
                                begrunnelse = "Begrunnelse som ikke skal endres",
                            ),
                        )
                    },
                )

            val adresser =
                lagAdresser(
                    bostedsadresser =
                        listOf(
                            lagAdresse(
                                gyldigFraOgMed = LocalDate.of(2021, 1, 1),
                                gyldigTilOgMed = null,
                                matrikkeladresse = lagMatrikkeladresse(kommunenummer = "0301"),
                            ),
                        ),
                )

            every { andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlingOgBarn(behandling.id, any()) } returns emptyList()

            // Act
            val vilkårresultat =
                preutfyllBosattIRiketService.oppdaterFinnmarkOgSvalbardmerkingPåBosattIRiketVilkårResultat(
                    personResultat = personResultat,
                    adresserForPerson = adresser,
                    behandling = behandling,
                )

            // Assert
            assertThat(vilkårresultat).hasSize(1)
            assertThat(vilkårresultat.single()).isEqualTo(personResultat.vilkårResultater.first())
        }
    }
}
