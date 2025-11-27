package no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse

import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.secureLogger
import no.nav.familie.ba.sak.common.toYearMonth
import no.nav.familie.ba.sak.common.validerBehandlingIkkeErAvsluttet
import no.nav.familie.ba.sak.kjerne.behandling.domene.Behandling
import no.nav.familie.ba.sak.kjerne.beregning.domene.AndelTilkjentYtelseRepository
import no.nav.familie.ba.sak.kjerne.fagsak.FagsakStatus
import no.nav.familie.ba.sak.kjerne.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.kjerne.personident.PersonidentService
import no.nav.familie.ba.sak.kjerne.vedtak.VedtakService
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.domene.Vedtaksbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.vedtaksperiode.VedtaksperiodeHentOgPersisterService
import no.nav.familie.ba.sak.kjerne.vilkårsvurdering.VilkårsvurderingService
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class AutovedtakFødselshendelseBegrunnelseService(
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val persongrunnlagService: PersongrunnlagService,
    private val personidentService: PersonidentService,
    private val vedtaksperiodeHentOgPersisterService: VedtaksperiodeHentOgPersisterService,
    private val vedtakService: VedtakService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {
    fun begrunnVedtaksperioderForBarnVurdertIFødselshendelse(
        behandling: Behandling,
        barnaSomVurderes: List<String>,
    ) {
        validerBehandlingIkkeErAvsluttet(behandling)

        val barnaAktørSomVurderes = personidentService.hentAktørIder(barnaSomVurderes)
        val vedtak = vedtakService.hentAktivForBehandlingThrows(behandling.id)
        val vedtaksperioder = vedtaksperiodeHentOgPersisterService.finnVedtaksperioderFor(vedtakId = vedtak.id)
        val persongrunnlag = persongrunnlagService.hentAktivThrows(behandlingId = behandling.id)
        val andelerIBehandling = andelTilkjentYtelseRepository.finnAndelerTilkjentYtelseForBehandling(behandlingId = behandling.id)

        val vurderteBarnSomPersoner =
            barnaAktørSomVurderes.map { aktør ->
                persongrunnlag.barna.find { it.aktør == aktør } ?: throw Feil("Finner ikke barn som har blitt vurdert i persongrunnlaget")
            }

        val fødselsmåneder = vurderteBarnSomPersoner.map { it.fødselsdato.toYearMonth() }.distinct()

        val innvilgelsesBegrunnelseForFødselshendelse =
            if (behandling.fagsak.status == FagsakStatus.LØPENDE) {
                Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN
            } else {
                Standardbegrunnelse.INNVILGET_FØDSELSHENDELSE_NYFØDT_BARN_FØRSTE
            }

        fødselsmåneder.forEach { fødselsmåned ->
            val månedenEtterFødsel = fødselsmåned.plusMonths(1)

            val vedtaksperiodeMedBegrunnelser =
                vedtaksperioder.find {
                    it.fom?.toYearMonth() == månedenEtterFødsel
                } ?: run {
                    val vilkårsvurdering = vilkårsvurderingService.hentAktivForBehandling(behandlingId = behandling.id)
                    secureLogger.info(
                        vilkårsvurdering?.personResultater?.joinToString("\n") {
                            "Fødselsnummer: ${it.aktør.aktivFødselsnummer()}.  Resultater: ${it.vilkårResultater}"
                        } ?: "Ingen vilkårsvurdering funnet",
                    )
                    throw Feil("Finner ikke vedtaksperiode å begrunne for barn fra hendelse")
                }

            val begrunnelser =
                mutableListOf(
                    Vedtaksbegrunnelse(
                        standardbegrunnelse = innvilgelsesBegrunnelseForFødselshendelse,
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    ),
                )

            val harFinnmarkstilleggNesteMåned = andelerIBehandling.any { it.erFinnmarkstillegg() && it.stønadFom == månedenEtterFødsel }
            if (harFinnmarkstilleggNesteMåned) {
                begrunnelser +=
                    Vedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_FINNMARKSTILLEGG_UTEN_DATO,
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    )
            }

            val harSvalbardtilleggNesteMåned = andelerIBehandling.any { it.erSvalbardtillegg() && it.stønadFom == månedenEtterFødsel }
            if (harSvalbardtilleggNesteMåned) {
                begrunnelser +=
                    Vedtaksbegrunnelse(
                        standardbegrunnelse = Standardbegrunnelse.INNVILGET_SVALBARDTILLEGG_UTEN_DATO,
                        vedtaksperiodeMedBegrunnelser = vedtaksperiodeMedBegrunnelser,
                    )
            }

            vedtaksperiodeMedBegrunnelser.settBegrunnelser(begrunnelser)
            vedtaksperiodeHentOgPersisterService.lagre(vedtaksperiodeMedBegrunnelser)

            /*
             * Hvis barn(a) er født før desember påvirkes vedtaket av satsendring januar 2022
             * og vi må derfor også automatisk begrunne satsendringen
             */
            if (fødselsmåned <
                YearMonth.of(
                    2021,
                    12,
                )
            ) {
                vedtaksperioder
                    .firstOrNull { it.fom?.toYearMonth() == YearMonth.of(2022, 1) }
                    ?.also { satsendringsvedtaksperiode ->
                        satsendringsvedtaksperiode.settBegrunnelser(
                            listOf(
                                Vedtaksbegrunnelse(
                                    standardbegrunnelse = Standardbegrunnelse.INNVILGET_SATSENDRING,
                                    vedtaksperiodeMedBegrunnelser = satsendringsvedtaksperiode,
                                ),
                            ),
                        )
                        vedtaksperiodeHentOgPersisterService.lagre(satsendringsvedtaksperiode)
                    }
            }
        }
    }
}
