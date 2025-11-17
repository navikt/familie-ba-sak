package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortBostedsadresserFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortDeltBostedForSøker
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortIkkeRelevanteSivilstand
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortOppholdsadresserFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortStatsborgerskapFørEldsteBarn
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTANDTYPE
import no.nav.familie.kontrakter.felles.personopplysning.Sivilstand
import no.nav.familie.kontrakter.felles.personopplysning.Statsborgerskap
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersongrunnlagFiltreringsTest {
    val søkerFnr = randomFnr()
    val barnFnr = randomFnr()
    val behandling = lagBehandling()

    @Test
    fun `skal filtrere bort bostedsadresser med til-og-med dato før eldste barns fødselsdato`() {
        // Arrange
        val grunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                barnasFødselsdatoer = listOf(LocalDate.of(2019, 1, 1)),
            )

        val bostedsadresserFør =
            listOf(
                lagBostedsadresse(gyldigTilOgMed = LocalDate.of(2020, 1, 1), gyldigFraOgMed = LocalDate.of(2018, 1, 1)),
                lagBostedsadresse(gyldigTilOgMed = LocalDate.of(2017, 1, 1), gyldigFraOgMed = LocalDate.of(2016, 1, 1)),
                lagBostedsadresse(gyldigTilOgMed = null, gyldigFraOgMed = LocalDate.of(2020, 1, 1)),
            )

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(grunnlag, true)

        // Assert
        assertThat(bostedsadresserEtter).hasSize(2)
        assertThat(bostedsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(bostedsadresserEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrere bort oppholdsadresser med til-og-med dato før eldste barns fødselsdato`() {
        // Arrange
        val grunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                barnasFødselsdatoer = listOf(LocalDate.of(2019, 1, 1)),
            )

        val oppholdsadresserFør =
            listOf(
                lagOppholdsadresse(gyldigTilOgMed = LocalDate.of(2020, 1, 1), gyldigFraOgMed = LocalDate.of(2018, 1, 1)),
                lagOppholdsadresse(gyldigTilOgMed = LocalDate.of(2017, 1, 1), gyldigFraOgMed = LocalDate.of(1997, 1, 1)),
                lagOppholdsadresse(gyldigTilOgMed = null, gyldigFraOgMed = LocalDate.of(2020, 1, 1)),
            )

        // Act
        val oppholdsadresserEtter = oppholdsadresserFør.filtrerBortOppholdsadresserFørEldsteBarn(grunnlag, true)

        // Assert
        assertThat(oppholdsadresserEtter).hasSize(2)
        assertThat(oppholdsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(oppholdsadresserEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrere bort delt bosted hos søker`() {
        // Arrange

        val delteBostederFør =
            listOf(
                lagDeltBosted(sluttdatoForKontrakt = LocalDate.of(2020, 1, 1), startdatoForKontrakt = LocalDate.of(2019, 1, 1)),
                lagDeltBosted(sluttdatoForKontrakt = null, startdatoForKontrakt = LocalDate.of(2020, 1, 1)),
            )

        // Act
        val deltBostedBarnEtter = delteBostederFør.filtrerBortDeltBostedForSøker(personType = PersonType.BARN, true)
        val deltBostedSøkerEtter = delteBostederFør.filtrerBortDeltBostedForSøker(personType = PersonType.SØKER, true)

        // Assert
        assertThat(deltBostedBarnEtter).hasSize(2)
        assertThat(deltBostedBarnEtter.first().sluttdatoForKontrakt).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(deltBostedBarnEtter.last().sluttdatoForKontrakt).isNull()

        assert(deltBostedSøkerEtter.isEmpty())
    }

    @Test
    fun `skal filtrere bort statsborgerskap før eldste barns fødselsdato`() {
        // Arrange
        val statsborgerskapFør =
            listOf(
                Statsborgerskap(
                    gyldigFraOgMed = LocalDate.of(2000, 1, 1),
                    gyldigTilOgMed = LocalDate.of(2010, 1, 1),
                    land = "POL",
                    bekreftelsesdato = null,
                ),
                Statsborgerskap(
                    gyldigFraOgMed = LocalDate.of(2010, 1, 1),
                    gyldigTilOgMed = LocalDate.of(2020, 1, 1),
                    land = "NOR",
                    bekreftelsesdato = null,
                ),
                Statsborgerskap(
                    gyldigFraOgMed = LocalDate.of(2020, 1, 1),
                    gyldigTilOgMed = null,
                    land = "SWE",
                    bekreftelsesdato = null,
                ),
            )

        val grunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                barnasFødselsdatoer = listOf(LocalDate.of(2019, 1, 1)),
            )

        // Act
        val statsborgerskapEtter = statsborgerskapFør.filtrerBortStatsborgerskapFørEldsteBarn(grunnlag, true)

        // Assert
        assertThat(statsborgerskapEtter).hasSize(2)
        assertThat(statsborgerskapEtter.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2010, 1, 1))
        assertThat(statsborgerskapEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(statsborgerskapEtter.last().gyldigFraOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(statsborgerskapEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrere bort sivilstand for søker før eldste barns fødselsdato`() {
        // Arrange
        val sivilstandSøkerFør =
            listOf(
                Sivilstand(
                    gyldigFraOgMed = LocalDate.of(2000, 1, 1),
                    type = SIVILSTANDTYPE.UGIFT,
                ),
                Sivilstand(
                    gyldigFraOgMed = LocalDate.of(2010, 1, 1),
                    type = SIVILSTANDTYPE.GIFT,
                ),
                Sivilstand(
                    gyldigFraOgMed = LocalDate.of(2020, 1, 1),
                    type = SIVILSTANDTYPE.SKILT,
                ),
            )

        val sivilstandBarnFør =
            listOf(
                Sivilstand(
                    gyldigFraOgMed = LocalDate.of(2019, 1, 1),
                    type = SIVILSTANDTYPE.UGIFT,
                ),
                Sivilstand(
                    gyldigFraOgMed = LocalDate.of(2022, 1, 1),
                    type = SIVILSTANDTYPE.GIFT,
                ),
            )

        val grunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                barnasFødselsdatoer = listOf(LocalDate.of(2019, 1, 1)),
            )

        // Act
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstand(grunnlag, true, BehandlingUnderkategori.UTVIDET, PersonType.SØKER)
        val sivilstandBarnEtter = sivilstandBarnFør.filtrerBortIkkeRelevanteSivilstand(grunnlag, true, BehandlingUnderkategori.ORDINÆR, PersonType.BARN)

        // Assert
        assertThat(sivilstandSøkerEtter).hasSize(2)
        assertThat(sivilstandSøkerEtter.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2010, 1, 1))
        assertThat(sivilstandSøkerEtter.last().gyldigFraOgMed).isEqualTo(LocalDate.of(2020, 1, 1))

        assertThat(sivilstandBarnEtter).hasSize(2)
        assertThat(sivilstandBarnEtter.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2019, 1, 1))
        assertThat(sivilstandBarnEtter.last().gyldigFraOgMed).isEqualTo(LocalDate.of(2022, 1, 1))
    }
}
