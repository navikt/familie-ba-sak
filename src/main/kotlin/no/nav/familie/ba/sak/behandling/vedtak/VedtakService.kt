package no.nav.familie.ba.sak.behandling.vedtak

import no.nav.familie.ba.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingOpprinnelse
import no.nav.familie.ba.sak.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.Person
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.behandling.restDomene.RestPutUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.RestUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.restDomene.toRestUtbetalingBegrunnelse
import no.nav.familie.ba.sak.behandling.steg.StegType
import no.nav.familie.ba.sak.behandling.vilkår.*
import no.nav.familie.ba.sak.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelse
import no.nav.familie.ba.sak.beregning.domene.TilkjentYtelseRepository
import no.nav.familie.ba.sak.common.*
import no.nav.familie.ba.sak.common.Utils.midlertidigUtledBehandlingResultatType
import no.nav.familie.ba.sak.common.Utils.slåSammen
import no.nav.familie.ba.sak.dokument.DokumentService
import no.nav.familie.ba.sak.logg.LoggService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext.SYSTEM_NAVN
import no.nav.familie.ba.sak.totrinnskontroll.TotrinnskontrollService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.nare.core.evaluations.Resultat
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
                                      opprinnelse = BehandlingOpprinnelse.MANUELL)

        // Må flushe denne til databasen for å sørge å opprettholde unikhet på (fagsakid,aktiv)
        behandlingRepository.saveAndFlush(gjeldendeBehandling.also { it.aktiv = false })
        behandlingRepository.save(nyBehandling)
        loggService.opprettBehandlingLogg(nyBehandling)

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
        val forrigeVedtak = hentForrigeVedtak(behandling = behandling)

        // TODO: Midlertidig fiks før støtte for delvis innvilget
        val behandlingResultatType = midlertidigUtledBehandlingResultatType(
                hentetBehandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id))
        //val behandlingResultatType = behandlingResultatService.hentBehandlingResultatTypeFraBehandling(behandling.id)

        val vedtak = Vedtak(
                behandling = behandling,
                forrigeVedtakId = forrigeVedtak?.id,
                ansvarligEnhet = arbeidsfordelingService.bestemBehandlendeEnhet(behandling),
                opphørsdato = if (behandlingResultatType == BehandlingResultatType.OPPHØRT) now()
                        .førsteDagINesteMåned() else null
        )

        return lagreOgDeaktiverGammel(vedtak)
    }


    @Transactional
    fun oppdaterVedtakMedStønadsbrev(vedtak: Vedtak) {
        vedtak.stønadBrevPdF = dokumentService.genererBrevForVedtak(vedtak)

        lagreOgDeaktiverGammel(vedtak)
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


        val behandlingResultat = behandlingResultatService.hentAktivForBehandling(vedtak.behandling.id)
                                 ?: throw Feil("Finner ikke behandlingsresultat ved fastsetting av begrunnelse")

        val stønadBrevBegrunnelse = vedtak.hentUtbetalingBegrunnelse(utbetalingBegrunnelseId)
                                    ?: throw Feil(message = "Fant ikke stønadbrevbegrunnelse med innsendt id")

        if (restPutUtbetalingBegrunnelse.vedtakBegrunnelse != null && restPutUtbetalingBegrunnelse.resultat != null) {
            val vilkår = Vilkår.values().firstOrNull {
                it.begrunnelser.filter { begrunnelse ->
                    begrunnelse.value.contains(restPutUtbetalingBegrunnelse.vedtakBegrunnelse)
                }.isNotEmpty()
            } ?: throw Feil("Finner ikke vilkår for valgt begrunnelse")


            val personerMedUtgjørendeVilkårForUtbetalingsperiode =
                    hentPersonerMedUtgjørendeVilkår(
                            behandlingResultat = behandlingResultat,
                            utbetalingBegrunnelse = stønadBrevBegrunnelse,
                            resultat = restPutUtbetalingBegrunnelse.resultat,
                            vilkår = vilkår)

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

            val vilkårsdato = if (personerMedUtgjørendeVilkårForUtbetalingsperiode.size == 1) {
                personerMedUtgjørendeVilkårForUtbetalingsperiode[0].second.periodeFom!!.tilKortString()
            } else {
                stønadBrevBegrunnelse.fom.minusMonths(1).tilMånedÅr()
            }


            val barnasFødselsdatoer = slåSammen(barnaMedVilkårSomPåvirkerUtbetaling.map { it.fødselsdato.tilKortString() })
            val begrunnelse = vilkår.begrunnelser[restPutUtbetalingBegrunnelse.resultat]
            val begrunnelsesfunksjon = begrunnelse?.get(restPutUtbetalingBegrunnelse.vedtakBegrunnelse)?.second

            val begrunnelseSomSkalPersisteres = begrunnelsesfunksjon?.invoke(gjelderSøker, barnasFødselsdatoer, vilkårsdato)

            vedtak.endreUtbetalingBegrunnelse(
                    stønadBrevBegrunnelse.id,
                    restPutUtbetalingBegrunnelse.resultat,
                    restPutUtbetalingBegrunnelse.vedtakBegrunnelse,
                    begrunnelseSomSkalPersisteres
            )
        } else {
            vedtak.endreUtbetalingBegrunnelse(
                    stønadBrevBegrunnelse.id,
                    restPutUtbetalingBegrunnelse.resultat,
                    restPutUtbetalingBegrunnelse.vedtakBegrunnelse,
                    ""
            )
        }

        lagreEllerOppdater(vedtak)

        return vedtak.utbetalingBegrunnelser.toList()
    }

    private fun hentPersonerMedUtgjørendeVilkår(behandlingResultat: BehandlingResultat,
                                                utbetalingBegrunnelse: UtbetalingBegrunnelse,
                                                resultat: BehandlingResultatType,
                                                vilkår: Vilkår): List<Pair<Person, VilkårResultat>> {
        return behandlingResultat.personResultater.fold(mutableListOf()) { acc, personResultat ->
            val utgjørendeVilkår = personResultat.vilkårResultater.firstOrNull { vilkårResultat ->
                when {
                    vilkårResultat.vilkårType != vilkår -> false
                    vilkårResultat.periodeFom == null -> {
                        false
                    }
                    resultat == BehandlingResultatType.INNVILGET -> {
                        vilkårResultat.periodeFom!!.monthValue == utbetalingBegrunnelse.fom.minusMonths(1).monthValue && vilkårResultat.resultat == Resultat.JA
                    }
                    else -> {
                        vilkårResultat.periodeTom != null && vilkårResultat.periodeTom!!.monthValue == utbetalingBegrunnelse.fom.minusMonths(
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

    fun hentForrigeVedtak(behandling: Behandling): Vedtak? {
        val behandlinger = behandlingService.hentBehandlinger(behandling.fagsak.id)


        return when (val forrigeBehandling = behandlinger.filter { it.id != behandling.id }.maxBy { it.opprettetTidspunkt }) {
            null -> null
            else -> hentAktivForBehandling(behandlingId = forrigeBehandling.id)
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

    fun lagreEllerOppdater(vedtak: Vedtak): Vedtak {
        return vedtakRepository.save(vedtak)
    }

    fun besluttVedtak(vedtak: Vedtak) {
        vedtak.vedtaksdato = now()
        oppdaterVedtakMedStønadsbrev(vedtak)

        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} beslutter vedtak $vedtak")
        lagreEllerOppdater(vedtak)
    }

    companion object {

        val LOG = LoggerFactory.getLogger(this::class.java)
    }
}


