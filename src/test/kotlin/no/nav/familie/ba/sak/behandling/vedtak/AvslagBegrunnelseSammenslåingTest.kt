package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService.Companion.BrevParameterComparator
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
    val avslagVilkårPeriodeFom = LocalDate.of(2000, 1, 1)
    val avslagVilkårPeriodeTom = LocalDate.of(2010, 1, 1)
    val begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET

    private fun ikkeOppfyltVilkårResultat(personResultat: PersonResultat,
                                          vilkårType: Vilkår,
                                          fom: LocalDate = avslagVilkårPeriodeFom,
                                          tom: LocalDate = avslagVilkårPeriodeTom) = VilkårResultat(personResultat = personResultat,
                                                                                                    vilkårType = vilkårType,
                                                                                                    resultat = Resultat.IKKE_OPPFYLT,
                                                                                                    periodeFom = fom,
                                                                                                    periodeTom = tom,
                                                                                                    begrunnelse = "",
                                                                                                    behandlingId = 1,
                                                                                                    regelInput = null,
                                                                                                    regelOutput = null)

    @Test
    fun `Avslagbegrunnelser med samme begrunnelse og datoer slås sammen`() {

        val vilkårResultatBarn = ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår)
        val vilkårResultatSøker = ikkeOppfyltVilkårResultat(personResultat = søkerPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatBarn.vedtaksperiodeFom,
                                                          tom = vilkårResultatBarn.vedtaksperiodeTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatSøker.vedtaksperiodeFom,
                                                          tom = vilkårResultatSøker.vedtaksperiodeTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatSøker))

        val sammenslåttBegrunnelse =
                VedtakService.mapTilRestAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag).singleOrNull()
        Assertions.assertEquals(vilkårResultatBarn.vedtaksperiodeFom, sammenslåttBegrunnelse?.fom)
        Assertions.assertEquals(vilkårResultatBarn.vedtaksperiodeTom, sammenslåttBegrunnelse?.tom)
        Assertions.assertEquals("Barnetrygd for barn født 01.01.99 fordi du og barnet ikke er bosatt i Norge fra januar 2000 til januar 2010.",
                                sammenslåttBegrunnelse?.brevBegrunnelser?.singleOrNull())
    }

    @Test
    fun `Avslagbegrunnelser med samme begrunnelse og ulike datoer slås IKKE sammen`() {
        val ulikAvslagFomDato = LocalDate.of(2000, 2, 1)

        val vilkårResultatBarn =
                ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår, fom = ulikAvslagFomDato)
        val vilkårResultatSøker = ikkeOppfyltVilkårResultat(personResultat = søkerPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatBarn.vedtaksperiodeTom,
                                                          tom = vilkårResultatBarn.vedtaksperiodeTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatSøker.vedtaksperiodeFom,
                                                          tom = vilkårResultatSøker.vedtaksperiodeTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatSøker))

        val sammenslåtteBegrunnelser =
                VedtakService.mapTilRestAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        Assertions.assertEquals(2, sammenslåtteBegrunnelser.size)
        Assertions.assertEquals(setOf("Barnetrygd for barn født 01.01.99 fordi barnet ikke er bosatt i Norge fra februar 2000 til januar 2010.",
                                      "Barnetrygd for barn født  fordi du ikke er bosatt i Norge fra januar 2000 til januar 2010."),
                                sammenslåtteBegrunnelser.flatMap { it.brevBegrunnelser }.toSet())
    }

    @Test
    fun `Avslagbegrunnelser med ulike begrunnelser og samme datoer slås IKKE sammen`() {
        val ulikBegrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_MEDLEM_I_FOLKETRYGDEN

        val vilkårResultatBarn = ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår)
        val vilkårResultatSøker = ikkeOppfyltVilkårResultat(personResultat = søkerPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatBarn.vedtaksperiodeFom,
                                                          tom = vilkårResultatBarn.vedtaksperiodeTom,
                                                          begrunnelse = begrunnelse,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatSøker.vedtaksperiodeFom,
                                                          tom = vilkårResultatSøker.vedtaksperiodeTom,
                                                          begrunnelse = ulikBegrunnelse,
                                                          vilkårResultat = vilkårResultatSøker))

        val sammenslåtteBegrunnelser =
                VedtakService.mapTilRestAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        Assertions.assertEquals(setOf("Barnetrygd for barn født 01.01.99 fordi barnet ikke er bosatt i Norge fra januar 2000 til januar 2010.",
                                      "Barnetrygd fordi du ikke er medlem av folketrygden fra januar 2000 til januar 2010."),
                                sammenslåtteBegrunnelser.flatMap { it.brevBegrunnelser }.toSet())
    }

    @Test
    fun `Avslagbegrunnelser for friteks blir filtrert vekk for restobjekter`() {

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = LocalDate.now(),
                                                          tom = LocalDate.now(),
                                                          begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST))

        val restAvslagBegrunnelser =
                VedtakService.mapTilRestAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        Assertions.assertTrue(restAvslagBegrunnelser.isEmpty())
    }

    @Test
    fun `Avslagbegrunnelser i samme periode blir IKKE slått sammen og lagt til ved mapping`() {
        val fritekst1 = "Fritekst 1"
        val fritekst2 = "Fritekst 2"

        val vilkårResultatBarn = ikkeOppfyltVilkårResultat(personResultat = barnPersonResultat, vilkårType = avslagVilkår)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatBarn.vedtaksperiodeFom,
                                                          tom = vilkårResultatBarn.vedtaksperiodeTom,
                                                          begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST,
                                                          brevBegrunnelse = fritekst1,
                                                          vilkårResultat = vilkårResultatBarn),
                                        VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = vilkårResultatBarn.vedtaksperiodeFom,
                                                          tom = vilkårResultatBarn.vedtaksperiodeTom,
                                                          begrunnelse = VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST,
                                                          brevBegrunnelse = fritekst2,
                                                          vilkårResultat = vilkårResultatBarn))

        val sammenslåtteBegrunnelser =
                VedtakService.mapAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        Assertions.assertEquals(1,
                                sammenslåtteBegrunnelser.size)
        Assertions.assertEquals(setOf(fritekst1, fritekst2),
                                sammenslåtteBegrunnelser.getValue(
                                        NullablePeriode(vilkårResultatBarn.vedtaksperiodeFom,
                                                        vilkårResultatBarn.vedtaksperiodeTom)).toSet())
    }

    @Test
    fun `Avslagbegrunnelser i samme periode blir sortert på søker, så barnas fødselsdatoer`() {
        val kunSøker = VedtakService.Companion.BrevtekstParametre(gjelderSøker = true,
                                                                  barnasFødselsdatoer = emptyList(),
                                                                  målform = Målform.NN)
        val beggeDeler = VedtakService.Companion.BrevtekstParametre(gjelderSøker = true,
                                                                    barnasFødselsdatoer = listOf(barnFødselsdato),
                                                                    målform = Målform.NN)
        val kunBarn = VedtakService.Companion.BrevtekstParametre(gjelderSøker = false,
                                                                 barnasFødselsdatoer = listOf(barnFødselsdato),
                                                                 målform = Målform.NN)
        val sortert =
                mapOf(VedtakBegrunnelseSpesifikasjon.AVSLAG_UNDER_18_ÅR to kunBarn,
                      VedtakBegrunnelseSpesifikasjon.AVSLAG_BOSATT_I_RIKET to beggeDeler,
                      VedtakBegrunnelseSpesifikasjon.AVSLAG_LOVLIG_OPPHOLD_EØS_BORGER to kunSøker).entries.sortedWith(
                        BrevParameterComparator)
                        .toList()
        Assertions.assertEquals(kunSøker, sortert[0].value)
        Assertions.assertEquals(beggeDeler, sortert[1].value)
        Assertions.assertEquals(kunBarn, sortert[2].value)
    }

    @Test
    fun `Forsøk på å slå sammen begrunnelser som ikke er av typen avslag kaster feil`() {
        val oppfyltVilkårResultatBarn = VilkårResultat(personResultat = barnPersonResultat,
                                                       vilkårType = Vilkår.LOVLIG_OPPHOLD,
                                                       resultat = Resultat.OPPFYLT,
                                                       periodeFom = avslagVilkårPeriodeFom,
                                                       periodeTom = avslagVilkårPeriodeTom,
                                                       begrunnelse = "",
                                                       behandlingId = 1,
                                                       regelInput = null,
                                                       regelOutput = null)

        val vedtakBegrunnelser = listOf(VedtakBegrunnelse(vedtak = randomVedtak,
                                                          fom = avslagVilkårPeriodeFom,
                                                          tom = avslagVilkårPeriodeTom,
                                                          begrunnelse = VedtakBegrunnelseSpesifikasjon.INNVILGET_BOSATT_I_RIKTET,
                                                          vilkårResultat = oppfyltVilkårResultatBarn))
        assertThrows<Feil> {
            VedtakService.mapTilRestAvslagBegrunnelser(vedtakBegrunnelser, personopplysningGrunnlag)
        }
    }
}