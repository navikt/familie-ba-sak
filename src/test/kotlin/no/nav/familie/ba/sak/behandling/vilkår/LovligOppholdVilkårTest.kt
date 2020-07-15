package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Medlemskap
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.opphold.GrOpphold
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.statsborgerskap.GrStatsborgerskap
import no.nav.familie.ba.sak.common.DatoIntervallEntitet
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.kontrakter.felles.personinfo.OPPHOLDSTILLATELSE
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovligOppholdVilkårTest {

    @Test
    fun `Ikke lovlig opphold dersom søker ikke har noen gjeldende opphold registrert`() {
        val evaluering = vilkår.spesifikasjon.evaluer(Fakta(personForVurdering = tredjelandsborger))
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
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
    fun `Ikke lovlig opphold dersom utenfor gyldig periode`() {
        val fakta = Fakta(personForVurdering = tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(GrOpphold(
                        gyldigPeriode = DatoIntervallEntitet(
                                fom = LocalDate.now().minusYears(10),
                                tom = LocalDate.now().minusYears(5)),
                        type = OPPHOLDSTILLATELSE.MIDLERTIDIG, person = tredjelandsborger))
        ))
        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
    }

    @Test
    fun `Lovlig opphold dersom status med gjeldende periode`() {
        val fakta = Fakta(personForVurdering = tredjelandsborger.copy(
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
        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }

    private fun fakta(oppholdstillatelse: OPPHOLDSTILLATELSE, periode: DatoIntervallEntitet?): Fakta {
        return Fakta(personForVurdering = tredjelandsborger.copy(
                statsborgerskap = listOf(GrStatsborgerskap(
                        landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = tredjelandsborger)),
                opphold = listOf(GrOpphold(gyldigPeriode = periode, type = oppholdstillatelse, person = tredjelandsborger))
        ))
    }

    companion object {
        val vilkår = Vilkår.LOVLIG_OPPHOLD
        val tredjelandsborger = tilfeldigPerson().apply {
            statsborgerskap = listOf(GrStatsborgerskap(
                    landkode = "ANG", medlemskap = Medlemskap.TREDJELANDSBORGER, person = this))
        }
    }
}