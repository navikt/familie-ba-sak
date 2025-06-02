package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.cucumber.lagVilkårsvurdering
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagMatrikkeladresse
import no.nav.familie.ba.sak.datagenerator.lagPersonResultat
import no.nav.familie.ba.sak.datagenerator.lagSøknad
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVegadresse
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.integrasjoner.pdl.PdlRestClient
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.søknad.SøknadService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
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
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
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
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårResultat).hasSize(3)
        assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(4))
        assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(2))
        assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårResultat.first().vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
    }

    @Test
    fun `skal ikke få noen vilkårresultat når pdl ikke returnerer noen bostedsadresser`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns emptyList<Bostedsadresse>()

        // Act
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårResultat).isEmpty()
    }

    @Test
    fun `skal gi vilkårsresultat IKKE_OPPFYLT hvis bosatt i riket er mindre enn 12 mnd`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
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
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårResultat.first().resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
    }

    @Test
    fun `skal ikke gi oppfylt i siste periode hvis den er under 12 mnd og søker ikke planlegger å bo i Norge neste 12`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(6),
                    gyldigTilOgMed = LocalDate.now().minusMonths(4),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.hentSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false)

        // Act
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårResultat).allMatch { it.resultat == Resultat.IKKE_OPPFYLT }
    }

    @Test
    fun `skal gi oppfylt i siste periode hvis den er under 12 mnd og søker planlegger å bo i Norge neste 12`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(2),
                    gyldigTilOgMed = null,
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.hentSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = true)

        // Act
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårResultat.singleOrNull()?.resultat).isEqualTo(Resultat.OPPFYLT)
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

        every { pdlRestClient.hentBostedsadresserForPerson(søkerFnr) } returns
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
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat, LocalDate.now().minusYears(10))

        // Assert
        assertThat(vilkårResultat).hasSize(3)
        assertThat(vilkårResultat.first().periodeFom).isEqualTo(LocalDate.now().minusYears(10))
        assertThat(vilkårResultat.first().periodeTom).isEqualTo(LocalDate.now().minusYears(5))
        assertThat(vilkårResultat.first().vilkårType).isEqualTo(Vilkår.BOSATT_I_RIKET)

        assertThat(vilkårResultat.last().periodeFom).isEqualTo(LocalDate.now().minusYears(1))
        assertThat(vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }).isNotNull
    }

    @Test
    fun `skal ikke sjekke 12 måneders krav for nordiske statsborgere`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
            listOf(
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(10),
                    gyldigTilOgMed = LocalDate.now().minusMonths(8),
                    vegadresse = lagVegadresse(12345L),
                ),
                Bostedsadresse(
                    gyldigFraOgMed = LocalDate.now().minusMonths(2),
                    vegadresse = lagVegadresse(12345L),
                ),
            )

        every { søknadService.hentSøknad(behandling.id) } returns lagSøknad(søkerPlanleggerÅBoINorge12Mnd = false, barneIdenterTilPlanleggerBoINorge12Mnd = mapOf(randomFnr() to false))

        every { pdlRestClient.hentStatsborgerskapUtenHistorikk(any()) } returns
            listOf(
                Statsborgerskap(land = "DNK", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        // Act
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        assertThat(vilkårResultat).hasSize(2)
    }

    @Test
    fun `skal gi ikke oppyflt på 12 måneders krav for nordiske statsborgere for eldre perioder`() {
        // Arrange
        val behandling = lagBehandling()
        val persongrunnlag = lagTestPersonopplysningGrunnlag(behandling.id)
        val vilkårsvurdering = lagVilkårsvurdering(persongrunnlag, behandling)
        val personResultat = lagPersonResultat(vilkårsvurdering = vilkårsvurdering)

        every { pdlRestClient.hentBostedsadresserForPerson(any()) } returns
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

        every { søknadService.hentSøknad(behandling.id) } returns lagSøknad()

        every { pdlRestClient.hentStatsborgerskapUtenHistorikk(any()) } returns
            listOf(
                Statsborgerskap(land = "DNK", gyldigFraOgMed = LocalDate.now().minusYears(3), gyldigTilOgMed = null, bekreftelsesdato = null),
            )

        // Act
        val vilkårResultat = preutfyllBosattIRiketService.genererBosattIRiketVilkårResultat(personResultat)

        // Assert
        val ikkeOppfyltPeriode = vilkårResultat.find { it.resultat == Resultat.IKKE_OPPFYLT }
        assertThat(ikkeOppfyltPeriode).`as`("Forventer én IKKE_OPPFYLT periode").isNotNull
        assertThat(ikkeOppfyltPeriode?.periodeFom).`as`("Ikke oppfylt periode fom").isEqualTo(LocalDate.now().minusMonths(12))
        assertThat(ikkeOppfyltPeriode?.periodeTom).`as`("Ikke oppfylt periode tom").isEqualTo(LocalDate.now().minusMonths(1).minusDays(1))

        val oppfyltPeriode = vilkårResultat.find { it.resultat == Resultat.OPPFYLT }
        assertThat(oppfyltPeriode).`as`("Forventer én OPPFYLT periode").isNotNull
        assertThat(oppfyltPeriode?.periodeFom).`as`("Oppfylt periode fom").isEqualTo(LocalDate.now().minusMonths(1))
        assertThat(oppfyltPeriode?.periodeTom).`as`("Oppfylt periode tom").isNull()
    }
}
