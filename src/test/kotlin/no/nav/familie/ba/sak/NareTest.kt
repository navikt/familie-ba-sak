package no.nav.familie.ba.sak

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.behandling.domene.*
import no.nav.familie.ba.sak.behandling.domene.vilkår.VilkårType
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkårsvurdering.rettenTilBarnetrygd
import no.nav.familie.ba.sak.personopplysninger.domene.AktørId
import no.nav.familie.ba.sak.personopplysninger.domene.PersonIdent
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test

class NareTest {
    @Test
    fun `Nare gir forventet resultat`() {
        val behandling = Behandling(1,
                                    Fagsak(1, AktørId("1"), PersonIdent("1"), FagsakStatus.LØPENDE),
                                    "",
                                    BehandlingType.FØRSTEGANGSBEHANDLING,
                                    "1",
                                    BehandlingKategori.NASJONAL,
                                    BehandlingUnderkategori.ORDINÆR,
                                    true,
                                    BehandlingStatus.FERDIGSTILT,
                                    StegType.GODKJENNE_VEDTAK,
                                    BehandlingResultat.INNVILGET,
                                    "")
        val vilkårtype = rettenTilBarnetrygd.vilkårType
        val evaluering = rettenTilBarnetrygd.evaluer(behandling)
        assertEquals(evaluering.resultat, Resultat.JA)
        assertEquals(vilkårtype, VilkårType.UNDER_18_ÅR_OG_BOR_MED_SØKER)
    }
}



