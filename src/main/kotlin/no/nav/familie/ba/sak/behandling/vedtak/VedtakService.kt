package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.restDomene.RestPostVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.tilVedtakBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseType
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelseUtils
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårResultat
import no.nav.familie.ba.sak.behandling.vilkår.Vilkårsvurdering
import no.nav.familie.ba.sak.behandling.vilkår.VilkårsvurderingService
import no.nav.familie.ba.sak.behandling.vilkår.hentMånedOgÅrForBegrunnelse
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.førsteDagINesteMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.tilKortString
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import org.slf4j.LoggerFactory
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
                    private val beregningService: BeregningService) {

    fun opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        return lagreEllerOppdater(vedtak = vedtak, oppdaterStønadsbrev = true)
    }

    @Transactional
    fun lagreEllerOppdaterVedtakForAktivBehandling(behandling: Behandling,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag): Vedtak {
        // TODO: Midlertidig fiks før støtte for delvis innvilget
        val behandlingResultat = midlertidigUtledBehandlingResultatType(
                hentetBehandlingResultat = behandlingService.hent(behandling.id).resultat)

        val vedtak = Vedtak(
                behandling = behandling,
                opphørsdato = if (behandlingResultat == BehandlingResultat.OPPHØRT) now()
                        .førsteDagINesteMåned() else null,
                vedtaksdato = if (behandling.skalBehandlesAutomatisk) LocalDateTime.now() else null
        )

        return lagreOgDeaktiverGammel(vedtak)
    }


    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak {
        vedtak.stønadBrevPdF = dokumentService.genererBrevForVedtak(vedtak)
        return vedtak
    }

    @Transactional
    fun leggTilBegrunnelse(restPostVedtakBegrunnelse: RestPostVedtakBegrunnelse,
                           fagsakId: Long): List<VedtakBegrunnelse> {

        val vedtakBegrunnelseType = restPostVedtakBegrunnelse.vedtakBegrunnelse.vedtakBegrunnelseType
        val vedtakBegrunnelse = restPostVedtakBegrunnelse.vedtakBegrunnelse

        val vedtak = hentVedtakForAktivBehandling(fagsakId)
                     ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(vedtak.behandling.id)
                                       ?: throw Feil("Finner ikke personopplysninggrunnlag ved fastsetting av begrunnelse")


        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(vedtak.behandling.id)
                               ?: throw Feil("Finner ikke vilkårsvurdering ved fastsetting av begrunnelse")

        val personerMedUtgjørendeVilkårForUtbetalingsperiode =
                hentPersonerMedUtgjørendeVilkår(
                        vilkårsvurdering = vilkårsvurdering,
                        utbetalingsperiode = Periode(
                                fom = restPostVedtakBegrunnelse.fom,
                                tom = restPostVedtakBegrunnelse.tom
                        ),
                        oppdatertBegrunnelseType = vedtakBegrunnelseType,
                        utgjørendeVilkår = vedtakBegrunnelse.finnVilkårFor())

        val barnaMedVilkårSomPåvirkerUtbetaling =
                personerMedUtgjørendeVilkårForUtbetalingsperiode.filter { (person, vilkårResultat) ->
                    person.type == PersonType.BARN
                }.map { (person, vilkårResultat) ->
                    person
                }

        val barnasFødselsdatoer = slåSammen(barnaMedVilkårSomPåvirkerUtbetaling.sortedBy { it.fødselsdato }
                                                    .map { it.fødselsdato.tilKortString() })

        val brevBegrunnelse = if (VedtakBegrunnelseUtils.utenVilkår.contains(vedtakBegrunnelse)) {
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
                it.first.type == PersonType.SØKER
            }

            vedtakBegrunnelse.hentBeskrivelse(gjelderSøker = gjelderSøker,
                                              barnasFødselsdatoer = barnasFødselsdatoer,
                                              månedOgÅrBegrunnelsenGjelderFor = vedtakBegrunnelseType.hentMånedOgÅrForBegrunnelse(
                                                      Periode(fom = restPostVedtakBegrunnelse.fom,
                                                              tom = restPostVedtakBegrunnelse.tom)),
                                              målform = personopplysningGrunnlag.søker.målform)
        }

        vedtak.leggTilBegrunnelse(restPostVedtakBegrunnelse.tilVedtakBegrunnelse(vedtak, brevBegrunnelse))

        lagreEllerOppdater(vedtak)

        return vedtak.vedtakBegrunnelser.toList()
    }

    @Transactional
    fun slettBegrunnelserForPeriode(periode: Periode,
                                    fagsakId: Long) {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettBegrunnelserForPeriode(periode)

        lagreEllerOppdater(vedtak)
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

        return lagreEllerOppdater(aktivtVedtak)
    }

    @Transactional
    fun slettBegrunnelse(begrunnelseId: Long,
                         fagsakId: Long): List<VedtakBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettBegrunnelse(begrunnelseId)

        lagreEllerOppdater(vedtak)

        return vedtak.vedtakBegrunnelser.toList()
    }

    @Transactional
    fun slettAlleVedtakBegrunnelser(behandlingId: Long) {
        val vedtak = hentAktivForBehandling(behandlingId)

        if (vedtak != null) {
            vedtak.slettAlleBegrunnelser()
            lagreEllerOppdater(vedtak)
        }
    }

    /**
     * Må vite om det gjelder søker og/eller barn da dette bestemmer ordlyd i begrunnelsen.
     * Funksjonen henter personer som trigger den gitte utbetalingsperioden ved å hente vilkårResultater
     * basert på utgjørendeVilkår og begrunnelseType.
     *
     * @param vilkårsvurdering - Behandlingresultatet man skal begrunne
     * @param utbetalingsperiode - Perioden for utbetaling
     * @param oppdatertBegrunnelseType - Brukes til å se om man skal sammenligne fom eller tom-dato
     * @param utgjørendeVilkår -  Brukes til å sammenligne vilkår i vilkårsvurdering
     * @return List med par bestående av person og vilkåret de trigger endring på
     */
    private fun hentPersonerMedUtgjørendeVilkår(vilkårsvurdering: Vilkårsvurdering,
                                                utbetalingsperiode: Periode,
                                                oppdatertBegrunnelseType: VedtakBegrunnelseType,
                                                utgjørendeVilkår: Vilkår?): List<Pair<Person, VilkårResultat>> {
        return vilkårsvurdering.personResultater.fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkårResultat = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->
                when {
                    vilkårResultat.vilkårType != utgjørendeVilkår -> false
                    vilkårResultat.periodeFom == null -> {
                        false
                    }
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE -> {
                        vilkårResultat.periodeFom!!.toYearMonth() == utbetalingsperiode.fom.minusMonths(1)
                                .toYearMonth() && vilkårResultat.resultat == Resultat.OPPFYLT
                    }

                    oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON -> {
                        val oppfyltTomMånedEtter = if (vilkårResultat.vilkårType == Vilkår.UNDER_18_ÅR) 0L else 1L
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.toYearMonth() == utbetalingsperiode.fom.minusMonths(
                                oppfyltTomMånedEtter).toYearMonth() && vilkårResultat.resultat == Resultat.OPPFYLT
                    }

                    oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.toYearMonth() == utbetalingsperiode.tom.toYearMonth()
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
                acc.add(Pair(person, utgjørendeVilkårResultat))
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

    fun lagreOgDeaktiverGammel(vedtak: Vedtak): Vedtak {
        val aktivVedtak = hentAktivForBehandling(vedtak.behandling.id)

        if (aktivVedtak != null && aktivVedtak.id != vedtak.id) {
            vedtakRepository.saveAndFlush(aktivVedtak.also { it.aktiv = false })
        }

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vedtak $vedtak")
        return vedtakRepository.save(vedtak)
    }

    fun lagreEllerOppdater(vedtak: Vedtak, oppdaterStønadsbrev: Boolean = false): Vedtak {
        val ikkeTekniskOpphør = !vedtak.behandling.erTekniskOpphør()
        val vedtakForLagring = if (oppdaterStønadsbrev && ikkeTekniskOpphør) oppdaterVedtakMedStønadsbrev(vedtak) else vedtak
        return vedtakRepository.save(vedtakForLagring)
    }

    /**
     * Oppdater vedtaksdato og brev.
     * Vi oppdaterer brevet for å garantere å få riktig beslutter og vedtaksdato.
     */
    fun oppdaterVedtaksdatoOgBrev(vedtak: Vedtak) {
        vedtak.vedtaksdato = LocalDateTime.now()
        lagreEllerOppdater(vedtak = vedtak, oppdaterStønadsbrev = true)

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}


