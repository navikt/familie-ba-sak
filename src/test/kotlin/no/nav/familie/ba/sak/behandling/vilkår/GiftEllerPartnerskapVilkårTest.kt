package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.sivilstand.GrSivilstand
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.ba.sak.nare.Resultat
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
                    sivilstandHistorisk = listOf(GrSivilstand(type = SIVILSTAND.UOPPGITT, person = this))
                }
    }
}