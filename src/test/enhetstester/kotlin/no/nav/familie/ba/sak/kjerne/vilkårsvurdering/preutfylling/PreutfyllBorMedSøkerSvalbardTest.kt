package no.nav.familie.ba.sak.kjerne.vilkårsvurdering.preutfylling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagGrMatrikkelOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrUkjentAdresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresse
import no.nav.familie.ba.sak.datagenerator.lagGrVegadresseOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.lagVilkårsvurderingMedOverstyrendeResultater
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.vilkårsvurdering.utfall.VilkårKanskjeOppfyltÅrsak.BOR_MED_SØKER_ULIK_OPPHOLDSADRESSE_SVALBARD
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.adresser.oppholdsadresse.GrOppholdsadresse
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.personopplysning.OppholdAnnetSted
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class PreutfyllBorMedSøkerSvalbardTest {
    private val fraDato = LocalDate.of(2020, 1, 1)
    private val persongrunnlagService = mockk<PersongrunnlagService>()
    private val preutfyllBorMedSøkerService = PreutfyllBorMedSøkerService(persongrunnlagService)

    @Test
    fun `skal ikke vurdere bor med søker når begge er på Svalbard med ulike oppholdsadresser`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L)
        val barnAdresse = lagOppholdsadresse(matrikkelId = 2L)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
        assertThat(resultat.erAutomatiskVurdert).isFalse()
        assertThat(resultat.begrunnelse).isEqualTo("Ikke fylt ut automatisk\n- Barn og søker er ikke registrert med samme oppholdsadresse på Svalbard")
        assertThat(resultat.evalueringÅrsaker).containsExactly(BOR_MED_SØKER_ULIK_OPPHOLDSADRESSE_SVALBARD.hentNavn())
    }

    @Test
    fun `skal ikke oppfylle bor med søker når ulike Svalbardadresser og ikke samme bostedsadresse`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L)
        val barnAdresse = lagOppholdsadresse(matrikkelId = 2L)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse), harSammeBostedsadresse = false)

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
        assertThat(resultat.erAutomatiskVurdert).isTrue()
        assertThat(resultat.begrunnelse).isEqualTo("Fylt ut automatisk fra registerdata i PDL\n- Har ikke hatt samme fast eller delt bostedsadresse som søker i minst 3 måneder")
        assertThat(resultat.evalueringÅrsaker).isEmpty()
    }

    @Test
    fun `skal beholde eksisterende vurdering når begge har samme oppholdsadresse på Svalbard`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L)
        val barnAdresse = lagOppholdsadresse(matrikkelId = 1L)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.OPPFYLT)
        assertThat(resultat.erAutomatiskVurdert).isTrue()
    }

    @ParameterizedTest
    @CsvSource("2100, 0301", "0301, 2100", "0301, 0301")
    fun `skal ikke kontrollere oppholdsadressene når ikke begge er på Svalbard`(
        søkerKommune: String,
        barnKommune: String,
    ) {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L, kommunenummer = søkerKommune)
        val barnAdresse = lagOppholdsadresse(matrikkelId = 2L, kommunenummer = barnKommune)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `skal ikke bruke like oppholdsadresser utenfor Svalbard til å oppfylle bor med søker`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L, kommunenummer = "0301")
        val barnAdresse = lagOppholdsadresse(matrikkelId = 1L, kommunenummer = "0301")
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse), harSammeBostedsadresse = false)

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @ParameterizedTest
    @CsvSource("true, false", "false, true", "false, false")
    fun `skal beholde eksisterende vurdering når minst en mangler oppholdsadresse`(
        søkerHarOppholdsadresse: Boolean,
        barnHarOppholdsadresse: Boolean,
    ) {
        // Arrange
        val søkerAdresser = if (søkerHarOppholdsadresse) listOf(lagOppholdsadresse(matrikkelId = 1L)) else emptyList()
        val barnAdresser = if (barnHarOppholdsadresse) listOf(lagOppholdsadresse(matrikkelId = 2L)) else emptyList()
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(søkerAdresser, barnAdresser)

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `skal godta lik matrikkeladresse på Svalbard`() {
        // Arrange
        val søkerAdresse = lagGrMatrikkelOppholdsadresse(matrikkelId = 1L, kommunenummer = "2100", periode = DatoIntervallEntitet(fraDato, null))
        val barnAdresse = lagGrMatrikkelOppholdsadresse(matrikkelId = 1L, kommunenummer = "2100", periode = DatoIntervallEntitet(fraDato, null))
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `skal ikke vurdere bor med søker når begge bare har Svalbardmerking uten konkret adresse`() {
        // Arrange
        val søkerAdresse =
            lagGrUkjentAdresseOppholdsadresse(
                periode = DatoIntervallEntitet(fraDato, null),
                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
            )
        val barnAdresse =
            lagGrUkjentAdresseOppholdsadresse(
                periode = DatoIntervallEntitet(fraDato, null),
                oppholdAnnetSted = OppholdAnnetSted.PAA_SVALBARD,
            )

        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
    }

    @Test
    fun `skal ikke anse bare likt kommunenummer som samme oppholdsadresse`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = null)
        val barnAdresse = lagOppholdsadresse(matrikkelId = null)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
    }

    @Test
    fun `skal kreve manuell vurdering for lik konkret vegadresse uten matrikkelId`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = null).copy(adressenavn = "Vei 500", husnummer = "1", bruksenhetsnummer = "H0101")
        val barnAdresse = lagOppholdsadresse(matrikkelId = null).copy(adressenavn = "Vei 500", husnummer = "1", bruksenhetsnummer = "H0101")
        søkerAdresse.periode = DatoIntervallEntitet(fraDato, null)
        barnAdresse.periode = DatoIntervallEntitet(fraDato, null)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultat = vilkårsvurdering.hentBorMedSøkerResultaterForBarn().single()
        assertThat(resultat.resultat).isEqualTo(Resultat.IKKE_VURDERT)
    }

    @Test
    fun `skal bare kreve manuell vurdering i perioden begge har ulike oppholdsadresser på Svalbard`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L, fom = fraDato.plusYears(1), tom = fraDato.plusYears(3).minusDays(1))
        val barnAdresse = lagOppholdsadresse(matrikkelId = 2L, fom = fraDato.plusYears(2), tom = fraDato.plusYears(4).minusDays(1))
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnAdresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultater = vilkårsvurdering.hentBorMedSøkerResultaterForBarn()
        assertThat(resultater.map { Triple(it.periodeFom, it.periodeTom, it.resultat) }).containsExactly(
            Triple(fraDato, fraDato.plusYears(2).minusDays(1), Resultat.OPPFYLT),
            Triple(fraDato.plusYears(2), fraDato.plusYears(3).minusDays(1), Resultat.IKKE_VURDERT),
            Triple(fraDato.plusYears(3), null, Resultat.OPPFYLT),
        )
    }

    @Test
    fun `skal vurdere Svalbardadressebytte fra ulike til like adresser`() {
        // Arrange
        val byttedato = fraDato.plusYears(1)
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L, fom = fraDato, tom = null)
        val barnFørBytte = lagOppholdsadresse(matrikkelId = 2L, fom = fraDato, tom = null)
        val barnEtterBytte = lagOppholdsadresse(matrikkelId = 1L, fom = byttedato, tom = null)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnFørBytte, barnEtterBytte))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultater = vilkårsvurdering.hentBorMedSøkerResultaterForBarn()
        assertThat(resultater).hasSize(2)
        assertThat(resultater.map { Triple(it.periodeFom, it.periodeTom, it.resultat) }).containsExactly(
            Triple(fraDato, byttedato.minusDays(1), Resultat.IKKE_VURDERT),
            Triple(byttedato, null, Resultat.OPPFYLT),
        )
        assertThat(resultater[0].erAutomatiskVurdert).isFalse()
        assertThat(resultater[0].evalueringÅrsaker).containsExactly(BOR_MED_SØKER_ULIK_OPPHOLDSADRESSE_SVALBARD.hentNavn())
        assertThat(resultater[1].erAutomatiskVurdert).isTrue()
        assertThat(resultater[1].evalueringÅrsaker).isEmpty()
    }

    @Test
    fun `skal vurdere Svalbardadressebytte fra like til ulike adresser`() {
        // Arrange
        val byttedato = fraDato.plusYears(1)
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L, fom = fraDato, tom = null)
        val barnFørBytte = lagOppholdsadresse(matrikkelId = 1L, fom = fraDato, tom = null)
        val barnEtterBytte = lagOppholdsadresse(matrikkelId = 2L, fom = byttedato, tom = null)
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnFørBytte, barnEtterBytte))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultater = vilkårsvurdering.hentBorMedSøkerResultaterForBarn()
        assertThat(resultater).hasSize(2)
        assertThat(resultater.map { Triple(it.periodeFom, it.periodeTom, it.resultat) }).containsExactly(
            Triple(fraDato, byttedato.minusDays(1), Resultat.OPPFYLT),
            Triple(byttedato, null, Resultat.IKKE_VURDERT),
        )
        assertThat(resultater[0].erAutomatiskVurdert).isTrue()
        assertThat(resultater[0].evalueringÅrsaker).isEmpty()
        assertThat(resultater[1].erAutomatiskVurdert).isFalse()
        assertThat(resultater[1].evalueringÅrsaker).containsExactly(BOR_MED_SØKER_ULIK_OPPHOLDSADRESSE_SVALBARD.hentNavn())
    }

    @Test
    fun `skal avslutte Svalbardkontrollen når neste oppholdsadresse er på fastlandet`() {
        // Arrange
        val søkerAdresse = lagOppholdsadresse(matrikkelId = 1L)
        val barnSvalbardadresse = lagOppholdsadresse(matrikkelId = 2L)
        val barnFastlandsadresse = lagOppholdsadresse(matrikkelId = 3L, kommunenummer = "0301", fom = fraDato.plusYears(1))
        val vilkårsvurdering = settOppVilkårsvurderingMedAdresser(listOf(søkerAdresse), listOf(barnSvalbardadresse, barnFastlandsadresse))

        // Act
        preutfyllBorMedSøkerService.preutfyllBorMedSøker(vilkårsvurdering, vilkårsvurdering.personResultater.map { it.aktør }, digitalSøknad = null)

        // Assert
        val resultater = vilkårsvurdering.hentBorMedSøkerResultaterForBarn()
        assertThat(resultater.map { Triple(it.periodeFom, it.periodeTom, it.resultat) }).containsExactly(
            Triple(fraDato, fraDato.plusYears(1).minusDays(1), Resultat.IKKE_VURDERT),
            Triple(fraDato.plusYears(1), null, Resultat.OPPFYLT),
        )
    }

    private fun lagOppholdsadresse(
        matrikkelId: Long?,
        kommunenummer: String = "2100",
        fom: LocalDate = fraDato,
        tom: LocalDate? = null,
    ) = lagGrVegadresseOppholdsadresse(
        matrikkelId = matrikkelId,
        kommunenummer = kommunenummer,
        periode = DatoIntervallEntitet(fom, tom),
    )

    private fun settOppVilkårsvurderingMedAdresser(
        søkerAdresser: List<GrOppholdsadresse>,
        barnAdresser: List<GrOppholdsadresse>,
        årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
        harSammeBostedsadresse: Boolean = true,
    ): Vilkårsvurdering {
        val behandling = lagBehandling(årsak = årsak, skalBehandlesAutomatisk = årsak == BehandlingÅrsak.AUTOMATISK_BEHANDLING_AV_SØKNAD)
        val persongrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = randomFnr(),
                barnasIdenter = listOf(randomFnr()),
            )
        val søker = persongrunnlag.søker
        val barn = persongrunnlag.barna.single()
        persongrunnlag.personer.forEach { person ->
            person.bostedsadresser =
                mutableListOf(
                    lagGrVegadresse(matrikkelId = if (person == søker || harSammeBostedsadresse) 123L else 456L).also {
                        it.periode = DatoIntervallEntitet(fraDato, null)
                        it.person = person
                    },
                )
            person.deltBosted.clear()
        }
        søker.oppholdsadresser = søkerAdresser.toMutableList()
        barn.oppholdsadresser = barnAdresser.toMutableList()
        every { persongrunnlagService.hentAktivThrows(behandling.id) } returns persongrunnlag
        return lagVilkårsvurderingMedOverstyrendeResultater(
            behandling = behandling,
            søker = søker,
            barna = listOf(barn),
            overstyrendeVilkårResultater = emptyMap(),
        )
    }

    private fun Vilkårsvurdering.hentBorMedSøkerResultaterForBarn(): List<VilkårResultat> =
        personResultater
            .single { !it.erSøkersResultater() }
            .vilkårResultater
            .filter { it.vilkårType == Vilkår.BOR_MED_SØKER }
            .sortedBy { it.periodeFom }
}
