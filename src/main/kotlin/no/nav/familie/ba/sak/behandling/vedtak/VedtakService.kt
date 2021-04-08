package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.restDomene.RestAvslagBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostFritekstVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.tilVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.config.FeatureToggleConfig
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDate.now
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class VedtakService(
        private val behandlingService: BehandlingService,
        private val vilkårsvurderingService: VilkårsvurderingService,
        private val persongrunnlagService: PersongrunnlagService,
        private val loggService: LoggService,
        private val vedtakRepository: VedtakRepository,
        private val dokumentService: DokumentService,
        private val totrinnskontrollService: TotrinnskontrollService,
        private val beregningService: BeregningService,
        private val featureToggleService: FeatureToggleService,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
) {

    fun opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        return oppdaterVedtakMedStønadsbrev(vedtak = vedtak)
    }

    fun oppdaterOpphørsdatoPåVedtak(behandlingId: Long) {
        // TODO: Midlertidig fiks før støtte for delvis innvilget
        val behandlingResultat =
                midlertidigUtledBehandlingResultatType(hentetBehandlingResultat = behandlingService.hent(behandlingId).resultat)

        val aktivtVedtak = hentAktivForBehandling(behandlingId = behandlingId)
        if (aktivtVedtak != null) {
            vedtakRepository.saveAndFlush(aktivtVedtak.also {
                it.opphørsdato = if (behandlingResultat == BehandlingResultat.OPPHØRT) now()
                        .førsteDagINesteMåned() else null
            })
        }
    }

    @Transactional
    fun leggTilVedtakBegrunnelse(restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse, fagsakId: Long): List<VedtakBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId)
                     ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(vedtak.behandling.id)
                                       ?: throw Feil("Finner ikke personopplysninggrunnlag ved fastsetting av begrunnelse")

        val brevBegrunnelse =
                if (restPostVedtakBegrunnelse.vedtakBegrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG)
                    restPostVedtakBegrunnelse.vedtakBegrunnelse.hentBeskrivelse(målform = personopplysningGrunnlag.søker.målform)
                else
                    lagBrevBegrunnelseForUtbetalingEllerOpphør(restPostVedtakBegrunnelse, vedtak, personopplysningGrunnlag)

        vedtak.leggTilBegrunnelse(restPostVedtakBegrunnelse.tilVedtakBegrunnelse(vedtak, brevBegrunnelse))

        oppdater(vedtak)

        return vedtak.vedtakBegrunnelser.toList()
    }

    fun lagBrevBegrunnelseForUtbetalingEllerOpphør(restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse,
                                                   vedtak: Vedtak,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag): String {

        val visOpphørsperioderToggle = featureToggleService.isEnabled(FeatureToggleConfig.VIS_OPPHØRSPERIODER_TOGGLE)
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
                                visOpphørsperioderToggle = visOpphørsperioderToggle)
                }

        val barnaMedVilkårSomPåvirkerUtbetaling =
                personerMedUtgjørendeVilkårForUtbetalingsperiode.filter { person ->
                    person.type == PersonType.BARN
                }

        val barnasFødselsdatoer = barnaMedVilkårSomPåvirkerUtbetaling.tilBrevTekst()

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
                                              barnasFødselsdatoer = barnasFødselsdatoer)
        } else {
            if (personerMedUtgjørendeVilkårForUtbetalingsperiode.isEmpty()) {
                throw FunksjonellFeil(melding = "Begrunnelsen samsvarte ikke med vilkårsvurderingen",
                                      frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.")
            }

            val gjelderSøker = personerMedUtgjørendeVilkårForUtbetalingsperiode.any {
                it.type == PersonType.SØKER
            }

            vedtakBegrunnelse.hentBeskrivelse(gjelderSøker = gjelderSøker,
                                              barnasFødselsdatoer = barnasFødselsdatoer,
                                              månedOgÅrBegrunnelsenGjelderFor = vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                      periode = Periode(fom = restPostVedtakBegrunnelse.fom,
                                                                        tom = restPostVedtakBegrunnelse.tom ?: TIDENES_ENDE),
                                                      visOpphørsperioderToggle = visOpphørsperioderToggle
                                              ),
                                              målform = personopplysningGrunnlag.søker.målform)
        }
    }

    @Transactional
    fun settFritekstbegrunnelserPåVedtaksperiodeOgType(restPostFritekstVedtakBegrunnelser: RestPostFritekstVedtakBegrunnelser,
                                                       fagsakId: Long): List<VedtakBegrunnelse> {
        val vedtak = hentVedtakForAktivBehandling(fagsakId)
                     ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.settFritekstbegrunnelser(restPostFritekstVedtakBegrunnelser)

        oppdater(vedtak)

        return vedtak.vedtakBegrunnelser.toList()
    }

    @Transactional
    fun slettBegrunnelserForPeriode(periode: Periode,
                                    fagsakId: Long) {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettUtbetalingOgOpphørBegrunnelserBegrunnelserForPeriode(periode)

        oppdater(vedtak)
    }

    @Transactional
    fun slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(restDeleteVedtakBegrunnelser: RestDeleteVedtakBegrunnelser,
                                                             fagsakId: Long) {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettBegrunnelserForPeriodeOgVedtaksbegrunnelseTyper(restDeleteVedtakBegrunnelser)

        oppdater(vedtak)
    }

    @Transactional
    fun leggTilBegrunnelsePåInneværendeUtbetalingsperiode(behandlingId: Long,
                                                          begrunnelseType: VedtakBegrunnelseType,
                                                          vedtakBegrunnelse: VedtakBegrunnelseSpesifikasjon,
                                                          målform: Målform,
                                                          barnasFødselsdatoer: List<Person>): Vedtak {

        val aktivtVedtak = hentAktivForBehandling(behandlingId = behandlingId)
                           ?: error("Fant ikke aktivt vedtak på behandling $behandlingId")

        val tomDatoForInneværendeUtbetalingsintervall =
                finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId)


        aktivtVedtak.leggTilBegrunnelse(VedtakBegrunnelse(vedtak = aktivtVedtak,
                                                          fom = YearMonth.now().førsteDagIInneværendeMåned(),
                                                          tom = tomDatoForInneværendeUtbetalingsintervall,
                                                          begrunnelse = vedtakBegrunnelse,
                                                          brevBegrunnelse = vedtakBegrunnelse.hentBeskrivelse(
                                                                  barnasFødselsdatoer = barnasFødselsdatoer.tilBrevTekst(),
                                                                  målform = målform)))

        return oppdater(aktivtVedtak)
    }

    @Transactional
    fun slettBegrunnelse(begrunnelseId: Long,
                         fagsakId: Long): List<VedtakBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettBegrunnelse(begrunnelseId)

        oppdater(vedtak)

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
                              fom = vilkårResultat.periodeFom,
                              tom = vilkårResultat.periodeTom,
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
                                                        fom = vilkårResultat.periodeFom,
                                                        tom = vilkårResultat.periodeTom,
                                                        vilkårResultat = vilkårResultat,
                                                        begrunnelse = it.begrunnelse,
                                                        brevBegrunnelse = it.begrunnelse.hentBeskrivelse(
                                                                gjelderSøker = personDetGjelder.type == PersonType.SØKER,
                                                                barnasFødselsdatoer = if (personDetGjelder.type == PersonType.BARN) personDetGjelder.fødselsdato.tilKortString() else "",
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

    /**
     * Må vite om det gjelder søker og/eller barn da dette bestemmer ordlyd i begrunnelsen.
     * Funksjonen henter personer som trigger den gitte utbetalingsperioden ved å hente vilkårResultater
     * basert på utgjørendeVilkår og begrunnelseType.
     *
     * @param vilkårsvurdering - Behandlingresultatet man skal begrunne
     * @param vedtaksperiode - Perioden for utbetaling
     * @param oppdatertBegrunnelseType - Brukes til å se om man skal sammenligne fom eller tom-dato
     * @param utgjørendeVilkår -  Brukes til å sammenligne vilkår i vilkårsvurdering
     * @return List med par bestående av person de trigger endring på
     */
    private fun hentPersonerMedUtgjørendeVilkår(vilkårsvurdering: Vilkårsvurdering,
                                                vedtaksperiode: Periode,
                                                oppdatertBegrunnelseType: VedtakBegrunnelseType,
                                                utgjørendeVilkår: Vilkår?,
                                                visOpphørsperioderToggle: Boolean): List<Person> {

        return vilkårsvurdering.personResultater.fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->

                val oppfyltTomMånedEtter = if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR) 0L else 1L
                when {
                    vilkårResultat.vilkårType != utgjørendeVilkår -> false
                    vilkårResultat.periodeFom == null -> {
                        false
                    }
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE -> {
                        vilkårResultat.periodeFom!!.toYearMonth() == vedtaksperiode.fom.minusMonths(1)
                                .toYearMonth() && vilkårResultat.resultat == Resultat.OPPFYLT
                    }

                    oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON ||
                    (oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR && visOpphørsperioderToggle) -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.plusDays(1)
                                .toYearMonth() == vedtaksperiode.fom.minusMonths(
                                oppfyltTomMånedEtter).toYearMonth() && vilkårResultat.resultat == Resultat.OPPFYLT
                    }

                    oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.toYearMonth() == vedtaksperiode.tom.toYearMonth()
                        && vilkårResultat.resultat == Resultat.OPPFYLT
                    }
                    else -> throw Feil("Henting av personer med utgjørende vilkår when: Ikke implementert")
                }
            }

            val person =
                    persongrunnlagService.hentAktiv(vilkårsvurdering.behandling.id)?.personer?.firstOrNull { person ->
                        person.personIdent.ident == personResultat.personIdent
                    }
                    ?: throw Feil(message = "Kunne ikke finne person på personResultat")

            if (utgjørendeVilkårResultat != null) {
                acc.add(person)
            }
            acc
        }

    }

    private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId: Long): LocalDate =
            beregningService.hentAndelerTilkjentYtelserInneværendeMåned(behandlingId)
                    .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()
            ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")

    fun hent(vedtakId: Long): Vedtak {
        return vedtakRepository.getOne(vedtakId)
    }

    fun hentAktivForBehandling(behandlingId: Long): Vedtak? {
        return vedtakRepository.findByBehandlingAndAktiv(behandlingId)
    }

    private fun hentVedtakForAktivBehandling(fagsakId: Long): Vedtak? {
        val behandling: Behandling = behandlingService.hentAktivForFagsak(fagsakId)
                                     ?: throw Feil(message = "Finner ikke aktiv behandling på fagsak")

        return hentAktivForBehandling(behandlingId = behandling.id)
               ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

    }

    fun oppdater(vedtak: Vedtak): Vedtak {
        vedtak.validerVedtakBegrunnelserForFritekstOpphørOgReduksjon()

        return if (vedtakRepository.findByIdOrNull(vedtak.id) != null) {
            vedtakRepository.saveAndFlush(vedtak)
        } else {
            error("Forsøker å oppdatere et vedtak som ikke er lagret")
        }
    }

    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak {
        val ikkeTekniskOpphør = !vedtak.behandling.erTekniskOpphør()
        return if (ikkeTekniskOpphør) {
            val brev = dokumentService.genererBrevForVedtak(vedtak)
            vedtakRepository.save(vedtak.also { it.stønadBrevPdF = brev })
        } else {
            vedtak
        }
    }

    /**
     * Oppdater vedtaksdato og brev.
     * Vi oppdaterer brevet for å garantere å få riktig beslutter og vedtaksdato.
     */
    fun oppdaterVedtaksdatoOgBrev(vedtak: Vedtak) {
        vedtak.vedtaksdato = LocalDateTime.now()
        oppdaterVedtakMedStønadsbrev(vedtak)

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
    }

    /**
     * Når et vilkår vurderes (endres) vil begrunnelsene satt på dette vilkåret resettes
     */
    fun settStegOgSlettVedtakBegrunnelser(behandlingId: Long) {
        behandlingService.leggTilStegPåBehandlingOgSettTidligereStegSomUtført(behandlingId = behandlingId,
                                                                              steg = StegType.VILKÅRSVURDERING)
        slettAlleUtbetalingOpphørOgAvslagFritekstBegrunnelser(behandlingId)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)

        data class BrevtekstParametre(
                val gjelderSøker: Boolean = false,
                val barnasFødselsdatoer: String = "",
                val månedOgÅrBegrunnelsenGjelderFor: String = "",
                val målform: Målform)

        val BrevParameterComparator =
                compareBy<Map.Entry<VedtakBegrunnelseSpesifikasjon, BrevtekstParametre>>({ !it.value.gjelderSøker },
                                                                                         { it.value.barnasFødselsdatoer != "" })

        /**
         * Slår sammen eventuelle brevtekster som har identisk periode og begrunnelse
         */
        fun mapTilRestAvslagBegrunnelser(avslagBegrunnelser: List<VedtakBegrunnelse>,
                                         personopplysningGrunnlag: PersonopplysningGrunnlag): List<RestAvslagBegrunnelser> {
            if (avslagBegrunnelser.any { it.begrunnelse.vedtakBegrunnelseType != VedtakBegrunnelseType.AVSLAG }) throw Feil("Forsøker å slå sammen begrunnelser som ikke er av typen AVSLAG")
            return avslagBegrunnelser
                    .filter { it.begrunnelse != VedtakBegrunnelseSpesifikasjon.AVSLAG_FRITEKST }
                    .grupperPåPeriode()
                    .map { (periode, begrunnelser) ->
                        RestAvslagBegrunnelser(
                                fom = periode.fom,
                                tom = periode.tom,
                                brevBegrunnelser = begrunnelser
                                        .groupBy { it.begrunnelse }
                                        .mapValues { (begrunnelse, tilfellerForSammenslåing) ->
                                            if (begrunnelse == VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN) {
                                                BrevtekstParametre(
                                                        gjelderSøker = true,
                                                        barnasFødselsdatoer = "",
                                                        månedOgÅrBegrunnelsenGjelderFor = "",
                                                        målform = personopplysningGrunnlag.søker.målform
                                                )
                                            } else {
                                                val begrunnedePersoner = tilfellerForSammenslåing
                                                        .map {
                                                            it.vilkårResultat?.personResultat
                                                            ?: error("VilkårResultat mangler person")
                                                        }.map { it.personIdent }
                                                BrevtekstParametre(
                                                        gjelderSøker = begrunnedePersoner.contains(personopplysningGrunnlag.søker.personIdent.ident),
                                                        barnasFødselsdatoer = personopplysningGrunnlag.barna
                                                                .filter { begrunnedePersoner.contains(it.personIdent.ident) }
                                                                .tilBrevTekst(),
                                                        månedOgÅrBegrunnelsenGjelderFor =
                                                        VedtakBegrunnelseType.AVSLAG.hentMånedOgÅrForBegrunnelse(Periode(periode.fom
                                                                                                                         ?: TIDENES_MORGEN,
                                                                                                                         periode.tom
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
                                        },
                        )
                    }
        }
    }
}