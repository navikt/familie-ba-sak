package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingÅrsak
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.*
import no.nav.familie.ba.sak.behandling.restDomene.BeregningEndringType
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.toRestUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.TilkjentYtelseUtils
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDate.now

@Service
class VedtakService(private val arbeidsfordelingService: ArbeidsfordelingService,
                    private val behandlingService: BehandlingService,
                    private val behandlingRepository: BehandlingRepository,
                    private val behandlingResultatService: BehandlingResultatService,
                    private val persongrunnlagService: PersongrunnlagService,
                    private val loggService: LoggService,
                    private val vedtakRepository: VedtakRepository,
                    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
                    private val dokumentService: DokumentService,
                    private val totrinnskontrollService: TotrinnskontrollService) {

    fun opprettVedtakOgTotrinnskontrollForAutomatiskBehandling(behandling: Behandling): Vedtak {
        totrinnskontrollService.opprettAutomatiskTotrinnskontroll(behandling)
        loggService.opprettBeslutningOmVedtakLogg(behandling, Beslutning.GODKJENT)

        val vedtak = hentAktivForBehandling(behandlingId = behandling.id)
                     ?: error("Fant ikke aktivt vedtak på behandling ${behandling.id}")

        return lagreEllerOppdater(oppdaterVedtakMedStønadsbrev(vedtak))
    }

    @Transactional
    fun opphørVedtak(saksbehandler: String,
                     gjeldendeBehandlingsId: Long,
                     nyBehandlingType: BehandlingType,
                     opphørsdato: LocalDate,
                     postProsessor: (Vedtak) -> Unit): Ressurs<Vedtak> {

        val gjeldendeVedtak = vedtakRepository.findByBehandlingAndAktiv(gjeldendeBehandlingsId)
                              ?: return Ressurs.failure("Fant ikke aktivt vedtak tilknyttet behandling $gjeldendeBehandlingsId")

        val gjeldendeAndelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(gjeldendeBehandlingsId))
        if (gjeldendeAndelerTilkjentYtelse.isEmpty()) {
            return Ressurs.failure(
                    "Fant ikke andeler tilkjent ytelse tilknyttet behandling $gjeldendeBehandlingsId")
        }

        val gjeldendeBehandling = gjeldendeVedtak.behandling
        if (!gjeldendeBehandling.aktiv) {
            return Ressurs.failure("Aktivt vedtak er tilknyttet behandling $gjeldendeBehandlingsId som IKKE er aktivt")
        }

        val nyBehandling = Behandling(fagsak = gjeldendeBehandling.fagsak,
                                      type = nyBehandlingType,
                                      kategori = gjeldendeBehandling.kategori,
                                      underkategori = gjeldendeBehandling.underkategori,
                                      opprettetÅrsak = BehandlingÅrsak.TEKNISK_OPPHØR)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv = false })
        behandlingRepository.save(nyBehandling)
        loggService.opprettBehandlingLogg(nyBehandling)

        arbeidsfordelingService.settBehandlendeEnhet(nyBehandling,
                                                     arbeidsfordelingService.hentArbeidsfordelingsenhet(gjeldendeBehandling))

        val nyttVedtak = Vedtak(
                behandling = nyBehandling,
                vedtaksdato = now(),
                forrigeVedtakId = gjeldendeVedtak.id,
                opphørsdato = opphørsdato
        )

        // Trenger ikke flush her fordi det kreves unikhet på (behandlingid,aktiv) og det er ny behandlingsid
        vedtakRepository.save(gjeldendeVedtak.also { it.aktiv = false })
        vedtakRepository.save(nyttVedtak)

        val nyTilkjentYtelse = TilkjentYtelse(
                behandling = nyBehandling,
                opprettetDato = now(),
                endretDato = now()
        )
        tilkjentYtelseRepository.save(nyTilkjentYtelse)

        totrinnskontrollService.opprettEllerHentTotrinnskontroll(nyBehandling, saksbehandler)
        totrinnskontrollService.besluttTotrinnskontroll(nyBehandling, SYSTEM_NAVN, Beslutning.GODKJENT)

        behandlingRepository.save(nyBehandling.also { it.steg = StegType.FERDIGSTILLE_BEHANDLING })

        postProsessor(nyttVedtak)

        return Ressurs.success(nyttVedtak)
    }

    @Transactional
    fun lagreEllerOppdaterVedtakForAktivBehandling(behandling: Behandling,
                                                   personopplysningGrunnlag: PersonopplysningGrunnlag): Vedtak {
        val forrigeVedtak = hentForrigeVedtakPåFagsak(sisteBehandlingPåFagsak = behandling)

        // TODO: Midlertidig fiks før støtte for delvis innvilget
        val behandlingResultatType = midlertidigUtledBehandlingResultatType(
                hentetBehandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling))
        //val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id)

        val vedtak = Vedtak(
                behandling = behandling,
                forrigeVedtakId = forrigeVedtak?.id,
                opphørsdato = if (behandlingResultatType == BehandlingResultatType.OPPHØRT) now()
                        .førsteDagINesteMåned() else null,
                vedtaksdato = if (behandling.skalBehandlesAutomatisk) now() else null
        )

        return lagreOgDeaktiverGammel(vedtak)
    }


    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak): Vedtak {
        vedtak.stønadBrevPdF = dokumentService.genererBrevForVedtak(vedtak)
        return vedtak
    }

    fun hentUtbetalingBegrunnelserPåForrigeVedtak(fagsakId: Long): List<UtbetalingBegrunnelse> {
        val forrigeVedtak = hentForrigeVedtakPåFagsak(fagsakId)
        return forrigeVedtak?.utbetalingBegrunnelser?.toList() ?: emptyList()
    }

    fun leggTilInitielleUtbetalingsbegrunnelser(fagsakId: Long, behandling: Behandling) {
        slettUtbetalingBegrunnelser(behandling.id)
        val forrigeBehandling = behandlingService.hentForrigeBehandling(fagsakId, behandling)
        val forrigeTilkjentYtelse =
                if (forrigeBehandling != null) tilkjentYtelseRepository.findByBehandling(forrigeBehandling.id) else null
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandling(behandling.id)
        val personopplysningsGrunnlag =
                persongrunnlagService.hentAktiv(behandling.id) ?: error("Finner ikke personopplhysningsgrunnlag på behandling")
        val beregningsoversikt = TilkjentYtelseUtils.hentBeregningOversikt(
                tilkjentYtelseForBehandling = tilkjentYtelse,
                tilkjentYtelseForForrigeBehandling = forrigeTilkjentYtelse,
                personopplysningGrunnlag = personopplysningsGrunnlag)

        val relevantePerioder = beregningsoversikt.filter { it.endring.trengerBegrunnelse }

        val uendrede = relevantePerioder
                .filter { !it.endring.erEndret() }
                .map { Periode(it.periodeFom, it.periodeTom) }
        val satsendringer = relevantePerioder
                .filter { it.endring.type == BeregningEndringType.ENDRET_SATS }
                .map { Periode(it.periodeFom, it.periodeTom) }
        val målform = personopplysningsGrunnlag.søker.målform
        leggTilUtbetalingsbegrunnelseForUendrede(fagsakId, uendrede)
        leggTilUtbetalingsbegrunnelseForSatsendring(fagsakId, satsendringer, målform)
    }

    private fun leggTilUtbetalingsbegrunnelseForSatsendring(fagsakId: Long, perioder: List<Periode>, målform: Målform) {
        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")
        perioder.forEach {
            leggTilUtbetalingBegrunnelse(fagsakId,
                                         UtbetalingBegrunnelse(vedtak = vedtak,
                                                               fom = it.fom,
                                                               tom = it.tom,
                                                               begrunnelseType = VedtakBegrunnelseType.SATSENDRING,
                                                               vedtakBegrunnelse = VedtakBegrunnelse.SATSENDRING,
                                                               brevBegrunnelse =
                                                               VedtakBegrunnelse.SATSENDRING.hentBeskrivelse(
                                                                       vilkårsdato = it.fom.toString(), målform = målform)))
        }
    }

    private fun leggTilUtbetalingsbegrunnelseForUendrede(fagsakId: Long, perioder: List<Periode>) {
        val utbetalingsbegrunnelser =
                hentUtbetalingBegrunnelserPåForrigeVedtak(fagsakId).filter { Periode(it.fom, it.tom) in perioder }
        utbetalingsbegrunnelser.forEach { leggTilUtbetalingBegrunnelse(fagsakId = fagsakId, utbetalingBegrunnelse = it) }
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
            it.toRestUtbetalingBegrunnelse()
        }
    }

    @Transactional
    fun leggTilUtbetalingBegrunnelse(fagsakId: Long,
                                     utbetalingBegrunnelse: UtbetalingBegrunnelse): List<RestUtbetalingBegrunnelse> {

        val vedtak = hentVedtakForAktivBehandling(fagsakId) ?: throw Feil(message = "Finner ikke aktiv vedtak på behandling")

        if (vedtak.utbetalingBegrunnelser.none { it.erLik(utbetalingBegrunnelse) }) {
            val begrunnelse = UtbetalingBegrunnelse(vedtak = vedtak,
                                                    fom = utbetalingBegrunnelse.fom,
                                                    tom = utbetalingBegrunnelse.tom,
                                                    begrunnelseType = utbetalingBegrunnelse.vedtakBegrunnelse?.vedtakBegrunnelseType,
                                                    vedtakBegrunnelse = utbetalingBegrunnelse.vedtakBegrunnelse,
                                                    brevBegrunnelse = utbetalingBegrunnelse.brevBegrunnelse)

            vedtak.leggTilUtbetalingBegrunnelse(begrunnelse)

            lagreEllerOppdater(vedtak)
        }

        return vedtak.utbetalingBegrunnelser.map {
            it.toRestUtbetalingBegrunnelse()
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


        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(vedtak.behandling.id)
                                 ?: throw Feil("Finner ikke behandlingsresultat ved fastsetting av begrunnelse")

        val opprinneligUtbetalingBegrunnelse = vedtak.hentUtbetalingBegrunnelse(utbetalingBegrunnelseId)
                                               ?: throw Feil(message = "Fant ikke stønadbrevbegrunnelse med innsendt id")

        if (restPutUtbetalingBegrunnelse.vedtakBegrunnelse != null) {

            if (VedtakBegrunnelse.utenVilkår().contains(restPutUtbetalingBegrunnelse.vedtakBegrunnelse)) {
                vedtak.endreUtbetalingBegrunnelse(
                        opprinneligUtbetalingBegrunnelse.id,
                        restPutUtbetalingBegrunnelse.vedtakBegrunnelse,
                        restPutUtbetalingBegrunnelse.vedtakBegrunnelse.hentBeskrivelse(målform = personopplysningGrunnlag.søker.målform)
                )
            } else {
                val personerMedUtgjørendeVilkårForUtbetalingsperiode =
                        hentPersonerMedUtgjørendeVilkår(
                                behandlingResultat = behandlingResultat,
                                opprinneligUtbetalingBegrunnelse = opprinneligUtbetalingBegrunnelse,
                                oppdatertBegrunnelseType = restPutUtbetalingBegrunnelse.vedtakBegrunnelseType,
                                oppdatertVilkår = Vilkår.finnForBegrunnelse(restPutUtbetalingBegrunnelse.vedtakBegrunnelse))

                if (personerMedUtgjørendeVilkårForUtbetalingsperiode.isEmpty()) {
                    throw Feil(message = "Begrunnelsen samsvarte ikke med vilkårsvurderingen",
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

                val vilkårsdato = if (restPutUtbetalingBegrunnelse.vedtakBegrunnelseType == VedtakBegrunnelseType.REDUKSJON)
                    opprinneligUtbetalingBegrunnelse.tom.minusMonths(1).tilMånedÅr() else
                    opprinneligUtbetalingBegrunnelse.fom.minusMonths(1).tilMånedÅr()


                val barnasFødselsdatoer = slåSammen(barnaMedVilkårSomPåvirkerUtbetaling.map { it.fødselsdato.tilKortString() })

                val begrunnelseSomSkalPersisteres =
                        restPutUtbetalingBegrunnelse.vedtakBegrunnelse.hentBeskrivelse(gjelderSøker,
                                                                                       barnasFødselsdatoer,
                                                                                       vilkårsdato,
                                                                                       personopplysningGrunnlag.søker.målform)

                vedtak.endreUtbetalingBegrunnelse(
                        opprinneligUtbetalingBegrunnelse.id,
                        restPutUtbetalingBegrunnelse.vedtakBegrunnelse,
                        begrunnelseSomSkalPersisteres
                )
            }
        } else {

            vedtak.endreUtbetalingBegrunnelse(
                    opprinneligUtbetalingBegrunnelse.id,
                    restPutUtbetalingBegrunnelse.vedtakBegrunnelse,
                    "")
        }

        lagreEllerOppdater(vedtak)

        return vedtak.utbetalingBegrunnelser.toList()
    }

    /**
     * Må vite om det gjelder søker og/eller barn da dette bestememr ordlyd i brev.
     * Funksjonen utleder hvilke personer som trigger en gitt endring ved å kontrollere
     * at vilkår og begrunnelsetype som man forsøker å oppdatere mot resultatet man
     * skal begrunne.
     *
     * @param behandlingResultat - Behandlingresultatet man skal begrunne
     * @param opprinneligUtbetalingBegrunnelse - Begrunnelsen man ønsker å oppdatere
     * @param oppdatertBegrunnelseType - Brukes til å se om man skal sammenligne fom eller tom-dato
     * @param oppdatertVilkår -  Brukes til å sammenligne vilkår i behandlingResultat
     * @return List med par bestående av person og vilkåret de trigger endring på
     */
    private fun hentPersonerMedUtgjørendeVilkår(behandlingResultat: BehandlingResultat,
                                                opprinneligUtbetalingBegrunnelse: UtbetalingBegrunnelse,
                                                oppdatertBegrunnelseType: VedtakBegrunnelseType,
                                                oppdatertVilkår: Vilkår): List<Pair<Person, VilkårResultat>> {
        return behandlingResultat.personResultater.fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkår = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->
                when {
                    vilkårResultat.vilkårType != oppdatertVilkår -> false
                    vilkårResultat.periodeFom == null -> {
                        false
                    }
                    oppdatertBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE -> {
                        vilkårResultat.periodeFom!!.monthValue == opprinneligUtbetalingBegrunnelse.fom.minusMonths(1).monthValue && vilkårResultat.resultat == Resultat.JA
                    }
                    else -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.monthValue == opprinneligUtbetalingBegrunnelse.fom.minusMonths(
                                1).monthValue && vilkårResultat.resultat == Resultat.NEI
                    }
                }
            }

            val person =
                    persongrunnlagService.hentAktiv(behandlingResultat.behandling.id)?.personer?.firstOrNull { person -> person.personIdent.ident == personResultat.personIdent }
                    ?: throw Feil(message = "Kunne ikke finne person på personResultat")

            if (utgjørendeVilkår != null) {
                acc.add(Pair(person, utgjørendeVilkår))
            }
            acc
        }

    }

    fun hentForrigeVedtakPåFagsak(sisteBehandlingPåFagsak: Behandling): Vedtak? {
        val behandlinger = behandlingService.hentBehandlinger(sisteBehandlingPåFagsak.fagsak.id)

        return when (val forrigeBehandling =
                behandlinger.filter { it.id != sisteBehandlingPåFagsak.id }.maxByOrNull { it.opprettetTidspunkt }) {
            null -> null
            else -> hentAktivForBehandling(behandlingId = forrigeBehandling.id)
        }
    }

    private fun hentForrigeVedtakPåFagsak(fagsakId: Long): Vedtak? {
        val aktivtVedtak = hentVedtakForAktivBehandling(fagsakId) ?: error("Finner ingen aktivt vedtak på fagsak $fagsakId")
        return if (aktivtVedtak.forrigeVedtakId != null) hent(aktivtVedtak.forrigeVedtakId) else null
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

    fun lagreEllerOppdater(vedtak: Vedtak): Vedtak {
        return vedtakRepository.save(vedtak)
    }

    fun besluttVedtak(vedtak: Vedtak) {
        vedtak.vedtaksdato = now()
        lagreEllerOppdater(oppdaterVedtakMedStønadsbrev(vedtak))

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}


