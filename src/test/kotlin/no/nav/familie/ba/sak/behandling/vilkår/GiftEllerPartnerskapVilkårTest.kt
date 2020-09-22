package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.common.tilfeldigPerson
import no.nav.familie.kontrakter.felles.personopplysning.SIVILSTAND
import no.nav.nare.core.evaluations.Resultat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class GiftEllerPartnerskapVilkårTest {

    @Test
    fun `Gift-vilkår gir resultat JA for fødselshendelse når sivilstand er uoppgitt`() {
        val fakta = FaktaTilVilkårsvurdering(personForVurdering = barn, behandlingOpprinnelse = BehandlingOpprinnelse.AUTOMATISK_VED_FØDSELSHENDELSE)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
    }

    @Test
    fun `Gift-vilkår gir resultat KANSKJE for manuell behandling når sivilstand er uoppgitt`() {
        val fakta = FaktaTilVilkårsvurdering(personForVurdering = barn, behandlingOpprinnelse = BehandlingOpprinnelse.MANUELL)

        val evaluering = vilkår.spesifikasjon.evaluer(fakta)
        Assertions.assertThat(evaluering.resultat).isEqualTo(Resultat.KANSKJE)
    }

    companion object {
        val vilkår = Vilkår.GIFT_PARTNERSKAP
        val barn = tilfeldigPerson(personType = PersonType.BARN).copy(sivilstand = SIVILSTAND.UOPPGITT)
    }
}