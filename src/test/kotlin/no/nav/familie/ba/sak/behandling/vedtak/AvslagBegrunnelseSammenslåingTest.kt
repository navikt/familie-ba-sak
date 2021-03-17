package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.nare.Resultat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate


class AvslagBegrunnelseSammenslåingTest {

    val søkerFnr = randomFnr()
    val barnFnr = randomFnr()
    val barnFødselsdato = LocalDate.of(1999, 1, 1)
    val randomVilkårsvurdering = Vilkårsvurdering(behandling = lagBehandling())
    val randomVedtak = lagVedtak()
    val personopplysningGrunnlag =
            lagTestPersonopplysningGrunnlag(1, søkerFnr, listOf(barnFnr), barnFødselsdato = barnFødselsdato)
    val barnPersonResultat = PersonResultat(vilkårsvurdering = randomVilkårsvurdering, personIdent = barnFnr)
    val søkerPersonResultat = PersonResultat(vilkårsvurdering = randomVilkårsvurdering, personIdent = søkerFnr)
    val avslagVilkår = Vilkår.BOSATT_I_RIKET
    val avslagFom = LocalDate.of(2000, 1, 1)
    val avslagTom = LocalDate.of(2010, 1, 1)
    val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET

    @Test
    fun `Avslagbegrunnelser med samme begrunnelse og datoer slås sammen`() {

        val vilkårResultatBarn = ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår)
        val vilkårResultatSøker = ikkeOppfyltVilkårResultat(personResultat = søkerPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagFom,
                                                          tom = avslagTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagFom,
                                                          tom = avslagTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatSøker))

        val sammenslåttBegrunnelse =
                VedtakService.sammenslåtteAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag).singleOrNull()
        Assertions.assertEquals(avslagFom, sammenslåttBegrunnelse?.fom)
        Assertions.assertEquals(avslagTom, sammenslåttBegrunnelse?.tom)
        Assertions.assertEquals(setOf(søkerFnr, barnFnr), sammenslåttBegrunnelse?.personer?.toSet())
        Assertions.assertEquals(Vilkår.BOSATT_I_RIKET, sammenslåttBegrunnelse?.vilkår)
        Assertions.assertEquals("Du og barn født 01.01.99 ikke er bosatt i Norge fra januar 2000 til januar 2010.",
                                sammenslåttBegrunnelse?.brevBegrunnelse)
    }

    @Test
    fun `Avslagbegrunnelser med samme begrunnelse og ulike datoer slås IKKE sammen`() {
        val ulikAvslagFomDato = LocalDate.of(2000, 2, 1)

        val vilkårResultatBarn = ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår)
        val vilkårResultatSøker = ikkeOppfyltVilkårResultat(personResultat = søkerPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = ulikAvslagFomDato,
                                                          tom = avslagTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagFom,
                                                          tom = avslagTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatSøker))

        val sammenslåtteBegrunnelser =
                VedtakService.sammenslåtteAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        Assertions.assertEquals(2, sammenslåtteBegrunnelser.size)
        Assertions.assertEquals(setOf("Barn født 01.01.99 ikke er bosatt i Norge fra februar 2000 til januar 2010.",
                                      "Du ikke er bosatt i Norge fra januar 2000 til januar 2010."),
                                sammenslåtteBegrunnelser.map { it.brevBegrunnelse }.toSet())
    }

    @Test
    fun `Avslagbegrunnelser med ulike begrunnelser og samme datoer slås IKKE sammen`() {
        val ulikBegrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_MEDLEM_I_FOLKETRYGDEN

        val vilkårResultatBarn = ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår)
        val vilkårResultatSøker = ikkeOppfyltVilkårResultat(personResultat = søkerPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagFom,
                                                          tom = avslagTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagFom,
                                                          tom = avslagTom,
                                                          begrunnelse = ulikBegrunnelse,
                                                          vilkårResultat = vilkårResultatSøker))

        val sammenslåtteBegrunnelser =
                VedtakService.sammenslåtteAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        Assertions.assertEquals(2, sammenslåtteBegrunnelser.size)
        Assertions.assertEquals(setOf("Barn født 01.01.99 ikke er bosatt i Norge fra januar 2000 til januar 2010.",
                                      "Du ikke er medlem av folketrygden fra januar 2000 til januar 2010."),
                                sammenslåtteBegrunnelser.map { it.brevBegrunnelse }.toSet())
    }


    @Test
    fun `Forsøk på å slå sammen begrunnelser som ikke er av typen avslag kaster feil`() {
        val oppfyltVilkårResultatBarn = VilkårResultat(personResultat = barnPersonResultat,
                                                       vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                       resultat = Resultat.OPPFYLT,
                                                       periodeFom = avslagFom,
                                                       periodeTom = avslagFom,
                                                       begrunnelse = "",
                                                       behandlingId = 1,
                                                       regelInput = null,
                                                       regelOutput = null)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagFom,
                                                          tom = avslagTom,
                                                          begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                                                          vilkårResultat = oppfyltVilkårResultatBarn))
        assertThrows<Feil> {
            VedtakService.sammenslåtteAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        }
    }
}

private fun ikkeOppfyltVilkårResultat(personResultat: PersonResultat,
                                      vilkårType: Vilkår) = VilkårResultat(personResultat = personResultat,
                                                                           vilkårType = vilkårType,
                                                                           resultat = Resultat.IKKE_OPPFYLT,
                                                                           periodeFom = null,
                                                                           periodeTom = null,
                                                                           begrunnelse = "",
                                                                           behandlingId = 1,
                                                                           regelInput = null,
                                                                           regelOutput = null)