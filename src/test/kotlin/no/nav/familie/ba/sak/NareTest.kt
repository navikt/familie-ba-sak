package no.nav.familie.ba.sak

import junit.framework.Assert.assertEquals
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.Kjønn
import no.nav.familie.ba.sak.behandling.domene.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.domene.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.domene.vilkår.Fakta
import no.nav.familie.ba.sak.behandling.domene.vilkår.under18årOgBorMedSøker
import no.nav.familie.ba.sak.integrasjoner.domene.FAMILIERELASJONSROLLE
import no.nav.familie.ba.sak.integrasjoner.domene.Familierelasjoner
import no.nav.familie.ba.sak.integrasjoner.domene.Personident
import no.nav.familie.ba.sak.integrasjoner.domene.Personinfo
import no.nav.nare.core.evaluations.Resultat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class NareTest {
    /*
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
     */
    val personinfo = Personinfo(fødselsdato = LocalDate.of(1990, 2, 19),
                                kjønn = Kjønn.KVINNE,
                                navn = "Mor Moresen",
                                familierelasjoner = setOf(Familierelasjoner(Personident("1"), FAMILIERELASJONSROLLE.BARN)))
    val fakta = Fakta(personinfo)

    @Test
    fun `Nare gir forventet resultat`() {
        val evaluering = under18årOgBorMedSøker.evaluer(fakta)
        assertEquals(evaluering.resultat, Resultat.JA)
    }

    @Test
    fun `Hent relevante vilkår for persontype med alt i en klasse`() {
        val relevanteVilkår = Vilkår.hentVilkårFor(PersonType.BARN, "TESTSAKSTYPE")
        val vilkårForBarn = setOf(Vilkår.UNDER_18_ÅR_OG_BOR_MED_SØKER,
                                  Vilkår.STØNADSPERIODE,
                                  Vilkår.BOSATT_I_RIKET,
                                  Vilkår.BARN_HAR_RETT_TIL)
        assertEquals(vilkårForBarn, relevanteVilkår)
    }


    @Test
    fun `Hent og evaluer vilkår for persontype`() {
        val relevanteVilkårForBarn = Vilkår.hentVilkårFor(PersonType.BARN, "TESTSAKSTYPE")
        val samletSpesifikasjon = relevanteVilkårForBarn
                .map { vilkår -> vilkår.spesifikasjon }
                .reduce { samledeVilkår, vilkår -> samledeVilkår og vilkår }
        assertEquals(samletSpesifikasjon.evaluer(fakta).resultat, Resultat.JA)
    }

}



