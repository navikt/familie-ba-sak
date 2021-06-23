package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtgjørendePersonerTest {

    @Test
    fun `Skal hente riktige personer fra vilkårsvurderingen basert på innvilgelsesbegrunnelse`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id, søkerFnr, listOf(barn1Fnr, barn2Fnr))

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val søkerPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = søkerFnr)
        søkerPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.LOVLIG_OPPHOLD,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        regelInput = null,
                        regelOutput = null),
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.of(2008, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        regelInput = null,
                        regelOutput = null)))

        val barn1PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn1PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 12, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(
                        personResultat = søkerPersonResultat,
                        vilkårType = Vilkår.BOSATT_I_RIKET,
                        resultat = Resultat.OPPFYLT,
                        periodeFom = LocalDate.of(2009, 12, 24),
                        periodeTom = LocalDate.of(2010, 6, 1),
                        begrunnelse = "",
                        behandlingId = vilkårsvurdering.behandling.id,
                        regelInput = null,
                        regelOutput = null)))

        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn2PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.LOVLIG_OPPHOLD,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 2, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null),
                VilkårResultat(personResultat = barn1PersonResultat,
                               vilkårType = Vilkår.GIFT_PARTNERSKAP,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2009, 11, 24),
                               periodeTom = LocalDate.of(2010, 6, 1),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårLovligOpphold = VedtakUtils.hentPersonerMedUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(fom = LocalDate.of(2010, 1, 1),
                                         tom = LocalDate.of(2010, 6, 1)),
                oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE.vedtakBegrunnelseType,
                utgjørendeVilkår = VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE.finnVilkårFor(),
                personerPåBehandling = personopplysningGrunnlag.personer.toList()

        )

        assertEquals(2, personerMedUtgjørendeVilkårLovligOpphold.size)
        assertEquals(listOf(søkerFnr, barn1Fnr).sorted(),
                     personerMedUtgjørendeVilkårLovligOpphold.map { it.personIdent.ident }.sorted())

        val personerMedUtgjørendeVilkårBosattIRiket = VedtakUtils.hentPersonerMedUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(fom = LocalDate.of(2010, 1, 1),
                                         tom = LocalDate.of(2010, 6, 1)),
                oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET.vedtakBegrunnelseType,
                utgjørendeVilkår = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET.finnVilkårFor(),
                personerPåBehandling = personopplysningGrunnlag.personer.toList()
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(barn1Fnr, personerMedUtgjørendeVilkårBosattIRiket.first().personIdent.ident)
    }


    @Test
    fun `Skal hente riktige personer fra vilkårsvurderingen basert på reduksjon og opphørsbegrunnelser`() {
        val søkerFnr = randomFnr()
        val barnFnr = randomFnr()
        val barn2Fnr = randomFnr()

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
                lagTestPersonopplysningGrunnlag(behandling.id,
                                                søkerFnr,
                                                listOf(barnFnr, barn2Fnr),
                                                barnFødselsdato = LocalDate.of(2010, 12, 24))

        val vilkårsvurdering = Vilkårsvurdering(
                behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        barnPersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barnPersonResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 12, 24),
                               periodeTom = LocalDate.of(2021, 3, 31),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))

        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn2Fnr)

        barn2PersonResultat.setSortedVilkårResultater(setOf(
                VilkårResultat(personResultat = barn2PersonResultat,
                               vilkårType = Vilkår.BOSATT_I_RIKET,
                               resultat = Resultat.OPPFYLT,
                               periodeFom = LocalDate.of(2010, 12, 24),
                               periodeTom = LocalDate.of(2021, 1, 31),
                               begrunnelse = "",
                               behandlingId = vilkårsvurdering.behandling.id,
                               regelInput = null,
                               regelOutput = null)))


        vilkårsvurdering.personResultater = setOf(barnPersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårBosattIRiket = VedtakUtils.hentPersonerMedUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(fom = LocalDate.of(2021, 2, 1),
                                         tom = TIDENES_ENDE),
                oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET.vedtakBegrunnelseType,
                utgjørendeVilkår = VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET.finnVilkårFor(),
                personerPåBehandling = personopplysningGrunnlag.personer.toList()
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(barn2Fnr,
                     personerMedUtgjørendeVilkårBosattIRiket.first().personIdent.ident)

        val personerMedUtgjørendeVilkårBarnUtvandret = VedtakUtils.hentPersonerMedUtgjørendeVilkår(
                vilkårsvurdering = vilkårsvurdering,
                vedtaksperiode = Periode(fom = LocalDate.of(2021, 4, 1),
                                         tom = TIDENES_ENDE),
                oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_UTVANDRET.vedtakBegrunnelseType,
                utgjørendeVilkår = VedtakBegrunnelseSpesifikasjon.OPPHØR_BARN_UTVANDRET.finnVilkårFor(),
                personerPåBehandling = personopplysningGrunnlag.personer.toList()
        )

        assertEquals(1, personerMedUtgjørendeVilkårBarnUtvandret.size)
        assertEquals(barnFnr,
                     personerMedUtgjørendeVilkårBarnUtvandret.first().personIdent.ident)
    }
}