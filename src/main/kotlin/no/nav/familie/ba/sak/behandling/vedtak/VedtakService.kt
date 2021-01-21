package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.tilRestUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.behandling.vilkår.VedtakBegrunnelse.Companion.finnVilkårFor
import no.nav.familie.ba.sak.beregning.SatsService
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate.now
import java.time.LocalDateTime

@Service
class VedtakService(private val behandlingService: BehandlingService,
                    private val vilkårsvurderingService: VilkårsvurderingService,
                    private val persongrunnlagService: PersongrunnlagService,
                    private val loggService: LoggService,
                    private val vedtakRepository: VedtakRepository,
                    private val dokumentService: DokumentService,
                    private val totrinnskontrollService: TotrinnskontrollService) {

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
    fun leggTilUtbetalingBegrunnelse(periode: Periode,
                                     fagsakId: Long): List<RestUtbetalingBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        val begrunnelse =
                UtbetalingBegrunnelse(vedtak = vedtak,
                                      fom = periode.fom,
                                      tom = periode.tom)

        vedtak.leggTilUtbetalingBegrunnelse(begrunnelse)

        lagreEllerOppdater(vedtak)

        return vedtak.utbetalingBegrunnelser.map {
            it.tilRestUtbetalingBegrunnelse()
        }
    }

    @Transactional
    fun slettUtbetalingBegrunnelse(utbetalingBegrunnelseId: Long,
                                   fagsakId: Long): List<UtbetalingBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        vedtak.slettUtbetalingBegrunnelse(utbetalingBegrunnelseId)

        lagreEllerOppdater(vedtak)

        return vedtak.utbetalingBegrunnelser.toList()
    }

    @Transactional
    fun slettUtbetalingBegrunnelser(behandlingId: Long) {
        val vedtak = hentAktivForBehandling(behandlingId)

        if (vedtak != null) {
            vedtak.slettUtbetalingBegrunnelser()
            lagreEllerOppdater(vedtak)
        }
    }

    @Transactional
    fun endreUtbetalingBegrunnelse(restPutUtbetalingBegrunnelse: RestPutUtbetalingBegrunnelse,
                                   fagsakId: Long, utbetalingBegrunnelseId: Long): List<UtbetalingBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(vedtak.behandling.id)
                                       ?: throw Feil("Finner ikke personopplysninggrunnlag ved fastsetting av begrunnelse")


        val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(vedtak.behandling.id)
                               ?: throw Feil("Finner ikke vilkårsvurdering ved fastsetting av begrunnelse")

        val opprinneligUtbetalingBegrunnelse = vedtak.hentUtbetalingBegrunnelse(utbetalingBegrunnelseId)
                                               ?: throw Feil(message = "Fant ikke stønadbrevbegrunnelse med innsendt id")

        if (restPutUtbetalingBegrunnelse.vedtakBegrunnelse != null && restPutUtbetalingBegrunnelse.vedtakBegrunnelseType != null) {

            if (VedtakBegrunnelseSerivce.utenVilkår.contains(restPutUtbetalingBegrunnelse.vedtakBegrunnelse)) {
                if (restPutUtbetalingBegrunnelse.vedtakBegrunnelse == VedtakBegrunnelse.INNVILGET_SATSENDRING
                    && SatsService.finnSatsendring(opprinneligUtbetalingBegrunnelse.fom).isEmpty()) {
                    throw FunksjonellFeil(melding = "Begrunnelsen stemmer ikke med satsendring.",
                                          frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse.")
                } else {
                    vedtak.endreUtbetalingBegrunnelse(
                            opprinneligUtbetalingBegrunnelse.id,
                            restPutUtbetalingBegrunnelse,
                            restPutUtbetalingBegrunnelse.vedtakBegrunnelse.hentBeskrivelse(målform = personopplysningGrunnlag.søker.målform)
                    )
                }
            } else {
                val personerMedUtgjørendeVilkårForUtbetalingsperiode =
                        hentPersonerMedUtgjørendeVilkår(
                                vilkårsvurdering = vilkårsvurdering,
                                utbetalingsperiode = Periode(
                                        fom = opprinneligUtbetalingBegrunnelse.fom,
                                        tom = opprinneligUtbetalingBegrunnelse.tom
                                ),
                                oppdatertBegrunnelseType = restPutUtbetalingBegrunnelse.vedtakBegrunnelseType,
                                utgjørendeVilkår = restPutUtbetalingBegrunnelse.vedtakBegrunnelse.finnVilkårFor())

                if (personerMedUtgjørendeVilkårForUtbetalingsperiode.isEmpty()) {
                    throw FunksjonellFeil(melding = "Begrunnelsen samsvarte ikke med vilkårsvurderingen",
                                          frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.")
                }

                val gjelderSøker = personerMedUtgjørendeVilkårForUtbetalingsperiode.any {
                    it.first.type == PersonType.SØKER
                }

                val barnaMedVilkårSomPåvirkerUtbetaling = personerMedUtgjørendeVilkårForUtbetalingsperiode.filter {
                    it.first.type == PersonType.BARN
                }.map {
                    it.first
                }

                val vilkårsdato = when (restPutUtbetalingBegrunnelse.vedtakBegrunnelseType) {
                    VedtakBegrunnelseType.REDUKSJON -> opprinneligUtbetalingBegrunnelse.tom.minusMonths(1).tilMånedÅr()
                    VedtakBegrunnelseType.OPPHØR ->
                        opprinneligUtbetalingBegrunnelse.tom.tilMånedÅr()
                    else -> opprinneligUtbetalingBegrunnelse.fom.minusMonths(1).tilMånedÅr()
                }


                val barnasFødselsdatoer = slåSammen(barnaMedVilkårSomPåvirkerUtbetaling.sortedBy { it.fødselsdato }
                                                            .map { it.fødselsdato.tilKortString() })

                val begrunnelseSomSkalPersisteres =
                        restPutUtbetalingBegrunnelse.vedtakBegrunnelse.hentBeskrivelse(gjelderSøker,
                                                                                       barnasFødselsdatoer,
                                                                                       vilkårsdato,
                                                                                       personopplysningGrunnlag.søker.målform)

                vedtak.endreUtbetalingBegrunnelse(
                        opprinneligUtbetalingBegrunnelse.id,
                        restPutUtbetalingBegrunnelse,
                        begrunnelseSomSkalPersisteres
                )
            }
        } else {
            vedtak.endreUtbetalingBegrunnelse(
                    opprinneligUtbetalingBegrunnelse.id,
                    restPutUtbetalingBegrunnelse,
                    "")
        }

        lagreEllerOppdater(vedtak)

        return vedtak.utbetalingBegrunnelser.toList()
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
                        vilkårResultat.periodeFom!!.monthValue == utbetalingsperiode.fom.minusMonths(1).monthValue && vilkårResultat.resultat == Resultat.OPPFYLT
                    }
                    /*
                    TODO: vilkåret fyller 18 år, gjelder måneden før, dette skal fikses i en seprarat opgave
                    hvor vilkåret settes til en tom-dato siste dagen måeden før 18 års dagen.
                     */
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.REDUKSJON -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.monthValue == utbetalingsperiode.tom.monthValue && vilkårResultat.resultat == Resultat.OPPFYLT
                    }
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.OPPHØR -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.monthValue == utbetalingsperiode.tom.monthValue
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


