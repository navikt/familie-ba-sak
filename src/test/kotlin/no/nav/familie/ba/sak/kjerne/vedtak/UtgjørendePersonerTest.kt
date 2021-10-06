package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.lagBehandling
import no.nav.familie.ba.sak.common.lagTestPersonopplysningGrunnlag
import no.nav.familie.ba.sak.common.lagVilkårResultat
import no.nav.familie.ba.sak.common.randomFnr
import no.nav.familie.ba.sak.kjerne.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.TriggesAv
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.PersonResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
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
        søkerPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2008, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        val barn1PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn1PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 11, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = søkerPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 12, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)

        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.LOVLIG_OPPHOLD,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 2, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                ),
                VilkårResultat(
                    personResultat = barn1PersonResultat,
                    vilkårType = Vilkår.GIFT_PARTNERSKAP,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2009, 11, 24),
                    periodeTom = LocalDate.of(2010, 6, 1),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        vilkårsvurdering.personResultater = setOf(søkerPersonResultat, barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårLovligOpphold = VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 6, 1)
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.INNVILGET_LOVLIG_OPPHOLD_OPPHOLDSTILLATELSE.vedtakBegrunnelseType,
            triggesAv = TriggesAv(vilkår = setOf(Vilkår.LOVLIG_OPPHOLD)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList(),
        )

        assertEquals(2, personerMedUtgjørendeVilkårLovligOpphold.size)
        assertEquals(
            listOf(søkerFnr, barn1Fnr).sorted(),
            personerMedUtgjørendeVilkårLovligOpphold.map { it.personIdent.ident }.sorted()
        )

        val personerMedUtgjørendeVilkårBosattIRiket = VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = LocalDate.of(2010, 1, 1),
                tom = LocalDate.of(2010, 6, 1)
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
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
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barnFnr, barn2Fnr),
                barnFødselsdato = LocalDate.of(2010, 12, 24)
            )

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        val barnPersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barnFnr)

        barnPersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barnPersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 12, 24),
                    periodeTom = LocalDate.of(2021, 3, 31),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn2Fnr)

        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                VilkårResultat(
                    personResultat = barn2PersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    resultat = Resultat.OPPFYLT,
                    periodeFom = LocalDate.of(2010, 12, 24),
                    periodeTom = LocalDate.of(2021, 1, 31),
                    begrunnelse = "",
                    behandlingId = vilkårsvurdering.behandling.id
                )
            )
        )

        vilkårsvurdering.personResultater = setOf(barnPersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårBosattIRiket = VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 2, 1),
                tom = TIDENES_ENDE
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.REDUKSJON_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), personTyper = setOf(PersonType.BARN)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(
            barn2Fnr,
            personerMedUtgjørendeVilkårBosattIRiket.first().personIdent.ident
        )

        val personerMedUtgjørendeVilkårBarnUtvandret = VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 4, 1),
                tom = TIDENES_ENDE
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.OPPHØR_UTVANDRET.vedtakBegrunnelseType,
            triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList() // Husk å fikse dette!
        )

        assertEquals(1, personerMedUtgjørendeVilkårBarnUtvandret.size)
        assertEquals(
            barnFnr,
            personerMedUtgjørendeVilkårBarnUtvandret.first().personIdent.ident
        )
    }

    @Test
    fun `Skal kun hente medlemskapsbegrunnelser ved medlemskap og ikke hente medlemskapsbegrunnelser ellers`() {
        val søkerFnr = randomFnr()
        val barn1Fnr = randomFnr()
        val barn2Fnr = randomFnr()

        val behandling = lagBehandling()
        val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(
                behandling.id,
                søkerFnr,
                listOf(barn1Fnr, barn2Fnr),
            )

        val vilkårsvurdering = Vilkårsvurdering(
            behandling = behandling
        )

        val barn1PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn1Fnr)
        val barn2PersonResultat = PersonResultat(vilkårsvurdering = vilkårsvurdering, personIdent = barn2Fnr)

        barn1PersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barn1PersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.of(2021, 11, 1),
                    erMedlemskapVurdert = true
                )
            )
        )
        barn2PersonResultat.setSortedVilkårResultater(
            setOf(
                lagVilkårResultat(
                    barn2PersonResultat,
                    vilkårType = Vilkår.BOSATT_I_RIKET,
                    periodeFom = LocalDate.of(2021, 11, 1),
                    erMedlemskapVurdert = false
                )
            )
        )

        vilkårsvurdering.personResultater =
            setOf(barn1PersonResultat, barn2PersonResultat)

        val personerMedUtgjørendeVilkårBosattIRiket = VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 12, 1),
                tom = TIDENES_ENDE
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET), medlemskap = true),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
        )

        val personerMedUtgjørendeVilkårBarnUtvandret = VedtakUtils.hentPersonerForAlleUtgjørendeVilkår(
            vilkårsvurdering = vilkårsvurdering,
            vedtaksperiode = Periode(
                fom = LocalDate.of(2021, 12, 1),
                tom = TIDENES_ENDE
            ),
            oppdatertBegrunnelseType = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET.vedtakBegrunnelseType,
            triggesAv = TriggesAv(vilkår = setOf(Vilkår.BOSATT_I_RIKET)),
            aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
        )

        assertEquals(1, personerMedUtgjørendeVilkårBosattIRiket.size)
        assertEquals(
            barn1Fnr,
            personerMedUtgjørendeVilkårBosattIRiket.first().personIdent.ident
        )


        assertEquals(1, personerMedUtgjørendeVilkårBarnUtvandret.size)
        assertEquals(
            barn2Fnr,
            personerMedUtgjørendeVilkårBarnUtvandret.first().personIdent.ident
        )
    }
}
