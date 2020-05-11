package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.SakType.Companion.hentSakType
import no.nav.familie.ba.sak.behandling.vilkår.Vilkår
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class Vilkårsvurdering(
        private val behandlingService: BehandlingService,
        private val søknadGrunnlagService: SøknadGrunnlagService,
        private val vilkårService: VilkårService,
        private val vedtakService: VedtakService,
        private val beregningService: BeregningService,
        private val persongrunnlagService: PersongrunnlagService
) : BehandlingSteg<RestVilkårsvurdering> {

    private val LOG = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførStegOgAngiNeste(behandling: Behandling,
                                      data: RestVilkårsvurdering,
                                      stegService: StegService?): StegType {
        val personopplysningGrunnlag = persongrunnlagService.hentAktiv(behandling.id)
                                       ?: error("Fant ikke personopplysninggrunnlag på behandling ${behandling.id}")

        val vilkårsvurdertBehandling = behandlingService.hent(behandlingId = behandling.id)

        if (data.personResultater.isNotEmpty()) {
            vilkårService.lagBehandlingResultatFraRestPersonResultater(data.personResultater,
                                                                       vilkårsvurdertBehandling.id)
        } else {
            vilkårService.vurderVilkårForFødselshendelse(vilkårsvurdertBehandling.id)
        }
        val vedtak = vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                vilkårsvurdertBehandling,
                personopplysningGrunnlag,
                ansvarligSaksbehandler = SikkerhetContext.hentSaksbehandlerNavn())

        if (!validerSteg(behandling)) {
            error("Vilkårsvurderingen er ikke gyldig.")
        }

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)
        vedtakService.oppdaterVedtakMedStønadsbrev(vedtak)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun validerSteg(behandling: Behandling): Boolean {
        val behandlingResultat = vilkårService.hentVilkårsvurdering(behandlingId = behandling.id)
                                 ?: error("Finner ikke vilkårsvurdering på behandling ved validering.")

        val listeAvFeil = mutableListOf<String>()

        val periodeResultater = behandlingResultat.periodeResultater(brukMåned = false)

        val søknadDTO = søknadGrunnlagService.hentAktiv(behandlingId = behandling.id)?.hentSøknadDto()
        val sakType = hentSakType(behandlingKategori = behandling.kategori, søknadDTO = søknadDTO)



        val harGyldigePerioder = periodeResultater.any { periodeResultat ->
            periodeResultat.allePåkrevdeVilkårVurdert(PersonType.SØKER,
                                                      sakType) &&
            periodeResultat.allePåkrevdeVilkårVurdert(PersonType.BARN,
                                                      sakType)
        }

        when {
            !harGyldigePerioder -> {
                listeAvFeil.add("Vurderingen mangler en eller flere påkrevde vilkår")
            }
        }

        val under18ÅrVilkår =
                behandlingResultat.personResultater
                        .flatMap { it.vilkårResultater }
                        .filter { it.vilkårType == Vilkår.UNDER_18_ÅR }

        val barna = persongrunnlagService.hentBarna(behandling)
        barna.map { barn ->
            under18ÅrVilkår.forEach {
                if (it.periodeFom == null && it.periodeTom == null) {
                    listeAvFeil.add("18 års vilkår for barn med fødselsdato ${barn.fødselsdato} mangler fom og tom dato")
                }
                if (it.periodeFom == null) {
                    listeAvFeil.add("18 års vilkår for barn med fødselsdato ${barn.fødselsdato} mangler fom")
                }
                if (it.periodeTom == null) {
                    listeAvFeil.add("18 års vilkår for barn med fødselsdato ${barn.fødselsdato} mangler tom")
                }
                if (it.periodeFom != null && it.periodeTom != null) {
                    if (it.periodeFom.isBefore(barn.fødselsdato) || it.periodeFom.isAfter(barn.fødselsdato.plusYears(18))) {
                        listeAvFeil.add("18 års vilkår for barn med fødselsdato ${barn.fødselsdato} har ugyldig fom(${it.periodeFom})")
                    }
                    if (it.periodeTom.isBefore(barn.fødselsdato) || it.periodeTom.isAfter(barn.fødselsdato.plusYears(18))) {
                        listeAvFeil.add("18 års vilkår for barn med fødselsdato ${barn.fødselsdato} har ugyldig tom(${it.periodeTom})")
                    }
                }
            }
        }

        when {
            listeAvFeil.isNotEmpty() -> {
                LOG.info("Validering feilet for behandling ${behandling.id} med følgende feilmeldinger:\n${listeAvFeil.joinToString { "\n" }}")
            }
            else -> {
                LOG.info("Validert vilkårsvurdering for behandling ${behandling.id} OK")
            }
        }

        return listeAvFeil.size == 0
    }
}