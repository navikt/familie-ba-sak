package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Målform
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.tilBrevTekst
import no.nav.familie.ba.sak.behandling.restDomene.RestDeleteVedtakBegrunnelser
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.tilVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseUtils
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.behandling.vilkår.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
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
class VedtakService(private val behandlingService: BehandlingService,
                    private val vilkårsvurderingService: VilkårsvurderingService,
                    private val persongrunnlagService: PersongrunnlagService,
                    private val loggService: LoggService,
                    private val vedtakRepository: VedtakRepository,
                    private val dokumentService: DokumentService,
                    private val totrinnskontrollService: TotrinnskontrollService,
                    private val beregningService: BeregningService,
                    private val featureToggleService: FeatureToggleService) {

    fun opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        return oppdaterVedtakMedStønadsbrev(vedtak = vedtak)
    }


    @Transactional
    fun initierVedtakForAktivBehandling(behandling: Behandling) {
        if (behandling.steg !== StegType.BESLUTTE_VEDTAK && behandling.steg !== StegType.REGISTRERE_PERSONGRUNNLAG) {
            error("Forsøker å initiere vedtak på steg ${behandling.steg}")
        }

        val aktivtVedtak = hentAktivForBehandling(behandlingId = behandling.id)
        if (aktivtVedtak != null) {
            vedtakRepository.saveAndFlush(aktivtVedtak.also { it.aktiv = false })
        }

        val vedtak = Vedtak(
                behandling = behandling,
                vedtaksdato = if (behandling.skalBehandlesAutomatisk) LocalDateTime.now() else null
        )
        vedtakRepository.save(vedtak)
    }

    fun oppdaterOpphørsdatoPåVedtak(behandlingId: Long) {
        // TODO: Midlertidig fiks før støtte for delvis innvilget
        val behandlingResultat =
                midlertidigUtledBehandlingResultatType(hentetBehandlingResultat = behandlingService.hent(behandlingId).resultat)

        val aktivtVedtak = hentAktivForBehandling(behandlingId = behandlingId) ?: error("Ved oppdatering av opphørsdato har behandling ikke aktivt vedtak.")

        vedtakRepository.saveAndFlush(aktivtVedtak.also {
            it.opphørsdato = if (behandlingResultat == BehandlingResultat.OPPHØRT) now()
                    .førsteDagINesteMåned() else null
        })
    }

    @Transactional
    fun leggTilBegrunnelse(restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse,
                           fagsakId: Long): List<VedtakBegrunnelse> {

        val visOpphørsperioderToggle = featureToggleService.isEnabled("familie-ba-sak.behandling.vis-opphoersperioder")
        val vedtakBegrunnelseType = restPostVedtakBegrunnelse.vedtakBegrunnelse.vedtakBegrunnelseType
        val vedtakBegrunnelse = restPostVedtakBegrunnelse.vedtakBegrunnelse

        val vedtak = hentVedtakForAktivBehandling(fagsakId)
                     ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(vedtak.behandling.id)
                                       ?: throw Feil("Finner ikke personopplysninggrunnlag ved fastsetting av begrunnelse")


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

        val brevBegrunnelse = if (VedtakBegrunnelseUtils.vedtakBegrunnelserIkkeTilknyttetVilkår.contains(vedtakBegrunnelse)) {
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

        vedtak.leggTilBegrunnelse(restPostVedtakBegrunnelse.tilVedtakBegrunnelse(vedtak, brevBegrunnelse))

        oppdater(vedtak)

        return vedtak.vedtakBegrunnelser.toList()
    }

    @Transactional
    fun slettBegrunnelserForPeriode(periode: Periode,
                                    fagsakId: Long) {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettBegrunnelserForPeriode(periode)

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
    fun slettAlleVedtakBegrunnelser(behandlingId: Long) {
        val vedtak = hentAktivForBehandling(behandlingId)

        if (vedtak != null) {
            vedtak.slettAlleBegrunnelser()
            oppdater(vedtak)
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
        return if (vedtakRepository.findByIdOrNull(vedtak.id) != null) {
            vedtakRepository.save(vedtak)
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

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}


