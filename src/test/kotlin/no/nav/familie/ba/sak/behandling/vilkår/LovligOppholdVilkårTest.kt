package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.kontrakter.felles.personopplysning.OPPHOLDSTILLATELSE
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovligOppholdVilkårTest {

    @Test
    fun `Ikke lovlig opphold dersom søker ikke har noen gjeldende opphold registrert`() {
        val evaluering = vilkår.spesifikasjon.evaluer(FaktaTilVilkårsvurdering(personForVurdering = tredjelandsborger))
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
    }

    @Test
    fun `Ikke lovlig opphold dersom søker er statsløs og ikke har noen gjeldende opphold registrert`() {
        val statsløsEvaluering = vilkår.spesifikasjon.evaluer(FaktaTilVilkårsvurdering(personForVurdering = statsløsPerson))
        assertThat(statsløsEvaluering.resultat).isEqualTo(Resultat.NEI)

        val ukjentStatsborgerskapEvaluering =
                vilkår.spesifikasjon.evaluer(FaktaTilVilkårsvurdering(personForVurdering = ukjentStatsborger))
        assertThat(ukjentStatsborgerskapEvaluering.resultat).isEqualTo(Resultat.NEI)
    }

    @Test
    fun `Lovlig opphold vurdert på bakgrunn av status`() {
        var evaluering = vilkår.spesifikasjon.evaluer(fakta(OPPHOLDSTILLATELSE.MIDLERTIDIG, null))
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
        evaluering = vilkår.spesifikasjon.evaluer(fakta(OPPHOLDSTILLATELSE.PERMANENT, null))
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
        evaluering = vilkår.spesifikasjon.evaluer(fakta(OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, null))
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
    }

    @Test
    fun `Lovlig opphold vurdert på bakgrunn av status for statsløs søker`() {
        var fakta = FaktaTilVilkårsvurdering(personForVurdering = statsløsPerson.copy(
        ).apply {
            opphold = listOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
        })
        var evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)

        fakta = FaktaTilVilkårsvurdering(personForVurdering = ukjentStatsborger.copy(
        ).apply {
            opphold = listOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
        })
        evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)

        fakta = FaktaTilVilkårsvurdering(personForVurdering = statsløsPerson.copy(
        ).apply {
            opphold = listOf(GrOpphold(gyldigPeriode = null, type = OPPHOLDSTILLATELSE.OPPLYSNING_MANGLER, person = this))
        })
        evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
    }

    @Test
    fun `Ikke lovlig opphold dersom utenfor gyldig periode`() {
        var fakta = FaktaTilVilkårsvurdering(personForVurdering = tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                                fom = LocalDate.now().minusYears(10),
                                tom = LocalDate.now().minusYears(5)),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = tredjelandsborger))
        ))
        var evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)

        fakta = FaktaTilVilkårsvurdering(personForVurdering = statsløsPerson.copy().apply {
            opphold = listOf(GrOpphold(
                    gyldigPeriode = DatoIntervallEntitet(
                            fom = LocalDate.now().minusYears(10),
                            tom = LocalDate.now().minusYears(5)),
                    type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = this))
        })
        evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
    }

    @Test
    fun `Lovlig opphold dersom status med gjeldende periode`() {
        var fakta = FaktaTilVilkårsvurdering(personForVurdering = tredjelandsborger.copy(
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
        ))
        var evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)

        fakta = FaktaTilVilkårsvurdering(personForVurdering = statsløsPerson.copy().apply {
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

        })
        evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }

    @Test
    fun `Lovlig opphold gir resultat JA for barn ved fødselshendelse`() {
        val fakta = FaktaTilVilkårsvurdering(personForVurdering = barn)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }

    private fun fakta(oppholdstillatelse: OPPHOLDSTILLATELSE, periode: DatoIntervallEntitet?): FaktaTilVilkårsvurdering {
        return FaktaTilVilkårsvurdering(personForVurdering = tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(GrOpphold(gyldigPeriode = periode, type = oppholdstillatelse, person = tredjelandsborger))
        ))
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