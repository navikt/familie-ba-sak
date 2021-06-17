package no.nav.familie.ba.sak.kjerne.fødselshendelse.regler

import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.FaktaTilVilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class GiftEllerPartnerskapVilkårTest {

    @Test
    fun `Gift-vilkår gir resultat JA for fødselshendelse når sivilstand er uoppgitt`() {
        val fakta = FaktaTilVilkårsvurdering(personForVurdering = barn)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.OPPFYLT)
    }

    companion object {

        val vilkår = Vilkår.GIFT_PARTNERSKAP
        val barn =
                tilfeldigPerson(personType = PersonType.BARN).apply {
                    sivilstander = listOf(GrSivilstand(type = SIVILSTAND.UOPPGITT, person = this))
                }
    }
}