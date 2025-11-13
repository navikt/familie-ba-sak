package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortBostedsadresserFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortDeltBostedForSøker
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortOppholdsadresserFørEldsteBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersongrunnlagFiltreringsTest {
    @Test
    fun `skal filtrere bort bostedsadresser med til-og-med dato før eldste barns fødselsdato`() {
        // Arrange
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val behandling = lagBehandling()

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
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val behandling = lagBehandling()

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
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val behandling = lagBehandling()

        val grunnlag =
            lagTestPersonopplysningGrunnlag(
                behandlingId = behandling.id,
                søkerPersonIdent = søkerFnr,
                barnasIdenter = listOf(barnFnr),
                barnasFødselsdatoer = listOf(LocalDate.of(2019, 1, 1)),
            )

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
}
