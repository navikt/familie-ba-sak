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
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).hasSize(2)
        assertThat(bostedsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(bostedsadresserEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrere bort bostedsadresse uten til-og-med dato som opphører ved neste adresse før eldste barns fødselsdato`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 20), angittFlyttedato = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val avsluttetAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2021, 8, 26), angittFlyttedato = LocalDate.of(2021, 8, 23), gyldigTilOgMed = LocalDate.of(2022, 1, 1))
        val gjeldendeAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 7, 28), angittFlyttedato = LocalDate.of(2025, 7, 23), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(avsluttetAdresse, gammelAdresse, gjeldendeAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(gjeldendeAdresse)
    }

    @Test
    fun `skal beholde bostedsadresse uten til-og-med dato som er gjeldende ved eldste barns fødselsdato og bevare rekkefølgen`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val gjeldendeAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 7, 23), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gjeldendeAdresse, gammelAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(gjeldendeAdresse, gammelAdresse)
    }

    @Test
    fun `skal filtrere bort bostedsadresse uten til-og-med dato når neste adresse begynner på eldste barns fødselsdato`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val nyAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 8, 22), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gammelAdresse, nyAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(nyAdresse)
    }

    @Test
    fun `skal beholde bostedsadresse uten til-og-med dato når neste adresse begynner dagen etter eldste barns fødselsdato`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val nyAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 8, 23), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gammelAdresse, nyAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(gammelAdresse, nyAdresse)
    }

    @Test
    fun `skal utlede til-og-med fra neste adresse med senere fra-og-med når flere bostedsadresser har lik fra-og-med`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val avsluttetAdresseMedLikFom = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = LocalDate.of(2019, 12, 31))
        val gjeldendeAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 7, 23), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gammelAdresse, avsluttetAdresseMedLikFom, gjeldendeAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(gammelAdresse, gjeldendeAdresse)
    }

    @Test
    fun `skal bruke angitt flyttedato fremfor gyldig fra-og-med når til-og-med utledes fra neste bostedsadresse`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 1, 1), gyldigTilOgMed = null)
        val nyAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 8, 25), angittFlyttedato = LocalDate.of(2024, 8, 20), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gammelAdresse, nyAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(nyAdresse)
    }

    @Test
    fun `skal beholde bostedsadresse uten fra-og-med og til-og-med dato`() {
        // Arrange
        val adresseUtenDatoer = lagBostedsadresse(gyldigFraOgMed = null, angittFlyttedato = null, gyldigTilOgMed = null)
        val gjeldendeAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 1, 1), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(adresseUtenDatoer, gjeldendeAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(adresseUtenDatoer, gjeldendeAdresse)
    }

    @Test
    fun `skal bruke gyldig fra-og-med når angitt flyttedato mangler`() {
        // Arrange
        val gammelAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 1, 1), angittFlyttedato = null, gyldigTilOgMed = null)
        val nyAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 8, 20), angittFlyttedato = null, gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gammelAdresse, nyAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(nyAdresse)
    }

    @Test
    fun `skal beholde bostedsadresse med til-og-med dato lik eldste barns fødselsdato`() {
        // Arrange
        val avsluttetAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = LocalDate.of(2024, 8, 22))
        val gjeldendeAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 8, 23), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(avsluttetAdresse, gjeldendeAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(avsluttetAdresse, gjeldendeAdresse)
    }

    @Test
    fun `skal ikke la bostedsadresser med ugyldig periode avslutte bostedsadresse uten til-og-med dato`() {
        // Arrange
        val gjeldendeAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val adresseMedLikFomOgTom = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 5, 1), gyldigTilOgMed = LocalDate.of(2024, 5, 1))
        val adresseMedFomEtterTom = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2024, 6, 1), gyldigTilOgMed = LocalDate.of(2024, 1, 1))
        val nyAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(2025, 7, 23), gyldigTilOgMed = null)
        val bostedsadresserFør = listOf(gjeldendeAdresse, adresseMedLikFomOgTom, adresseMedFomEtterTom, nyAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(gjeldendeAdresse, nyAdresse)
    }

    @Test
    fun `skal beholde bostedsadresse med manglende flyttedato fra Freg selv om en eldre adresse har fra-og-med`() {
        // Arrange
        val gjeldendeAdresseUtenFlyttedato = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(1998, 3, 1), angittFlyttedato = LocalDate.of(1, 1, 1), gyldigTilOgMed = null)
        val eldreAdresse = lagBostedsadresse(gyldigFraOgMed = LocalDate.of(1990, 5, 1), gyldigTilOgMed = LocalDate.of(1998, 2, 28))
        val bostedsadresserFør = listOf(gjeldendeAdresseUtenFlyttedato, eldreAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(bostedsadresserEtter).containsExactly(gjeldendeAdresseUtenFlyttedato)
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
        val oppholdsadresserEtter = oppholdsadresserFør.filtrerBortOppholdsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(oppholdsadresserEtter).hasSize(2)
        assertThat(oppholdsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(oppholdsadresserEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal filtrere bort oppholdsadresse uten til-og-med dato som opphører ved neste adresse før eldste barns fødselsdato`() {
        // Arrange
        val gammelAdresse = lagOppholdsadresse(gyldigFraOgMed = LocalDate.of(2019, 8, 5), gyldigTilOgMed = null)
        val avsluttetAdresse = lagOppholdsadresse(gyldigFraOgMed = LocalDate.of(2021, 8, 23), gyldigTilOgMed = LocalDate.of(2022, 1, 1))
        val gjeldendeAdresse = lagOppholdsadresse(gyldigFraOgMed = LocalDate.of(2025, 7, 23), gyldigTilOgMed = null)
        val oppholdsadresserFør = listOf(avsluttetAdresse, gammelAdresse, gjeldendeAdresse)

        val eldsteBarnsFødselsdato = LocalDate.of(2024, 8, 22)

        // Act
        val oppholdsadresserEtter = oppholdsadresserFør.filtrerBortOppholdsadresserFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(oppholdsadresserEtter).containsExactly(gjeldendeAdresse)
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
        val deltBostedBarnEtter = delteBostederFør.filtrerBortDeltBostedForSøker(personType = PersonType.BARN)
        val deltBostedSøkerEtter = delteBostederFør.filtrerBortDeltBostedForSøker(personType = PersonType.SØKER)

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
        val statsborgerskapEtter = statsborgerskapFør.filtrerBortStatsborgerskapFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(statsborgerskapEtter).hasSize(2)
        assertThat(statsborgerskapEtter.first().gyldigFraOgMed).isEqualTo(LocalDate.of(2010, 1, 1))
        assertThat(statsborgerskapEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(statsborgerskapEtter.last().gyldigFraOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(statsborgerskapEtter.last().gyldigTilOgMed).isNull()
    }

    @Test
    fun `skal beholde statsborgerskap uten til-og-med dato selv om et nyere statsborgerskap begynner før eldste barns fødselsdato`() {
        // Arrange
        val statsborgerskapFør =
            listOf(
                Statsborgerskap(
                    gyldigFraOgMed = LocalDate.of(2000, 1, 1),
                    gyldigTilOgMed = null,
                    land = "NOR",
                    bekreftelsesdato = null,
                ),
                Statsborgerskap(
                    gyldigFraOgMed = LocalDate.of(2010, 1, 1),
                    gyldigTilOgMed = null,
                    land = "POL",
                    bekreftelsesdato = null,
                ),
            )

        val eldsteBarnsFødselsdato = LocalDate.of(2015, 1, 1)

        // Act
        val statsborgerskapEtter = statsborgerskapFør.filtrerBortStatsborgerskapFørEldsteBarn(eldsteBarnsFødselsdato)

        // Assert
        assertThat(statsborgerskapEtter).containsExactlyElementsOf(statsborgerskapFør)
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
        val oppholdEtter = oppholdFør.filtrerBortOppholdFørEldsteBarn(eldsteBarnsFødselsdato)
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
        val sivilstandBarnEtter = sivilstandBarnFør.filtrerBortIkkeRelevanteSivilstander(BehandlingKategori.NASJONAL, BehandlingUnderkategori.ORDINÆR, PersonType.BARN)

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
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstander(BehandlingKategori.NASJONAL, BehandlingUnderkategori.ORDINÆR, PersonType.SØKER)

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
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstander(BehandlingKategori.NASJONAL, BehandlingUnderkategori.UTVIDET, PersonType.SØKER)

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
        val sivilstandSøkerEtter = sivilstandSøkerFør.filtrerBortIkkeRelevanteSivilstander(BehandlingKategori.EØS, BehandlingUnderkategori.ORDINÆR, PersonType.SØKER)

        // Assert
        assertThat(sivilstandSøkerEtter).hasSize(3)
    }
}
