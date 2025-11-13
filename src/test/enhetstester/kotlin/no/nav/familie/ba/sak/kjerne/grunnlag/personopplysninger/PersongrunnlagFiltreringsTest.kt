package no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger

import no.nav.familie.ba.sak.datagenerator.lagBehandling
import no.nav.familie.ba.sak.datagenerator.lagBostedsadresse
import no.nav.familie.ba.sak.datagenerator.lagDeltBosted
import no.nav.familie.ba.sak.datagenerator.lagOppholdsadresse
import no.nav.familie.ba.sak.datagenerator.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.datagenerator.randomFnr
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortBostedsadresserFørEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortDeltBostedførEldsteBarn
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningsgrunnlagFiltreringUtils.filtrerBortOppholdsadresserFørEldsteBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersongrunnlagFiltreringsTest {
    @Test
    fun `skal filtrere bort adresser med til-og-med dato før eldste barns fødselsdato`() {
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

        val oppholdsadresserFør =
            listOf(
                lagOppholdsadresse(gyldigTilOgMed = LocalDate.of(2020, 1, 1), gyldigFraOgMed = LocalDate.of(2018, 1, 1)),
                lagOppholdsadresse(gyldigTilOgMed = LocalDate.of(2017, 1, 1), gyldigFraOgMed = LocalDate.of(1997, 1, 1)),
                lagOppholdsadresse(gyldigTilOgMed = null, gyldigFraOgMed = LocalDate.of(2020, 1, 1)),
            )

        val delteBostederFør =
            listOf(
                lagDeltBosted(sluttdatoForKontrakt = LocalDate.of(2020, 1, 1), startdatoForKontrakt = LocalDate.of(2018, 1, 1)),
                lagDeltBosted(sluttdatoForKontrakt = LocalDate.of(2017, 1, 1), startdatoForKontrakt = LocalDate.of(1997, 1, 1)),
                lagDeltBosted(sluttdatoForKontrakt = null, startdatoForKontrakt = LocalDate.of(2020, 1, 1)),
            )

        val bostedsadresserEtter = bostedsadresserFør.filtrerBortBostedsadresserFørEldsteBarn(grunnlag)
        val oppholdsadresserEtter = oppholdsadresserFør.filtrerBortOppholdsadresserFørEldsteBarn(grunnlag)
        val deltBostedEtter = delteBostederFør.filtrerBortDeltBostedførEldsteBarn(grunnlag)

        assertThat(bostedsadresserEtter.size == 2)
        assertThat(oppholdsadresserEtter.size == 2)
        assertThat(deltBostedEtter.size == 2)

        assertThat(bostedsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(oppholdsadresserEtter.first().gyldigTilOgMed).isEqualTo(LocalDate.of(2020, 1, 1))
        assertThat(deltBostedEtter.first().sluttdatoForKontrakt).isEqualTo(LocalDate.of(2020, 1, 1))

        assertThat(bostedsadresserEtter.last().gyldigTilOgMed).isNull()
        assertThat(oppholdsadresserEtter.last().gyldigTilOgMed).isNull()
        assertThat(deltBostedEtter.last().sluttdatoForKontrakt).isNull()
    }
}
