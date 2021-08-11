package no.nav.familie.ba.sak.kjerne.fødselshendelse.vilkårsvurdering

import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Disabled
class LovligOppholdVilkårTest {

    @Test
    fun `Ikke lovlig opphold dersom søker ikke har noen gjeldende opphold registrert`() {
        val evaluering = vilkår.vurderVilkår(tredjelandsborger).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Ikke lovlig opphold dersom søker er statsløs og ikke har noen gjeldende opphold registrert`() {
        val statsløsEvaluering = vilkår.vurderVilkår(statsløsPerson).evaluering
        assertThat(statsløsEvaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)

        val ukjentStatsborgerskapEvaluering =
                vilkår.vurderVilkår(ukjentStatsborger).evaluering
        assertThat(ukjentStatsborgerskapEvaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Lovlig opphold vurdert på bakgrunn av status`() {
        var evaluering = vilkår.vurderVilkår(faktaPerson(OPPHOLDSTILLATELSE.MIDLERTIDIG, null)).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
        evaluering = vilkår.vurderVilkår(faktaPerson(OPPHOLDSTILLATELSE.PERMANENT, null)).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
        evaluering = vilkår.vurderVilkår(faktaPerson(OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, null)).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Lovlig opphold vurdert på bakgrunn av status for statsløs søker`() {
        var evaluering = vilkår.vurderVilkår(statsløsPerson.copy(
        ).apply {
            opphold = listOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
        }).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)

        evaluering = vilkår.vurderVilkår(ukjentStatsborger.copy(
        ).apply {
            opphold = listOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
        }).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)

        evaluering = vilkår.vurderVilkår(statsløsPerson.copy(
        ).apply {
            opphold = listOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, person = this))
        }).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Ikke lovlig opphold dersom utenfor gyldig periode`() {
        var evaluering = vilkår.vurderVilkår(tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                                fom = LocalDate.now().minusYears(10),
                                tom = LocalDate.now().minusYears(5)),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = tredjelandsborger))
        )).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)

        evaluering = vilkår.vurderVilkår(statsløsPerson.copy().apply {
            opphold = listOf(GrOpphold(
                    gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(10),
                            tom = LocalDate.now().minusYears(5)),
                    type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
        }).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.IKKE_OPPFYLT)
    }

    @Test
    fun `Lovlig opphold dersom status med gjeldende periode`() {
        var evaluering = vilkår.vurderVilkår(tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(
                        GrOpphold(
                                gyldigPeriode = DatoIntervallEntitet(
                                        fom = LocalDate.now().minusYears(10),
                                        tom = LocalDate.now().minusYears(5)),
                                type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, person = tredjelandsborger),
                        GrOpphold(
                                gyldigPeriode = DatoIntervallEntitet(
                                        fom = LocalDate.now().minusYears(5),
                                        tom = null),
                                type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = tredjelandsborger)
                )
        )).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)

        evaluering = vilkår.vurderVilkår(statsløsPerson.copy().apply {
            opphold = listOf(
                    GrOpphold(
                            gyldigPeriode = DatoIntervallEntitet(
                                    fom = LocalDate.now().minusYears(10),
                                    tom = LocalDate.now().minusYears(5)),
                            type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, person = this),
                    GrOpphold(
                            gyldigPeriode = DatoIntervallEntitet(
                                    fom = LocalDate.now().minusYears(5),
                                    tom = null),
                            type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this)
            )
        }).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    @Test
    fun `Lovlig opphold gir resultat JA for barn ved fødselshendelse`() {
        val evaluering = vilkår.vurderVilkår(barn).evaluering
        assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    private fun faktaPerson(oppholdstillatelse: OPPHOLDSTILLATELSE, periode: DatoIntervallEntitet?): Person {
        return tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(GrOpphold(gyldigPeriode = periode, type = oppholdstillatelse, person = tredjelandsborger))
        )
    }

    companion object {

        val vilkår = Vilkår.LOVLIG_OPPHOLD
        val tredjelandsborger = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = listOf(GrStatsborgerskap(
                    landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = this))
        }
        val statsløsPerson = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = listOf(GrStatsborgerskap(
                    landkode = "XXX", medlemskap = Medlemskap.STATSLØS, person = this))
        }
        val ukjentStatsborger = tilfeldigPerson(personType = PersonType.SØKER).apply {
            statsborgerskap = listOf(GrStatsborgerskap(
                    landkode = "XUK", medlemskap = Medlemskap.UKJENT, person = this))
        }

        val barn = tilfeldigPerson(personType = PersonType.BARN)
    }
}