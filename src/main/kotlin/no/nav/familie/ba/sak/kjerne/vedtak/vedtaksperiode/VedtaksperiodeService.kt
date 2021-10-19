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
import no.nav.familie.ba.sak.ekstern.restDomene.BarnMedOpplysninger
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedFritekster
import no.nav.familie.ba.sak.ekstern.restDomene.RestPutVedtaksperiodeMedStandardbegrunnelser
import no.nav.familie.ba.sak.kjerne.behandling.Behandlingutils
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingRepository
import no.nav.familie.ba.sak.kjerne.behandling.domene.BehandlingResultat
import no.nav.familie.ba.sak.kjerne.beregning.SatsService
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelse
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.beregning.domene.YtelseType
import no.nav.familie.ba.sak.kjerne.dokument.BrevKlient
import no.nav.familie.ba.sak.kjerne.dokument.domene.maler.Brevmal
import no.nav.familie.ba.sak.kjerne.dokument.domene.tilTriggesAv
import no.nav.familie.ba.sak.kjerne.dokument.hentVedtaksbrevmal
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.EndretUtbetalingAndelRepository
import no.nav.familie.ba.sak.kjerne.endretutbetaling.domene.hentPersonerForEtterEndretUtbetalingsperiode
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlag
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersonopplysningGrunnlagRepository
import no.nav.familie.ba.sak.kjerne.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.kjerne.vedtak.Vedtak
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakUtils.hentPersonerForAlleUtgjørendeVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseSpesifikasjon
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.VedtakBegrunnelseType
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.erTilknyttetVilkår
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilSanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.tilVedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Begrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeMedBegrunnelser
import no.nav.familie.ba.sak.kjerne.vedtak.domene.VedtaksperiodeRepository
import no.nav.familie.ba.sak.kjerne.vedtak.domene.byggBegrunnelserOgFriteksterForVedtaksperiode
import no.nav.familie.ba.sak.kjerne.vedtak.domene.tilVedtaksbegrunnelseFritekst
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.Vilkår
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårResultat
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.domene.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class VedtaksperiodeService(
    private val behandlingRepository: BehandlingRepository,
    private val persongrunnlagRepository: PersonopplysningGrunnlagRepository,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val vedtaksperiodeRepository: VedtaksperiodeRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val brevKlient: BrevKlient,
    private val søknadGrunnlagService: SøknadGrunnlagService,
    private val endretUtbetalingAndelRepository: EndretUtbetalingAndelRepository
) {

    fun lagre(vedtaksperiodeMedBegrunnelser: VedtaksperiodeMedBegrunnelser): VedtaksperiodeMedBegrunnelser {
        if (vedtaksperiodeMedBegrunnelser.vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET && vedtaksperiodeMedBegrunnelser.harFriteksterOgStandardbegrunnelser()) {
            throw FunksjonellFeil(
                "Det ble sendt med både fritekst og begrunnelse. " +
                    "Vedtaket skal enten ha fritekst eller bregrunnelse, men ikke begge deler."
            )
        }

        return vedtaksperiodeRepository.save(vedtaksperiodeMedBegrunnelser)
    }

    fun lagre(vedtaksperiodeMedBegrunnelser: List<VedtaksperiodeMedBegrunnelser>): List<VedtaksperiodeMedBegrunnelser> =
        vedtaksperiodeRepository.saveAll(vedtaksperiodeMedBegrunnelser)

    fun slettVedtaksperioderFor(vedtak: Vedtak) {
        vedtaksperiodeRepository.slettVedtaksperioderFor(vedtak)
    }

    fun oppdaterVedtaksperiodeMedFritekster(
        vedtaksperiodeId: Long,
        restPutVedtaksperiodeMedFritekster: RestPutVedtaksperiodeMedFritekster
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        vedtaksperiodeMedBegrunnelser.settFritekster(
            restPutVedtaksperiodeMedFritekster.fritekster.map {
                tilVedtaksbegrunnelseFritekst(
                    vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    fritekst = it
                )
            }
        )

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperiodeMedStandardbegrunnelser(
        vedtaksperiodeId: Long,
        restPutVedtaksperiodeMedStandardbegrunnelser: RestPutVedtaksperiodeMedStandardbegrunnelser
    ): Vedtak {
        val vedtaksperiodeMedBegrunnelser = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)
        val begrunnelserMedFeil = mutableListOf<VedtakBegrunnelseSpesifikasjon>()

        val behandling = vedtaksperiodeMedBegrunnelser.vedtak.behandling

        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)
        val persongrunnlag =
            persongrunnlagRepository.findByBehandlingAndAktiv(behandling.id) ?: error("Finner ikke persongrunnlag")

        val utvidetVedtaksperiodeMedBegrunnelser =
            vedtaksperiodeMedBegrunnelser.tilUtvidetVedtaksperiodeMedBegrunnelser(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag
            )

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
            ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

        val identerMedUtbetaling = utvidetVedtaksperiodeMedBegrunnelser
            .utbetalingsperiodeDetaljer
            .map { utbetalingsperiodeDetalj -> utbetalingsperiodeDetalj.person.personIdent }

        vedtaksperiodeMedBegrunnelser.settBegrunnelser(
            restPutVedtaksperiodeMedStandardbegrunnelser.standardbegrunnelser.map {

                val triggesAv = it.tilSanityBegrunnelse(brevKlient.hentSanityBegrunnelse()).tilTriggesAv()
                val vedtakBegrunnelseType = it.vedtakBegrunnelseType

                val personerGjeldendeForBegrunnelseIdenter: MutableSet<String> = when {
                    triggesAv.barnMedSeksårsdag ->
                        persongrunnlag.barna
                            .filter { person ->
                                person.hentSeksårsdag().toYearMonth() == (
                                    vedtaksperiodeMedBegrunnelser.fom?.toYearMonth()
                                        ?: TIDENES_ENDE.toYearMonth()
                                    )
                            }.map { person -> person.personIdent.ident }

                    triggesAv.personerManglerOpplysninger -> if (vilkårsvurdering.harPersonerManglerOpplysninger())
                        emptyList() else error("Legg til opplysningsplikt ikke oppfylt begrunnelse men det er ikke person med det resultat")

                    vedtaksperiodeMedBegrunnelser.type == Vedtaksperiodetype.FORTSATT_INNVILGET ||
                        vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET -> identerMedUtbetaling

                    triggesAv.etterEndretUtbetaling -> hentPersonerForEtterEndretUtbetalingsperiode(
                        endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(
                            behandling.id
                        ),
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                        triggesAv = triggesAv
                    )

                    else -> hentPersonerForAlleUtgjørendeVilkår(
                        vilkårsvurdering = vilkårsvurdering,
                        vedtaksperiode = Periode(
                            fom = vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN,
                            tom = vedtaksperiodeMedBegrunnelser.tom ?: TIDENES_ENDE
                        ),
                        oppdatertBegrunnelseType = vedtakBegrunnelseType,
                        aktuellePersonerForVedtaksperiode = persongrunnlagRepository.findByBehandlingAndAktiv(behandling.id)?.personer?.filter { person ->
                            if (vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE) {
                                identerMedUtbetaling.contains(person.personIdent.ident) || person.type == PersonType.SØKER
                            } else true
                        }?.toList() ?: error(
                            "Finner ikke personer på behandling ved begrunning av vedtak"
                        ),
                        triggesAv = triggesAv
                    ).map { person -> person.personIdent.ident }
                }.toMutableSet()

                if (triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD)) {
                    personerGjeldendeForBegrunnelseIdenter.add(persongrunnlag.søker.personIdent.ident)
                }

                if (it == VedtakBegrunnelseSpesifikasjon.INNVILGET_SATSENDRING &&
                    SatsService.finnSatsendring(vedtaksperiodeMedBegrunnelser.fom ?: TIDENES_MORGEN).isEmpty()
                ) {
                    throw FunksjonellFeil(
                        melding = "Begrunnelsen stemmer ikke med satsendring.",
                        frontendFeilmelding = "Begrunnelsen stemmer ikke med satsendring. Vennligst velg en annen begrunnelse."
                    )
                }

                val sanityBegrunnelser = brevKlient.hentSanityBegrunnelse()
                if (it.erTilknyttetVilkår(sanityBegrunnelser) && personerGjeldendeForBegrunnelseIdenter.isEmpty()) {
                    begrunnelserMedFeil.add(it)
                }

                it.tilVedtaksbegrunnelse(vedtaksperiodeMedBegrunnelser, personerGjeldendeForBegrunnelseIdenter.toList())
            }
        )

        if (begrunnelserMedFeil.isNotEmpty()) {
            throw FunksjonellFeil(
                melding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.",
                frontendFeilmelding = "Begrunnelsen passer ikke til vilkårsvurderingen. For å rette opp, gå tilbake til vilkårsvurderingen eller velg en annen begrunnelse.\n" +
                    begrunnelserMedFeil.fold("") { acc, vedtakBegrunnelseSpesifikasjon ->

                        val triggesAv =
                            vedtakBegrunnelseSpesifikasjon
                                .tilSanityBegrunnelse(brevKlient.hentSanityBegrunnelse())
                                .tilTriggesAv()
                        val tittel =
                            vedtakBegrunnelseSpesifikasjon
                                .tilSanityBegrunnelse(brevKlient.hentSanityBegrunnelse())
                                .navnISystem

                        acc + "'$tittel' forventer vurdering på '${triggesAv.vilkår.first().beskrivelse}'"
                    }
            )
        }

        if (vedtaksperiodeMedBegrunnelser.harFriteksterUtenStandardbegrunnelser()) {
            throw FunksjonellFeil(
                melding = "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                    "Legg først til en ny begrunnelse eller fjern friteksten(e).",
                frontendFeilmelding = "Fritekst kan kun brukes i kombinasjon med en eller flere begrunnelser. " +
                    "Legg først til en ny begrunnelse eller fjern friteksten(e)."
            )
        }

        lagre(vedtaksperiodeMedBegrunnelser)

        return vedtaksperiodeMedBegrunnelser.vedtak
    }

    fun oppdaterVedtaksperioderForBarnVurdertIFødselshendelse(vedtak: Vedtak, barnaSomVurderes: List<String>) {
        val vedtaksperioderMedBegrunnelser = vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)
        val persongrunnlag = persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId = vedtak.behandling.id)
            ?: error("Finner ikke persongrunnlag")
        val vurderteBarnSomPersoner =
            barnaSomVurderes.map { barnSomVurderes ->
                persongrunnlag.barna.find { it.personIdent.ident == barnSomVurderes }
                    ?: error("Finner ikke barn som har blitt vurdert i persongrunnlaget")
            }

        vurderteBarnSomPersoner.forEach { barn ->
            val vedtaksperiodeMedBegrunnelser = vedtaksperioderMedBegrunnelser.firstOrNull {
                barn.fødselsdato.toYearMonth().plusMonths(1).equals(it.fom?.toYearMonth() ?: TIDENES_ENDE)
            } ?: throw Feil("Finner ikke vedtaksperiode å begrunne for barn fra hendelse")

            vedtaksperiodeMedBegrunnelser.settBegrunnelser(
                listOf(
                    Vedtaksbegrunnelse(
                        vedtakBegrunnelseSpesifikasjon = if (vedtak.behandling.fagsak.status == FagsakStatus.LØPENDE) {
                            VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
                        } else VedtakBegrunnelseSpesifikasjon.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE,
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    )
                )
            )
            lagre(vedtaksperiodeMedBegrunnelser)
        }
    }

    fun oppdaterVedtakMedVedtaksperioder(vedtak: Vedtak) {

        slettVedtaksperioderFor(vedtak)
        if (vedtak.behandling.resultat == BehandlingResultat.FORTSATT_INNVILGET) {

            val vedtaksbrevmal = hentVedtaksbrevmal(vedtak.behandling)
            val erAutobrevFor6Og18År = vedtaksbrevmal == Brevmal.AUTOVEDTAK_BARN6_ÅR ||
                vedtaksbrevmal == Brevmal.AUTOVEDTAK_BARN18_ÅR

            val fom = if (erAutobrevFor6Og18År) {
                YearMonth.now().førsteDagIInneværendeMåned()
            } else null

            val tom = if (erAutobrevFor6Og18År) {
                finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(vedtak.behandling.id)
            } else null

            lagre(
                VedtaksperiodeMedBegrunnelser(
                    fom = fom,
                    tom = tom,
                    vedtak = vedtak,
                    type = Vedtaksperiodetype.FORTSATT_INNVILGET
                )
            )
        } else {
            lagre(genererVedtaksperioderMedBegrunnelser(vedtak))
        }
    }

    fun genererVedtaksperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        val utbetalingsperioderUtenEndringer = hentUtbetalingsperioder(vedtak.behandling) {
            it.filter { andelTilkjentYtelse -> andelTilkjentYtelse.endretUtbetalingAndeler.isEmpty() }
        }

        val utbetalingOgOpphørsperioder =
            (
                utbetalingsperioderUtenEndringer +
                    hentOpphørsperioder(vedtak.behandling)
                ).map {
                it.tilVedtaksperiodeMedBegrunnelse(vedtak)
            }
        val avslagsperioder = hentAvslagsperioderMedBegrunnelser(vedtak)

        val endretUtbetalingsperioder = hentEndredeUtbetalingsperioderMedBegrunnelser(
            vedtak = vedtak,
            endredeUtbetalingsAndeler = endretUtbetalingAndelRepository.findByBehandlingId(vedtak.behandling.id)
        )

        return utbetalingOgOpphørsperioder + avslagsperioder + endretUtbetalingsperioder
    }

    fun kopierOverVedtaksperioder(deaktivertVedtak: Vedtak, aktivtVedtak: Vedtak) {
        val gamleVedtaksperioderMedBegrunnelser =
            vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = deaktivertVedtak.id)

        gamleVedtaksperioderMedBegrunnelser.forEach { vedtaksperiodeMedBegrunnelser ->
            val nyVedtaksperiodeMedBegrunnelser = lagre(
                VedtaksperiodeMedBegrunnelser(
                    vedtak = aktivtVedtak,
                    fom = vedtaksperiodeMedBegrunnelser.fom,
                    tom = vedtaksperiodeMedBegrunnelser.tom,
                    type = vedtaksperiodeMedBegrunnelser.type,
                )
            )

            nyVedtaksperiodeMedBegrunnelser.settBegrunnelser(
                vedtaksperiodeMedBegrunnelser.begrunnelser.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )
            nyVedtaksperiodeMedBegrunnelser.settFritekster(
                vedtaksperiodeMedBegrunnelser.fritekster.map {
                    it.kopier(nyVedtaksperiodeMedBegrunnelser)
                }
            )

            lagre(nyVedtaksperiodeMedBegrunnelser)
        }
    }

    fun hentPersisterteVedtaksperioder(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {
        return vedtaksperiodeRepository.finnVedtaksperioderFor(vedtakId = vedtak.id)
    }

    fun hentUtvidetVedtaksperiodeMedBegrunnelser(vedtak: Vedtak): List<UtvidetVedtaksperiodeMedBegrunnelser> {
        val vedtaksperioderMedBegrunnelser = hentPersisterteVedtaksperioder(vedtak)

        val behandling = vedtak.behandling
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
            behandlingId = behandling.id
        )

        val persongrunnlag =
            persongrunnlagRepository.findByBehandlingAndAktiv(behandling.id) ?: error("Finner ikke persongrunnlag")

        return vedtaksperioderMedBegrunnelser.map {
            it.tilUtvidetVedtaksperiodeMedBegrunnelser(
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                personopplysningGrunnlag = persongrunnlag
            )
        }.map { utvidetVedtaksperiodeMedBegrunnelser ->
            val gyldigeBegrunnelser = when (utvidetVedtaksperiodeMedBegrunnelser.type) {
                Vedtaksperiodetype.FORTSATT_INNVILGET -> {
                    VedtakBegrunnelseSpesifikasjon.values()
                        .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
                }
                Vedtaksperiodetype.AVSLAG -> {
                    VedtakBegrunnelseSpesifikasjon.values()
                        .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.AVSLAG }
                }
                else -> {
                    val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandling.id)
                        ?: error("Finner ikke vilkårsvurdering ved begrunning av vedtak")

                    val ytelseTyper =
                        utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.ytelseType }

                    val identerMedUtbetaling =
                        utvidetVedtaksperiodeMedBegrunnelser.utbetalingsperiodeDetaljer.map { it.person.personIdent }

                    val standardbegrunnelser: MutableSet<VedtakBegrunnelseSpesifikasjon> =
                        VedtakBegrunnelseSpesifikasjon.values()
                            .filter { vedtakBegrunnelseSpesifikasjon -> vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType != VedtakBegrunnelseType.AVSLAG && vedtakBegrunnelseSpesifikasjon.vedtakBegrunnelseType != VedtakBegrunnelseType.FORTSATT_INNVILGET }
                            .fold(mutableSetOf()) { acc, standardBegrunnelse ->
                                val triggesAv =
                                    standardBegrunnelse.tilSanityBegrunnelse(brevKlient.hentSanityBegrunnelse())
                                        .tilTriggesAv()
                                val vedtakBegrunnelseType = standardBegrunnelse.vedtakBegrunnelseType

                                if (triggesAv.vilkår.contains(Vilkår.UTVIDET_BARNETRYGD) && ytelseTyper.contains(
                                        YtelseType.UTVIDET_BARNETRYGD
                                    ) &&
                                    vedtakBegrunnelseType == VedtakBegrunnelseType.INNVILGELSE
                                ) {
                                    acc.add(standardBegrunnelse)
                                } else if (standardBegrunnelse.triggesForPeriode(
                                        utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelser,
                                        vilkårsvurdering = vilkårsvurdering,
                                        persongrunnlag = persongrunnlag,
                                        identerMedUtbetaling = identerMedUtbetaling,
                                        triggesAv = triggesAv,
                                        vedtakBegrunnelseType = vedtakBegrunnelseType,
                                        endretUtbetalingAndeler = endretUtbetalingAndelRepository.findByBehandlingId(
                                                behandling.id
                                            )
                                    )
                                ) {
                                    acc.add(standardBegrunnelse)
                                }

                                acc
                            }

                    standardbegrunnelser.ifEmpty {
                        VedtakBegrunnelseSpesifikasjon.values()
                            .filter { it.vedtakBegrunnelseType == VedtakBegrunnelseType.FORTSATT_INNVILGET }
                    }
                }
            }

            utvidetVedtaksperiodeMedBegrunnelser.copy(
                gyldigeBegrunnelser = gyldigeBegrunnelser.filter {
                    it
                        .tilSanityBegrunnelse(brevKlient.hentSanityBegrunnelse())
                        .tilTriggesAv().valgbar
                }.toList()
            )
        }
    }

    fun oppdaterFortsattInnvilgetPeriodeMedAutobrevBegrunnelse(
        vedtak: Vedtak,
        vedtakBegrunnelseSpesifikasjon: VedtakBegrunnelseSpesifikasjon
    ) {
        val vedtaksperioder = hentPersisterteVedtaksperioder(vedtak)

        val fortsattInnvilgetPeriode: VedtaksperiodeMedBegrunnelser =
            vedtaksperioder.singleOrNull()
                ?: throw Feil("Finner ingen eller flere vedtaksperioder ved fortsatt innvilget")

        val personidenter =
            if (vedtakBegrunnelseSpesifikasjon == VedtakBegrunnelseSpesifikasjon.REDUKSJON_UNDER_18_ÅR) {
                val fødselsMånedOgÅrForAlder18 = YearMonth.from(LocalDate.now()).minusYears(18)
                val persongrunnlag = persongrunnlagRepository.findByBehandlingAndAktiv(vedtak.behandling.id)
                    ?: error("Fant ikke persongrunnlag for behandling ${vedtak.behandling.id}")

                persongrunnlag.barna.filter { barn ->
                    barn.fødselsdato.toYearMonth().equals(fødselsMånedOgÅrForAlder18) ||
                        barn.fødselsdato.toYearMonth().equals(fødselsMånedOgÅrForAlder18.plusMonths(1))
                }.map { it.personIdent.ident }
            } else {
                hentPersonIdenterFraUtbetalingsperioder(hentUtbetalingsperioder(vedtak.behandling))
            }

        fortsattInnvilgetPeriode.settBegrunnelser(
            listOf(
                Vedtaksbegrunnelse(
                    vedtaksperiodeMedBegrunnelser = fortsattInnvilgetPeriode,
                    vedtakBegrunnelseSpesifikasjon = vedtakBegrunnelseSpesifikasjon,
                    personIdenter = personidenter
                )
            )
        )

        lagre(fortsattInnvilgetPeriode)
    }

    fun genererBrevBegrunnelserForPeriode(vedtaksperiodeId: Long): List<Begrunnelse> {
        val vedtaksperiode = vedtaksperiodeRepository.hentVedtaksperiode(vedtaksperiodeId)

        val behandlingId = vedtaksperiode.vedtak.behandling.id
        val persongrunnlag = persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandlingId)
            ?: throw Feil("Finner ikke persongrunnlag for behandling $behandlingId")
        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = behandlingId)?.hentUregistrerteBarn() ?: emptyList()

        val utvidetVedtaksperiodeMedBegrunnelse = vedtaksperiode.tilUtvidetVedtaksperiodeMedBegrunnelser(
            personopplysningGrunnlag = persongrunnlag,
            andelerTilkjentYtelse = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId)
        )

        return byggBegrunnelserOgFriteksterForVedtaksperiode(
            utvidetVedtaksperiodeMedBegrunnelser = utvidetVedtaksperiodeMedBegrunnelse,
            personerIPersongrunnlag = persongrunnlag.personer.toList(),
            målform = persongrunnlag.søker.målform,
            uregistrerteBarn = uregistrerteBarn
        )
    }

    private fun finnTomDatoIFørsteUtbetalingsintervallFraInneværendeMåned(behandlingId: Long): LocalDate =
        andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandlinger(listOf(behandlingId))
            .filter { it.stønadFom <= YearMonth.now() && it.stønadTom >= YearMonth.now() }
            .minByOrNull { it.stønadTom }?.stønadTom?.sisteDagIInneværendeMåned()
            ?: error("Fant ikke andel for tilkjent ytelse inneværende måned for behandling $behandlingId.")

    fun hentUtbetalingsperioder(
        behandling: Behandling,
        filterAndeler: (andelerTilkjentYtelse: List<AndelTilkjentYtelse>) -> List<AndelTilkjentYtelse> = { it }
    ): List<Utbetalingsperiode> {
        val personopplysningGrunnlag = persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: return emptyList()
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilUtbetalingsperioder(
            andelerTilkjentYtelse = andelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
            filterAndeler = filterAndeler
        )
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
                persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId = forrigeIverksatteBehandling.id)
            else null
        val forrigeAndelerTilkjentYtelse: List<AndelTilkjentYtelse> =
            if (forrigeIverksatteBehandling != null) andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(
                behandlingId = forrigeIverksatteBehandling.id
            ) else emptyList()

        val personopplysningGrunnlag = persongrunnlagRepository.findByBehandlingAndAktiv(behandlingId = behandling.id)
            ?: return emptyList()
        val andelerTilkjentYtelse =
            andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        return mapTilOpphørsperioder(
            forrigePersonopplysningGrunnlag = forrigePersonopplysningGrunnlag,
            forrigeAndelerTilkjentYtelse = forrigeAndelerTilkjentYtelse,
            personopplysningGrunnlag = personopplysningGrunnlag,
            andelerTilkjentYtelse = andelerTilkjentYtelse,
        )
    }

    private fun hentAvslagsperioderMedBegrunnelser(vedtak: Vedtak): List<VedtaksperiodeMedBegrunnelser> {

        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId = vedtak.behandling.id)
            ?: throw Feil("Fant ikke vilkårsvurdering for behandling ${vedtak.behandling.id} ved generering av avslagsperioder")

        val periodegrupperteAvslagsvilkår: Map<NullablePeriode, List<VilkårResultat>> =
            vilkårsvurdering.personResultater.flatMap { it.vilkårResultater }
                .filter { it.erEksplisittAvslagPåSøknad == true }
                .groupBy { NullablePeriode(it.periodeFom, it.periodeTom) }

        val avslagsperioder = periodegrupperteAvslagsvilkår.map { (fellesPeriode, vilkårResultater) ->

            val begrunnelserMedIdenter: Map<VedtakBegrunnelseSpesifikasjon, List<String>> =
                begrunnelserMedIdentgrupper(vilkårResultater)

            VedtaksperiodeMedBegrunnelser(
                vedtak = vedtak,
                fom = fellesPeriode.fom,
                tom = fellesPeriode.tom,
                type = Vedtaksperiodetype.AVSLAG
            )
                .apply {
                    begrunnelser.addAll(
                        begrunnelserMedIdenter.map { (begrunnelse, identer) ->
                            Vedtaksbegrunnelse(
                                vedtaksperiodeMedBegrunnelser = this,
                                vedtakBegrunnelseSpesifikasjon = begrunnelse,
                                personIdenter = identer
                            )
                        }
                    )
                }
        }.toMutableList()

        val uregistrerteBarn =
            søknadGrunnlagService.hentAktiv(behandlingId = vedtak.behandling.id)?.hentUregistrerteBarn() ?: emptyList()

        return if (uregistrerteBarn.isNotEmpty()) {
            leggTilAvslagsbegrunnelseForUregistrertBarn(
                avslagsperioder = avslagsperioder,
                vedtak = vedtak,
                uregistrerteBarn = uregistrerteBarn
            )
        } else avslagsperioder
    }

    private fun leggTilAvslagsbegrunnelseForUregistrertBarn(
        avslagsperioder: List<VedtaksperiodeMedBegrunnelser>,
        vedtak: Vedtak,
        uregistrerteBarn: List<BarnMedOpplysninger>
    ): List<VedtaksperiodeMedBegrunnelser> {
        val avslagsperioderMedTomPeriode =
            if (avslagsperioder.none { it.fom == null && it.tom == null }) {
                avslagsperioder + VedtaksperiodeMedBegrunnelser(
                    vedtak = vedtak,
                    fom = null,
                    tom = null,
                    type = Vedtaksperiodetype.AVSLAG
                )
            } else avslagsperioder

        return avslagsperioderMedTomPeriode.map {
            if (it.fom == null && it.tom == null && uregistrerteBarn.isNotEmpty()) {
                it.apply {
                    begrunnelser.add(
                        Vedtaksbegrunnelse(
                            vedtaksperiodeMedBegrunnelser = this,
                            vedtakBegrunnelseSpesifikasjon = VedtakBegrunnelseSpesifikasjon.AVSLAG_UREGISTRERT_BARN,
                        )
                    )
                }
            } else it
        }.toList()
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
}
