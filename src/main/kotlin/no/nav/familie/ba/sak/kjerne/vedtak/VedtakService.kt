package no.nav.familie.ba.sak.kjerne.vedtak

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.ekstern.restDomene.RestAvslagBegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.tilVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.behandling.BehandlingService
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingStatus
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.dokument.DokumentService
import no.nav.familie.ba.sak.kjerne.fagsak.Beslutning
import no.nav.familie.ba.sak.kjerne.fødselshendelse.nare.Resultat
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.logg.LoggService
import no.nav.familie.ba.sak.kjerne.steg.StegType
import no.nav.familie.ba.sak.kjerne.tilbakekreving.TilbakekrevingService
import no.nav.familie.ba.sak.kjerne.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentPersonerMedUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseUtils
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.toVedtakFritekstBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.AnnenVurderingType
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class VedtakService(
        private val behandlingService: BehandlingService,
        private val vilkårsvurderingService: VilkårsvurderingService,
        private val persongrunnlagService: PersongrunnlagService,
        private val loggService: LoggService,
        private val vedtakRepository: VedtakRepository,
        private val dokumentService: DokumentService,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
        private val tilbakekrevingService: TilbakekrevingService,
) {

    fun opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        val skalSendesBrev =
                !vedtak.behandling.erTekniskOpphør()
                && vedtak.behandling.opprettetÅrsak != BehandlingÅrsak.SATSENDRING

        return if (skalSendesBrev) oppdaterVedtakMedStønadsbrev(vedtak = vedtak) else vedtak
    }

    @Transactional
    fun leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse, fagsakId: Long): List<VedtakBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId)
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(vedtak.behandling.id)
                                       ?: throw Feil("Finner ikke personopplysninggrunnlag ved fastsetting av begrunnelse")

        val brevBegrunnelse =
                if (restPostVedtakBegrunnelse.vedtakBegrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET) {
                    restPostVedtakBegrunnelse.vedtakBegrunnelse.hentBeskrivelse(
                            gjelderSøker = false,
                            barnasFødselsdatoer = emptyList(),
                            månedOgÅrBegrunnelsenGjelderFor = restPostVedtakBegrunnelse.vedtakBegrunnelse.vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                    periode = Periode(fom = restPostVedtakBegrunnelse.fom,
                                                      tom = restPostVedtakBegrunnelse.tom ?: TIDENES_ENDE)
                            ),
                            målform = personopplysningGrunnlag.søker.målform
                    )
                } else {
                    lagBrevBegrunnelseForUtbetalingEllerOpphør(restPostVedtakBegrunnelse, vedtak, personopplysningGrunnlag)
                }


        vedtak.leggTilBegrunnelse(restPostVedtakBegrunnelse.tilVedtakBegrunnelse(vedtak, brevBegrunnelse))

        oppdater(vedtak)

        return vedtak.vedtakBegrunnelser.toList()
    }

    fun lagBrevBegrunnelseForUtbetalingEllerOpphør(restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse,
                                                   vedtak: Vedtak,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag): String {

        val vedtakBegrunnelseType = restPostVedtakBegrunnelse.vedtakBegrunnelse.vedtakBegrunnelseType
        val vedtakBegrunnelse = restPostVedtakBegrunnelse.vedtakBegrunnelse

        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(vedtak.behandling.id)
                               ?: throw Feil("Finner ikke vilkårsvurdering ved fastsetting av begrunnelse")

        val personerMedUtgjørendeVilkårForUtbetalingsperiode =
                when (vedtakBegrunnelse) {
                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR -> persongrunnlagService.hentAktiv(vilkårsvurdering.behandling.id)
                                                                                   ?.personer
                                                                                   ?.filter {
                                                                                       it.hentSeksårsdag()
                                                                                               .toYearMonth() == restPostVedtakBegrunnelse.fom.toYearMonth()
                                                                                   } ?: listOf()

                    VedtakBegrunnelseSpesifikasjon.REDUKSJON_MANGLENDE_OPPLYSNINGER, VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER
                    -> if (harPersonerManglerOpplysninger(vilkårsvurdering))
                        emptyList() else error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")

                    else ->
                        hentPersonerMedUtgjørendeVilkår(
                                vilkårsvurdering = vilkårsvurdering,
                                vedtaksperiode = Periode(
                                        fom = restPostVedtakBegrunnelse.fom,
                                        tom = restPostVedtakBegrunnelse.tom ?: TIDENES_ENDE
                                ),
                                oppdatertBegrunnelseType = vedtakBegrunnelseType,
                                utgjørendeVilkår = vedtakBegrunnelse.finnVilkårFor(),
                                aktuellePersonerForVedtaksperiode = personopplysningGrunnlag.personer.toList()
                        )
                }

        val barnaMedVilkårSomPåvirkerUtbetaling =
                personerMedUtgjørendeVilkårForUtbetalingsperiode.filter { person ->
                    person.type == PersonType.BARN
                }

        return if (VedtakBegrunnelseUtils.vedtakBegrunnelserIkkeTilknyttetVilkår.contains(vedtakBegrunnelse)) {
            if (vedtakBegrunnelse == VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                && SatsService.finnSatsendring(restPostVedtakBegrunnelse.fom).isEmpty()) {
                throw FunksjonellFeil(melding = "Begrunnelsen stemmer ikke med satsendring.",
                                      frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse.")
            }

            if (vedtakBegrunnelse == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR
                && barnaMedVilkårSomPåvirkerUtbetaling.isEmpty()) {
                throw FunksjonellFeil(melding = "Begrunnelsen stemmer ikke med fødselsdag.",
                                      frontendFeilmelding = "Begrunnelsen stemmer ikke med fødselsdag. Vennligst velg en annen periode eller begrunnelse.")
            }
            vedtakBegrunnelse.hentBeskrivelse(målform = personopplysningGrunnlag.søker.målform,
                                              barnasFødselsdatoer = barnaMedVilkårSomPåvirkerUtbetaling.map { it.fødselsdato })
        } else {
            if (personerMedUtgjørendeVilkårForUtbetalingsperiode.isEmpty()) {
                throw FunksjonellFeil(melding = "Begrunnelsen samsvarte ikke med vilkårsvurderingen",
                                      frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.")
            }

            val gjelderSøker = personerMedUtgjørendeVilkårForUtbetalingsperiode.any {
                it.type == PersonType.SØKER
            }

            vedtakBegrunnelse.hentBeskrivelse(gjelderSøker = gjelderSøker,
                                              barnasFødselsdatoer = barnaMedVilkårSomPåvirkerUtbetaling.map { it.fødselsdato },
                                              månedOgÅrBegrunnelsenGjelderFor = vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                      periode = Periode(fom = restPostVedtakBegrunnelse.fom,
                                                                        tom = restPostVedtakBegrunnelse.tom ?: TIDENES_ENDE)
                                              ),
                                              målform = personopplysningGrunnlag.søker.målform)
        }
    }

    @Transactional
    fun settFritekstbegrunnelserPåVedtaksperiodeOgType(restPostFritekstVedtakBegrunnelser: RestPostFritekstVedtakBegrunnelser,
                                                       fagsakId: Long,
                                                       validerKombinasjoner: Boolean = true): Vedtak {
        val vedtak = hentVedtakForAktivBehandling(fagsakId)

        vedtak.slettFritekstbegrunnelserForPeriode(restPostFritekstVedtakBegrunnelser)

        restPostFritekstVedtakBegrunnelser.fritekster.map {
            vedtak.leggTilBegrunnelse(VedtakBegrunnelse(
                    vedtak = vedtak,
                    fom = restPostFritekstVedtakBegrunnelser.fom,
                    tom = restPostFritekstVedtakBegrunnelser.tom,
                    begrunnelse = restPostFritekstVedtakBegrunnelser.vedtaksperiodetype.toVedtakFritekstBegrunnelseSpesifikasjon(),
                    brevBegrunnelse = it
            ))
            oppdater(vedtak)
        }
        if (validerKombinasjoner) vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon()

        return vedtak
    }

    @Transactional
    fun slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(restDeleteVedtakBegrunnelser: RestDeleteVedtakBegrunnelser,
                                                             fagsakId: Long) {

        val vedtak = hentVedtakForAktivBehandling(fagsakId)

        vedtak.slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(restDeleteVedtakBegrunnelser)

        oppdater(vedtak)

        vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon()
    }

    @Transactional
    fun slettBegrunnelse(begrunnelseId: Long,
                         fagsakId: Long): List<VedtakBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId)

        vedtak.slettBegrunnelse(begrunnelseId)

        oppdater(vedtak)
        vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon()

        return vedtak.vedtakBegrunnelser.toList()
    }

    @Transactional
    fun slettAlleUtbetalingOpphørOgAvslagFritekstBegrunnelser(behandlingId: Long) =
            hentAktivForBehandling(behandlingId)?.let {
                it.slettAlleUtbetalingOpphørOgAvslagFritekstBegrunnelser()
                oppdater(it)
            }

    @Transactional
    fun slettAvslagBegrunnelserForVilkår(vilkårResultatId: Long,
                                         behandlingId: Long) {
        val vedtak = hentAktivForBehandling(behandlingId)
                     ?: throw Feil(message = "Finner ikke aktivt vedtak på behandling ved oppdatering av avslagbegrunnelser")
        vedtak.slettAlleAvslagBegrunnelserForVilkår(vilkårResultatId = vilkårResultatId)
        oppdater(vedtak)
    }

    @Transactional
    fun oppdaterAvslagBegrunnelserForVilkår(vilkårResultat: VilkårResultat,
                                            begrunnelser: List<VedtakBegrunnelseSpesifikasjon>,
                                            behandlingId: Long): List<VedtakBegrunnelseSpesifikasjon> {

        if (begrunnelser.any { it.finnVilkårFor() != vilkårResultat.vilkårType }) error("Avslagbegrunnelser som oppdateres må tilhøre samme vilkår")

        val vedtak = hentAktivForBehandling(behandlingId)
                     ?: throw Feil(message = "Finner ikke aktivt vedtak på behandling ved oppdatering av avslagbegrunnelser")

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(vedtak.behandling.id)
                                       ?: throw Feil("Finner ikke personopplysninggrunnlag ved oppdatering av avslagbegrunnelser")


        val vilkårPerson = vilkårResultat.personResultat?.personIdent
                           ?: error("VilkårResultat ${vilkårResultat.id} ikke knyttet til PersonResultat")

        val personDetGjelder = personopplysningGrunnlag.takeIf { it.søker.personIdent.ident == vilkårPerson }?.søker
                               ?: personopplysningGrunnlag.barna.singleOrNull { it.personIdent.ident == vilkårPerson }
                               ?: error("Finner ikke person på VilkårResultat ${vilkårResultat.id} i personopplysningGrunnlag ${personopplysningGrunnlag.id}")

        val lagredeBegrunnelser = vedtakBegrunnelseRepository.findByVedtakId(vedtakId = vedtak.id)
                .filter { it.vilkårResultat?.id == vilkårResultat.id }
                .toSet()
        val oppdaterteBegrunnelser = begrunnelser.map {
            VedtakBegrunnelse(vedtak = vedtak,
                              fom = vilkårResultat.vedtaksperiodeFom,
                              tom = vilkårResultat.vedtaksperiodeTom,
                              begrunnelse = it)
        }.toSet()

        val fjernede = lagredeBegrunnelser.subtract(oppdaterteBegrunnelser)
        val lagtTil = oppdaterteBegrunnelser.subtract(lagredeBegrunnelser)

        fjernede.forEach {
            vedtak.slettAvslagBegrunnelse(vilkårResultatId = vilkårResultat.id,
                                          begrunnelse = it.begrunnelse)
        }
        lagtTil.forEach {
            vedtak.leggTilBegrunnelse(VedtakBegrunnelse(vedtak = vedtak,
                                                        fom = vilkårResultat.vedtaksperiodeFom,
                                                        tom = vilkårResultat.vedtaksperiodeTom,
                                                        vilkårResultat = vilkårResultat,
                                                        begrunnelse = it.begrunnelse,
                                                        brevBegrunnelse = it.begrunnelse.hentBeskrivelse(
                                                                gjelderSøker = personDetGjelder.type == PersonType.SØKER,
                                                                barnasFødselsdatoer = if (personDetGjelder.type == PersonType.BARN) listOf(
                                                                        personDetGjelder.fødselsdato) else emptyList(),
                                                                månedOgÅrBegrunnelsenGjelderFor =
                                                                VedtakBegrunnelseType.AVSLAG.hentMånedOgÅrForBegrunnelse(Periode(
                                                                        vilkårResultat.periodeFom
                                                                        ?: TIDENES_MORGEN,
                                                                        vilkårResultat.periodeTom
                                                                        ?: TIDENES_ENDE)),
                                                                målform = personopplysningGrunnlag.søker.målform)))
        }

        oppdater(vedtak)
        return begrunnelser
    }

    private fun harPersonerManglerOpplysninger(vilkårsvurdering: Vilkårsvurdering): Boolean =
            vilkårsvurdering.personResultater.any { personResultat ->
                personResultat.andreVurderinger.any {
                    it.type == AnnenVurderingType.OPPLYSNINGSPLIKT && it.resultat == Resultat.IKKE_OPPFYLT
                }
            }

    fun hent(vedtakId: Long): Vedtak {
        return vedtakRepository.getOne(vedtakId)
    }

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentAktivForBehandlingThrows(behandlingId: Long): Vedtak {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
               ?: throw Feil("Finner ikke aktivt vedtak på behandling $behandlingId")
    }

    private fun hentVedtakForAktivBehandling(fagsakId: Long): Vedtak {
        val behandling: Behandling = behandlingService.hentAktivForFagsak(fagsakId)
                                     ?: throw Feil(message = "Finner ikke aktiv behandling på fagsak")

        return hentAktivForBehandling(behandlingId = behandling.id)
               ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

    }

    fun oppdater(vedtak: Vedtak): Vedtak {

        return if (vedtakRepository.findByIdOrNull(vedtak.id) != null) {
            vedtakRepository.saveAndFlush(vedtak)
        } else {
            error("Forsøker å oppdatere et vedtak som ikke er lagret")
        }
    }

    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak {
        val brev = dokumentService.genererBrevForVedtak(vedtak)
        return vedtakRepository.save(vedtak.also { it.stønadBrevPdF = brev })
    }

    /**
     * Oppdater vedtaksdato og brev.
     * Vi oppdaterer brevet for å garantere å få riktig beslutter og vedtaksdato.
     */
    fun oppdaterVedtaksdato(vedtak: Vedtak) {
        vedtak.vedtaksdato = LocalDateTime.now()
        vedtakRepository.save(vedtak)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
    }

    /**
     * Når et vilkår vurderes (endres) vil begrunnelsene satt på dette vilkåret resettes
     */
    @Transactional
    fun settStegSlettVedtakBegrunnelserOgTilbakekreving(behandlingId: Long) {
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId = behandlingId,
                                                                              steg = StegType.VILKÅRSVURDERING)
        tilbakekrevingService.slettTilbakekrevingPåBehandling(behandlingId)
        slettAlleUtbetalingOpphørOgAvslagFritekstBegrunnelser(behandlingId)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(VedtakService::class.java)

        data class BrevtekstParametre(
                val gjelderSøker: Boolean = false,
                val barnasFødselsdatoer: List<LocalDate> = emptyList(),
                val månedOgÅrBegrunnelsenGjelderFor: String = "",
                val målform: Målform)

        val BrevParameterComparator =
                compareBy<Map.Entry<VedtakBegrunnelseSpesifikasjon, BrevtekstParametre>>({ !it.value.gjelderSøker },
                                                                                         { it.value.barnasFødselsdatoer.isNotEmpty() })

        fun mapTilRestAvslagBegrunnelser(avslagBegrunnelser: List<VedtakBegrunnelse>,
                                         personopplysningGrunnlag: PersonopplysningGrunnlag): List<RestAvslagBegrunnelser> =
                mapAvslagBegrunnelser(avslagBegrunnelser = avslagBegrunnelser.filter { it.begrunnelse != VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST },
                                      personopplysningGrunnlag = personopplysningGrunnlag)
                        .map { (periode, begrunnelser) ->
                            RestAvslagBegrunnelser(
                                    fom = periode.fom,
                                    tom = periode.tom,
                                    brevBegrunnelser = begrunnelser)
                        }

        /**
         * Slår sammen eventuelle brevtekster som har identisk periode og begrunnelse
         */
        fun mapAvslagBegrunnelser(avslagBegrunnelser: List<VedtakBegrunnelse>,
                                  personopplysningGrunnlag: PersonopplysningGrunnlag): Map<NullablePeriode, List<String>> {
            if (avslagBegrunnelser.any { it.begrunnelse.vedtakBegrunnelseType != VedtakBegrunnelseType.AVSLAG }) throw Feil("Forsøker å slå sammen begrunnelser som ikke er av typen AVSLAG")
            return avslagBegrunnelser
                    .grupperPåPeriode()
                    .mapValues { (_, begrunnelser) ->
                        val (begrunnelserSomSkalGenereres, fritekster) = begrunnelser.partition { !it.begrunnelse.erFritekstBegrunnelse() }
                        val genererteBrevtekster = begrunnelserSomSkalGenereres
                                .groupBy { it.begrunnelse }
                                .mapValues { (fellesBegrunnelse, tilfellerForSammenslåing) ->
                                    if (fellesBegrunnelse == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN) {
                                        BrevtekstParametre(
                                                gjelderSøker = true,
                                                målform = personopplysningGrunnlag.søker.målform
                                        )
                                    } else {
                                        val begrunnedePersoner = tilfellerForSammenslåing
                                                .map {
                                                    it.vilkårResultat?.personResultat
                                                    ?: error("Begrunnelse mangler VilkårResultat")
                                                }.map { it.personIdent }
                                        BrevtekstParametre(
                                                gjelderSøker = begrunnedePersoner.contains(personopplysningGrunnlag.søker.personIdent.ident),
                                                barnasFødselsdatoer = personopplysningGrunnlag.barna
                                                        .filter { begrunnedePersoner.contains(it.personIdent.ident) }
                                                        .map { it.fødselsdato },
                                                månedOgÅrBegrunnelsenGjelderFor = VedtakBegrunnelseType.AVSLAG.hentMånedOgÅrForBegrunnelse(
                                                        Periode(tilfellerForSammenslåing.first().vilkårResultat?.periodeFom
                                                                ?: TIDENES_MORGEN,
                                                                tilfellerForSammenslåing.first().vilkårResultat?.periodeTom
                                                                ?: TIDENES_ENDE)),
                                                målform = personopplysningGrunnlag.søker.målform)
                                    }
                                }
                                .entries.sortedWith(BrevParameterComparator)
                                .map { (begrunnelse, parametere) ->
                                    begrunnelse.hentBeskrivelse(parametere.gjelderSøker,
                                                                parametere.barnasFødselsdatoer,
                                                                parametere.månedOgÅrBegrunnelsenGjelderFor,
                                                                parametere.målform)
                                }
                        genererteBrevtekster +
                        fritekster.sortedBy { it.opprettetTidspunkt }
                                .map { it.brevBegrunnelse ?: error("Fritekst mangler brevbegrunnelse") }
                    }
        }
    }
}