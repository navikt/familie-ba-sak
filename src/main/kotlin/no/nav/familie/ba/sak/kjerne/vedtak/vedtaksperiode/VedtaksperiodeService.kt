package no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.FunksjonellFeil
import no.nav.familie.ba.sak.common.NullablePeriode
import no.nav.familie.ba.sak.common.Periode
import no.nav.familie.ba.sak.common.TIDENES_ENDE
import no.nav.familie.ba.sak.common.TIDENES_MORGEN
import no.nav.familie.ba.sak.common.førsteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.sisteDagIInneværendeMåned
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.config.FeatureToggleConfig.Companion.BRUK_VEDTAKSTYPE_MED_BEGRUNNELSER
import no.nav.familie.ba.sak.config.FeatureToggleService
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedBegrunnelse
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.ekstern.restDomene.RestVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Vedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.dokument.hentVedtaksbrevtype
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakBegrunnelseRepository
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentPersonerMedUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon.Companion.finnVilkårFor
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseUtils.vedtakBegrunnelserIkkeTilknyttetVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseUtils.vilkårMedVedtakBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksperiodeType
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.domene.byggBegrunnelserOgFriteksterForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilRestVedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkårsvurdering
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class VedtaksperiodeService(
        private val behandlingRepository: BehandlingRepository,
        private val persongrunnlagService: PersongrunnlagService,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
        private val vedtakBegrunnelseRepository: VedtakBegrunnelseRepository,
        private val vedtaksperiodeRepository: VedtaksperiodeRepository,
        private val vilkårsvurderingRepository: VilkårsvurderingRepository,
        private val featureToggleService: FeatureToggleService,
) {

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET && vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()) {
            throw FunksjonellFeil("Det ble sendt med både fritekst og begrunnelse. " +
                                  "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler.")
        }

        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun lagre(vedtaksperiodeMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): List<VedtaksperiodeMedBegrunnelser> =
            vedtaksperiodeRepository.saveAll(vedtaksperiodeMedBegrunnelser)

    fun slettVedtaksperioderFor(vedtak: Vedtak) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(vedtak)
    }

    @Deprecated("Fjernes når frontend støtter put av fritekster og standardbegrunnelser hver for seg.")
    fun oppdaterVedtaksperiodeMedBegrunnelser(vedtaksperiodeId: Long,
                                              restPutVedtaksperiodeMedBegrunnelse: RestPutVedtaksperiodeMedBegrunnelse): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)
        val begrunnelserMedFeil = mutableListOf<VedtakBegrunnelseSpesifikasjon>()

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(restPutVedtaksperiodeMedBegrunnelse.begrunnelser.map {
            val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling
            val vilkår = it.vedtakBegrunnelseSpesifikasjon.finnVilkårFor()
            val personIdenter =
                    if (vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET) hentPersonIdenterFraUtbetalingsperiode(
                            hentUtbetalingsperioder(behandling)) else
                        hentPersonerMedUtgjørendeVilkår(
                                vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
                                                   ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak"),
                                vedtaksperiode = Periode(
                                        fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                                        tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                                ),
                                oppdatertBegrunnelseType = it.vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType,
                                utgjørendeVilkår = vilkår,
                                aktuellePersonerForVedtaksperiode = persongrunnlagService.hentAktiv(behandling.id)?.personer?.toList()
                                                                    ?: error(
                                                                            "Finner ikke personer på behandling ved begrunning av vedtak")
                        ).map { person -> person.personIdent.ident }

            if (personIdenter.isEmpty()) {
                begrunnelserMedFeil.add(it.vedtakBegrunnelseSpesifikasjon)
            }

            it.vedtakBegrunnelseSpesifikasjon.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser, personIdenter)
        })

        if (begrunnelserMedFeil.isNotEmpty()) {
            throw FunksjonellFeil(
                    melding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.",
                    frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.\n" +
                                          begrunnelserMedFeil.fold("") { acc, vedtakBegrunnelseSpesifikasjon ->
                                              acc + "'${vedtakBegrunnelseSpesifikasjon.tittel}' forventer vurdering på '${vedtakBegrunnelseSpesifikasjon.finnVilkårFor()?.beskrivelse ?: "ukjent vilkår"}'"
                                          }
            )
        }

        vedtaksperiodeMedBegrunnelser.settFritekster(restPutVedtaksperiodeMedBegrunnelse.fritekster.map {
            tilVedtaksbegrunnelseFritekst(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser, fritekst = it)
        })

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedFritekster(vedtaksperiodeId: Long,
                                            restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settFritekster(restPutVedtaksperiodeMedFritekster.fritekster.map {
            tilVedtaksbegrunnelseFritekst(vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser, fritekst = it)
        })

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(vedtaksperiodeId: Long,
                                                      restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)
        val begrunnelserMedFeil = mutableListOf<VedtakBegrunnelseSpesifikasjon>()

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling
        val utbetalingsperioder = hentUtbetalingsperioder(behandling)

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(restPutVedtaksperiodeMedStandardbegrunnelser.standardbegrunnelser.map {

            val vilkår = it.finnVilkårFor()
            val persongrunnlag = persongrunnlagService.hentAktiv(behandling.id) ?: error("Finner ikke persongrunnlag")
            val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
                                   ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

            val identerMedUtbetaling = hentUtbetalingsperiodeForVedtaksperiode(utbetalingsperioder = utbetalingsperioder,
                                                                               fom = if (vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET)
                                                                                   null
                                                                               else vedtaksperiodeMedBegrunnelser.fom).utbetalingsperiodeDetaljer
                    .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent }

            val personIdenter = when {
                it == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR -> persongrunnlag.personer
                        .filter { person ->
                            person
                                    .hentSeksårsdag()
                                    .toYearMonth() == vedtaksperiodeMedBegrunnelser.fom?.toYearMonth() ?: TIDENES_ENDE.toYearMonth()
                        }.map { person -> person.personIdent.ident }

                it == VedtakBegrunnelseSpesifikasjon.REDUKSJON_MANGLENDE_OPPLYSNINGER || it == VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER -> if (vilkårsvurdering.harPersonerManglerOpplysninger())
                    emptyList() else error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")

                vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET -> identerMedUtbetaling

                else -> hentPersonerMedUtgjørendeVilkår(
                        vilkårsvurdering = vilkårsvurdering,
                        vedtaksperiode = Periode(
                                fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                                tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                        ),
                        oppdatertBegrunnelseType = it.vedtakBegrunnelseType,
                        utgjørendeVilkår = vilkår,
                        aktuellePersonerForVedtaksperiode = persongrunnlagService.hentAktiv(behandling.id)?.personer?.filter { person ->
                            if (it.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                                identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                            } else true
                        }?.toList() ?: error(
                                "Finner ikke personer på behandling ved begrunning av vedtak")
                ).map { person -> person.personIdent.ident }
            }

            if (it == VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                && SatsService.finnSatsendring(vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN).isEmpty()) {
                throw FunksjonellFeil(melding = "Begrunnelsen stemmer ikke med satsendring.",
                                      frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse.")
            }

            if (!vedtakBegrunnelserIkkeTilknyttetVilkår.contains(it) && personIdenter.isEmpty()) {
                begrunnelserMedFeil.add(it)
            }

            it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser, personIdenter)
        })

        if (begrunnelserMedFeil.isNotEmpty()) {
            throw FunksjonellFeil(
                    melding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.",
                    frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.\n" +
                                          begrunnelserMedFeil.fold("") { acc, vedtakBegrunnelseSpesifikasjon ->
                                              acc + "'${vedtakBegrunnelseSpesifikasjon.tittel}' forventer vurdering på '${vedtakBegrunnelseSpesifikasjon.finnVilkårFor()?.beskrivelse ?: "ukjent vilkår"}'"
                                          }
            )
        }

        if (vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()) {
            throw FunksjonellFeil(melding = "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                                            "Legg først til en ny begrunnelse eller fjern friteksten(e).",
                                  frontendFeilmelding = "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                                                        "Legg først til en ny begrunnelse eller fjern friteksten(e).")
        }

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak: Vedtak, barnaSomVurderes: List<String>) {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = vedtak.behandling.id)
        val vurderteBarnSomPersoner =
                barnaSomVurderes.map { barnSomVurderes ->
                    persongrunnlag.barna.find { it.personIdent.ident == barnSomVurderes }
                    ?: error("Finner ikke barn som har blitt vurdert i persongrunnlaget")
                }

        vurderteBarnSomPersoner.forEach { barn ->
            val vedtaksperiodeMedBegrunnelser = vedtaksperioderMedBegrunnelser.firstOrNull {
                barn.fødselsdato.toYearMonth()
                        .plusMonths(1)
                        .equals(it.fom?.toYearMonth() ?: TIDENES_ENDE)
            } ?: throw Feil("Finner ikke vedtaksperiode å begrunne for barn fra hendelse")

            vedtaksperiodeMedBegrunnelser.settBegrunnelser(listOf(Vedtaksbegrunnelse(
                    vedtakBegrunnelseSpesifikasjon = if (vedtak.behandling.fagsak.status == FagsakStatus.LØPENDE) {
                        VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
                    } else VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE,
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
            )))
            lagre(vedtaksperiodeMedBegrunnelser)
        }
    }

    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak) {

        slettVedtaksperioderFor(vedtak)
        if (vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) {

            val vedtakstype = hentVedtaksbrevtype(vedtak.behandling)
            val erAutobrevFor6Og18År = vedtakstype == Vedtaksbrevtype.AUTOVEDTAK_BARN6_ÅR
                                       || vedtakstype == Vedtaksbrevtype.AUTOVEDTAK_BARN18_ÅR

            val fom = if (erAutobrevFor6Og18År) {
                YearMonth.now().førsteDagIInneværendeMåned()
            } else null

            val tom = if (erAutobrevFor6Og18År) {
                finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(vedtak.behandling.id)
            } else null

            lagre(VedtaksperiodeMedBegrunnelser(
                    fom = fom,
                    tom = tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
            ))
        } else if (featureToggleService.isEnabled(BRUK_VEDTAKSTYPE_MED_BEGRUNNELSER) && !behandlingerIGammelState.contains(vedtak.behandling.id)) {
            val utbetalingOgOpphørsperioder =
                    (hentUtbetalingsperioder(vedtak.behandling) + hentOpphørsperioder(vedtak.behandling)).map {
                        VedtaksperiodeMedBegrunnelser(
                                fom = it.periodeFom,
                                tom = it.periodeTom,
                                vedtak = vedtak,
                                type = it.vedtaksperiodetype
                        )
                    }
            val avslagsperioder = hentAvslagsperioderMedBegrunnelser(vedtak)

            lagre(utbetalingOgOpphørsperioder + avslagsperioder)
        }
    }

    fun kopierOverVedtaksperioder(deaktivertVedtak: Vedtak, aktivtVedtak: Vedtak) {
        val gamleVedtaksperioderMedBegrunnelser = vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser = lagre(VedtaksperiodeMedBegrunnelser(
                    vedtak = aktivtVedtak,
                    fom = vedtaksperiodeMedBegrunnelser.fom,
                    tom = vedtaksperiodeMedBegrunnelser.tom,
                    type = vedtaksperiodeMedBegrunnelser.type,
            ))

            lagre(nyVedtaksperiodeMedBegrunnelser.copy(
                    begrunnelser = vedtaksperiodeMedBegrunnelser.begrunnelser.map { it.kopier(nyVedtaksperiodeMedBegrunnelser) }
                            .toMutableSet(),
                    fritekster = vedtaksperiodeMedBegrunnelser.fritekster.map { it.kopier(nyVedtaksperiodeMedBegrunnelser) }
                            .toMutableSet()
            ))
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)
    }

    fun hentRestVedtaksperiodeMedBegrunnelser(vedtak: Vedtak): List<RestVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling
        val utbetalingsperioder = hentUtbetalingsperioder(behandling)
        val persongrunnlag = persongrunnlagService.hentAktiv(behandling.id) ?: error("Finner ikke persongrunnlag")

        return vedtaksperioderMedBegrunnelser.map { vedtaksperiodeMedBegrunnelser ->
            val vilkår = Vilkår.values()
            val gyldigeBegrunnelser = mutableSetOf<VedtakBegrunnelseSpesifikasjon>()

            when (vedtaksperiodeMedBegrunnelser.type) {
                Vedtaksperiodetype.FORTSATT_INNVILGET -> {
                    gyldigeBegrunnelser.addAll(vilkårMedVedtakBegrunnelser.flatMap { it.value }
                                                       .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET })
                }
                Vedtaksperiodetype.AVSLAG -> {
                    gyldigeBegrunnelser.addAll(vilkårMedVedtakBegrunnelser.flatMap { it.value }
                                                       .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG })
                }
                else -> {
                    val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
                                           ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

                    val identerMedUtbetaling =
                            if (vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.OPPHØR) emptyList() else hentUtbetalingsperiodeForVedtaksperiode(
                                    utbetalingsperioder = utbetalingsperioder,
                                    fom = vedtaksperiodeMedBegrunnelser.fom).utbetalingsperiodeDetaljer
                                    .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent }


                    vilkår.forEach {
                        gyldigeBegrunnelser.addAll(hentBegrunnelserForVilkår(
                                vilkår = it,
                                vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                                vilkårsvurdering = vilkårsvurdering,
                                persongrunnlag = persongrunnlag,
                                identerMedUtbetaling = identerMedUtbetaling
                        ))
                    }
                }
            }

            vedtaksperiodeMedBegrunnelser.tilRestVedtaksperiodeMedBegrunnelser(gyldigeBegrunnelser.filter { it.erTilgjengeligFrontend && !it.erFritekstBegrunnelse() }
                                                                                       .toList())
        }
    }

    private fun hentBegrunnelserForVilkår(vilkår: Vilkår,
                                          vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser,
                                          vilkårsvurdering: Vilkårsvurdering,
                                          persongrunnlag: PersonopplysningGrunnlag,
                                          identerMedUtbetaling: List<String>): MutableList<VedtakBegrunnelseSpesifikasjon> {
        val begrunnelserForVilkår = vilkårMedVedtakBegrunnelser[vilkår] ?: emptyList()

        return (begrunnelserForVilkår + vedtakBegrunnelserIkkeTilknyttetVilkår).filter { begrunnelseForVilkår ->
            begrunnelseForVilkår.vedtakBegrunnelseType != VedtakBegrunnelseType.AVSLAG && begrunnelseForVilkår.vedtakBegrunnelseType.tilVedtaksperiodeType() == vedtaksperiodeMedBegrunnelser.type
        }.fold(mutableListOf()) { gyldigeBegrunnelser, begrunnelseForVilkår ->

            when {
                (begrunnelseForVilkår == VedtakBegrunnelseSpesifikasjon.REDUKSJON_MANGLENDE_OPPLYSNINGER || begrunnelseForVilkår == VedtakBegrunnelseSpesifikasjon.OPPHØR_IKKE_MOTTATT_OPPLYSNINGER)
                && vilkårsvurdering.harPersonerManglerOpplysninger() ->
                    gyldigeBegrunnelser.add(begrunnelseForVilkår)

                begrunnelseForVilkår == VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING
                && SatsService.finnSatsendring(vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN).isNotEmpty() ->
                    gyldigeBegrunnelser.add(begrunnelseForVilkår)

                begrunnelseForVilkår == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_6_ÅR
                && persongrunnlag.harBarnMedSeksårsdagPåFom(vedtaksperiodeMedBegrunnelser.fom) ->
                    gyldigeBegrunnelser.add(begrunnelseForVilkår)

                else -> {
                    val personIdenter = hentPersonerMedUtgjørendeVilkår(
                            vilkårsvurdering = vilkårsvurdering,
                            vedtaksperiode = Periode(
                                    fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                                    tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                            ),
                            oppdatertBegrunnelseType = begrunnelseForVilkår.vedtakBegrunnelseType,
                            utgjørendeVilkår = vilkår,
                            aktuellePersonerForVedtaksperiode = persongrunnlag.personer.filter { person ->
                                if (begrunnelseForVilkår.vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                                    identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                                } else true
                            }.toList())

                    if (personIdenter.isNotEmpty()) gyldigeBegrunnelser.add(begrunnelseForVilkår)
                }
            }

            gyldigeBegrunnelser
        }
    }

    fun oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(vedtak: Vedtak,
                                                               vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon) {
        val vedtaksperioder = hentPersisterteVedtaksperioder(vedtak)

        val fortsattInnvilgetPeriode: VedtaksperiodeMedBegrunnelser =
                vedtaksperioder.singleOrNull() ?: throw Feil("Finner ingen eller flere vedtaksperioder ved fortsatt innvilget")

        fortsattInnvilgetPeriode.settBegrunnelser(listOf(
                Vedtaksbegrunnelse(
                        vedtaksperiodeMedBegrunnelser = fortsattInnvilgetPeriode,
                        vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                        personIdenter = hentPersonIdenterFraUtbetalingsperiode(hentUtbetalingsperioder(vedtak.behandling))
                )
        ))

        lagre(fortsattInnvilgetPeriode)
    }

    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<Begrunnelse> {
        val vedtaksperiode = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)
        val behandlingId = vedtaksperiode.vedtak.behandling.id
        val persongrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandlingId)
                             ?: throw Feil("Finner ikke persongrunnlag for behandling $behandlingId")
        return byggBegrunnelserOgFriteksterForVedtaksperiode(
                vedtaksperiode = vedtaksperiode,
                personerIPersongrunnlag = persongrunnlag.personer.toList(),
                målform = persongrunnlag.søker.målform,
                brukBegrunnelserFraSanity = false,
        )
    }

    private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId: Long): LocalDate =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandlingId))
                    .filter { it.stønadFom <= YearMonth.now() && it.stønadTom >= YearMonth.now() }
                    .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()

            ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")

    fun hentUtbetalingsperioder(behandling: Behandling): List<Utbetalingsperiode> {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilUtbetalingsperioder(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag
        )
    }

    fun hentVedtaksperioder(behandling: Behandling): List<Vedtaksperiode> {
        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger =
                behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
                iverksatteBehandlinger = iverksatteBehandlinger,
                behandlingFørFølgende = behandling
        )

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
                if (forrigeIverksatteBehandling != null)
                    persongrunnlagService.hentAktiv(behandlingId = forrigeIverksatteBehandling.id) else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
                if (forrigeIverksatteBehandling != null)
                    andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                            behandlingId = forrigeIverksatteBehandling.id
                    ) else emptyList()

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        val utbetalingsperioder = hentUtbetalingsperioder(behandling)

        val opphørsperioder = mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
        )

        val avslagsperioder =
                mapTilAvslagsperioderDeprecated(
                        vedtakBegrunnelser = vedtakBegrunnelseRepository.finnForAktivtVedtakPåBehandling(behandlingId = behandling.id)
                )

        return utbetalingsperioder + opphørsperioder + avslagsperioder
    }

    fun hentOpphørsperioder(behandling: Behandling): List<Opphørsperiode> {
        if (behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) return emptyList()

        val iverksatteBehandlinger =
                behandlingRepository.finnIverksatteBehandlinger(fagsakId = behandling.fagsak.id)

        val forrigeIverksatteBehandling: Behandling? = Behandlingutils.hentForrigeIverksatteBehandling(
                iverksatteBehandlinger = iverksatteBehandlinger,
                behandlingFørFølgende = behandling
        )

        val forrigePersonopplysningGrunnlag: PersonopplysningGrunnlag? =
                if (forrigeIverksatteBehandling != null)
                    persongrunnlagService.hentAktiv(behandlingId = forrigeIverksatteBehandling.id)
                else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
                if (forrigeIverksatteBehandling != null) andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                        behandlingId = forrigeIverksatteBehandling.id) else emptyList()

        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandlingId = behandling.id)
                                       ?: return emptyList()
        val andelerTilkjentYtelse =
                andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilOpphørsperioder(
                forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
                forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
                personopplysningGrunnlag = personopplysningGrunnlag,
                andelerTilkjentYtelse = andelerTilkjentYtelse
        )
    }

    private fun hentAvslagsperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = vedtak.behandling.id)
                               ?: throw Feil("Fant ikke vilkårsvurdering for behandling ${vedtak.behandling.id} ved generering av avslagsperioder")

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
                vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
                        .filter { it.erEksplisittAvslagPåSøknad == true }
                        .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

        return periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val begrunnelserMedIdenter: Map<VedtakBegrunnelseSpesifikasjon, List<String>> =
                    begrunnelserMedIdentgrupper(vilkårResultater)

            VedtaksperiodeMedBegrunnelser(vedtak = vedtak,
                                          fom = fellesPeriode.fom,
                                          tom = fellesPeriode.tom,
                                          type = Vedtaksperiodetype.AVSLAG)
                    .apply {
                        begrunnelser.addAll(begrunnelserMedIdenter.map { (begrunnelse, identer) ->
                            Vedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser = this,
                                               vedtakBegrunnelseSpesifikasjon = begrunnelse,
                                               personIdenter = identer
                            )
                        })
                    }
        }
    }

    private fun begrunnelserMedIdentgrupper(vilkårResultater: List<VilkårResultat>): Map<VedtakBegrunnelseSpesifikasjon, List<String>> {
        val begrunnelseOgIdentListe: List<Pair<VedtakBegrunnelseSpesifikasjon, String>> =
                vilkårResultater
                        .map { vilkår ->
                            val personIdent = vilkår.personResultat?.personIdent
                                              ?: throw Feil("VilkårResultat ${vilkår.id} mangler PersonResultat ved sammenslåing av begrunnelser")
                            vilkår.vedtakBegrunnelseSpesifikasjoner.map { begrunnelse -> Pair(begrunnelse, personIdent) }
                        }
                        .flatten()

        return begrunnelseOgIdentListe
                .groupBy { (begrunnelse, _) -> begrunnelse }
                .mapValues { (_, parGruppe) -> parGruppe.map { it.second } }
    }

    companion object {

        val behandlingerIGammelState: List<Long> = listOf(
                1058408,
                1075799,
                1075801,
                1082701,
                1082651,
        )
    }
}