package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPerson
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.domene.PersonIdent
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling.BegrunnelseForManuellKontrollAvVilkår.INFORMASJON_FRA_SØKNAD
import no.nav.familie.kontrakter.felles.personopplysning.Bostedsadresse
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PreutfyllBosattIRiketServiceTest {
    private val pdlRestClient: PdlRestClient = mockk(relaxed = true)
    private val søknadService: SøknadService = mockk(relaxed = true)
    private val persongrunnlagService: PersongrunnlagService = mockk(relaxed = true)
    private val preutfyllBosattIRiketService = PreutfyllBosattIRiketService(pdlRestClient, søknadService, persongrunnlagService)

    @Test
    fun `skal lage preutfylt vilkårresultat basert på data fra pdl`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(4),
                    gyldigTilOgMed = LocalDate.now().minusYears(3),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(3).plusDays(1),
                    gyldigTilOgMed = LocalDate.now().minusYears(2),
                    matrikkeladresse = lagMatrikkeladresse(54321L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(98765L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(6),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(98764L),
                ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                bostedsadresserForPerson = bostedsadresser,
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
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                bostedsadresserForPerson = emptyList(),
            )

        // Assert
        assertThat(vilkårResultat).isEmpty()
    }

    @Test
    fun `skal gi vilkårsresultat IKKE_OPPFYLT hvis bosatt i riket er mindre enn 12 mnd`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(6),
                    gyldigTilOgMed = LocalDate.now().minusMonths(4),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(4).plusDays(1),
                    gyldigTilOgMed = LocalDate.now().minusMonths(2),
                    matrikkeladresse = lagMatrikkeladresse(54321L),
                ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                bostedsadresserForPerson = bostedsadresser,
            )

        // Assert
        assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }

    @Test
    fun `skal ikke gi oppfylt i siste periode hvis den er under 12 mnd og søker ikke planlegger å bo i Norge neste 12`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id, søkerPersonIdent = randomFnr(), barnasIdenter = listOf(randomFnr()))
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(6),
                    gyldigTilOgMed = LocalDate.now().minusMonths(4),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false)

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                bostedsadresserForPerson = bostedsadresser,
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
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(8),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = true)

        // Act
        val vilkårResultater =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(2),
                bostedsadresserForPerson = bostedsadresser,
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
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(2),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false, barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(barnFnr to true))

        // Act
        val vilkårResultater =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(2),
                bostedsadresserForPerson = bostedsadresser,
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
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(20),
                    gyldigTilOgMed = LocalDate.now().minusYears(15),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(12),
                    gyldigTilOgMed = LocalDate.now().minusYears(5),
                    matrikkeladresse = lagMatrikkeladresse(54321L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(1),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(98765L),
                ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(10),
                bostedsadresserForPerson = bostedsadresser,
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
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(12),
                    gyldigTilOgMed = LocalDate.now().minusMonths(5),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(1),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad()

        every { pdlRestClient.hentStatsborgerskap(any(), historikk = true) } returns
            listOf(
                Statsborgerskap(land = "DNK", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(10),
                bostedsadresserForPerson = bostedsadresser,
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
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.søker.aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(5),
                    gyldigTilOgMed = LocalDate.now().minusYears(4).minusMonths(2),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(1),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.finnSøknad(behandling.id) } returns lagSøknad()

        every { pdlRestClient.hentStatsborgerskap(any(), historikk = true) } returns
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
                bostedsadresserForPerson = bostedsadresser,
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
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(2),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusMonths(2),
                bostedsadresserForPerson = bostedsadresser,
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
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(4),
                    gyldigTilOgMed = LocalDate.now().minusYears(3),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusYears(2).plusDays(1),
                    matrikkeladresse = lagMatrikkeladresse(54321L),
                ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                bostedsadresserForPerson = bostedsadresser,
            )

        // Assert
        val begrunnelse = vilkårResultat.firstOrNull { it.resultat == Resultat.OPPFYLT }?.begrunnelse
        assertThat(vilkårResultat).hasSize(3)
        assertThat(begrunnelse).isEqualTo(
            "Fylt ut automatisk fra registerdata i PDL\n" +
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
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering, aktør = persongrunnlag.barna.first().aktør)

        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag

        val bostedsadresser =
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(4),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        // Act
        val vilkårResultat =
            preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(
                personResultat = personResultat,
                fødselsdatoForBeskjæring = LocalDate.now().minusYears(4),
                bostedsadresserForPerson = bostedsadresser,
            )

        // Assert
        val begrunnelse = vilkårResultat.firstOrNull { it.resultat == Resultat.OPPFYLT }?.begrunnelse
        assertThat(begrunnelse).isEqualTo(
            "Fylt ut automatisk fra registerdata i PDL\n" +
                "- Bosatt i Norge siden fødsel.",
        )
        assertThat(vilkårResultat).allSatisfy {
            assertThat(it.begrunnelseForManuellKontroll).isNull()
        }
    }
}
