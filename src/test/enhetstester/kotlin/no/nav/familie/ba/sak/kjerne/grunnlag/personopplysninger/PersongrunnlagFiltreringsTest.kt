package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingKategori
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingUnderkategori
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortBostedsadresserFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortDeltBostedForSøker
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortIkkeRelevanteSivilstander
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortOppholdFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortOppholdsadresserFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortStatsborgerskapFørEldsteBarn
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import no.nav.familie.kontrakter.felles.personopplysning.Opphold
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
        val bostedsadresserFør =
            listOf(
                lagBostedsadresse(gyldigTilOgMed = LocalDate.of(2020, 1, 1), gyldigFraOgMed = LocalDate.of(2018, 1, 1)),
                lagBostedsadresse(gyldigTilOgMed = LocalDate.of(2017, 1, 1), gyldigFraOgMed = LocalDate.of(2016, 1, 1)),
                lagBostedsadresse(gyldigTilOgMed = null, gyldigFraOgMed = LocalDate.of(2020, 1, 1)),
            )

        val eldsteBarnsFødselsdato = LocalDate.of(2019, 1, 1)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato, true)

        // Assert
        assertThat(bostedsadresserEtter).hasSize(2)
        assertThat(bostedsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(bostedsadresserEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrere bort oppholdsadresser med til-og-med dato før eldste barns fødselsdato`() {
        // Arrange
        val oppholdsadresserFør =
            listOf(
                lagOppholdsadresse(gyldigTilOgMed = LocalDate.of(2020, 1, 1), gyldigFraOgMed = LocalDate.of(2018, 1, 1)),
                lagOppholdsadresse(gyldigTilOgMed = LocalDate.of(2017, 1, 1), gyldigFraOgMed = LocalDate.of(1997, 1, 1)),
                lagOppholdsadresse(gyldigTilOgMed = null, gyldigFraOgMed = LocalDate.of(2020, 1, 1)),
            )

        val eldsteBarnsFødselsdato = LocalDate.of(2019, 1, 1)

        // Act
        val oppholdsadresserEtter = oppholdsadresserFør.filtrerBortOppholdsadresserFørEldsteBarn(eldsteBarnsFødselsdato, true)

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

        val eldsteBarnsFødselsdato = LocalDate.of(2019, 1, 1)

        // Act
        val statsborgerskapEtter = statsborgerskapFør.filtrerBortStatsborgerskapFørEldsteBarn(eldsteBarnsFødselsdato, true)

        // Assert
        assertThat(statsborgerskapEtter).hasSize(2)
        assertThat(statsborgerskapEtter.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2010, 1, 1))
        assertThat(statsborgerskapEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(statsborgerskapEtter.last().gyldigFraOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(statsborgerskapEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrer bort opphold før eldste barns sin fødselsdato`() {
        // Arrange
        val oppholdFør =
            listOf(
                Opphold(
                    oppholdFra = LocalDate.of(2000, 1, 1),
                    oppholdTil = LocalDate.of(2010, 1, 1),
                    type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                ),
                Opphold(
                    oppholdFra = LocalDate.of(2010, 1, 1),
                    oppholdTil = LocalDate.of(2020, 1, 1),
                    type = OPPHOLDSTILLATELSE.MIDLERTIDIG,
                ),
                Opphold(
                    oppholdFra = LocalDate.of(2020, 1, 1),
                    oppholdTil = null,
                    type = OPPHOLDSTILLATELSE.PERMANENT,
                ),
            )

        val eldsteBarnsFødselsdato = LocalDate.of(2019, 1, 1)

        // Act
        val oppholdEtter = oppholdFør.filtrerBortOppholdFørEldsteBarn(eldsteBarnsFødselsdato, true)
        // Assert
        assertThat(oppholdEtter).hasSize(2)
        assertThat(oppholdEtter.first().oppholdFra).isEqualTo(LocalDate.of(2010, 1, 1))
        assertThat(oppholdEtter.first().oppholdTil).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(oppholdEtter.last().oppholdFra).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(oppholdEtter.last().oppholdTil).isNull()
    }

    @Test
    fun `Skal ikke filtrere bort sivilstander på barn`() {
        // Arrange

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
        // Act
        val sivilstandBarnEtter = sivilstandBarnFør.filtrerBortIkkeRelevanteSivilstander(true, BehandlingKategori.NASJONAL, BehandlingUnderkategori.ORDINÆR, PersonType.BARN)

        // Assert
        assertThat(sivilstandBarnEtter).hasSize(2)
        assertThat(sivilstandBarnEtter.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2019, 1, 1))
        assertThat(sivilstandBarnEtter.last().gyldigFraOgMed).isEqualTo(LocalDate.of(2022, 1, 1))
    }

    @Test
    fun `skal filtrere bort sivilstander for søker for ordinære nasjonale behandlinger`() {
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

        // Act
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstander(true, BehandlingKategori.NASJONAL, BehandlingUnderkategori.ORDINÆR, PersonType.SØKER)

        // Assert
        assertThat(sivilstandSøkerEtter).hasSize(0)
    }

    @Test
    fun `skal ikke filtrere bort sivilstander for søker for utvidet behandling`() {
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

        // Act
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstander(true, BehandlingKategori.NASJONAL, BehandlingUnderkategori.UTVIDET, PersonType.SØKER)

        // Assert
        assertThat(sivilstandSøkerEtter).hasSize(3)
    }

    @Test
    fun `skal ikke filtrere bort sivilstander for søker for EØS behandling`() {
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

        // Act
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstander(true, BehandlingKategori.EØS, BehandlingUnderkategori.ORDINÆR, PersonType.SØKER)

        // Assert
        assertThat(sivilstandSøkerEtter).hasSize(3)
    }
}
