package no.nav.familie.ba.sak.behandling.steg

import no.nav.familie.ba.sak.behandling.BehandlingService
import no.nav.familie.ba.sak.behandling.domene.Behandling
import no.nav.familie.ba.sak.behandling.domene.BehandlingType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersonType
import no.nav.familie.ba.sak.behandling.grunnlag.personopplysninger.PersongrunnlagService
import no.nav.familie.ba.sak.behandling.grunnlag.søknad.SøknadGrunnlagService
import no.nav.familie.ba.sak.behandling.vedtak.RestVilkårsvurdering
import no.nav.familie.ba.sak.behandling.vedtak.VedtakService
import no.nav.familie.ba.sak.behandling.vilkår.SakType.Companion.hentSakType
import no.nav.familie.ba.sak.behandling.vilkår.VilkårService
import no.nav.familie.ba.sak.beregning.BeregningService
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.common.RessursUtils
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import no.nav.nare.core.evaluations.Resultat
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
        vedtakService.lagreEllerOppdaterVedtakForAktivBehandling(
                vilkårsvurdertBehandling,
                personopplysningGrunnlag,
                ansvarligSaksbehandler = SikkerhetContext.hentSaksbehandlerNavn())

        validerSteg(behandling)

        beregningService.oppdaterBehandlingMedBeregning(behandling, personopplysningGrunnlag)

        return hentNesteStegForNormalFlyt(behandling)
    }

    override fun stegType(): StegType {
        return StegType.VILKÅRSVURDERING
    }

    override fun validerSteg(behandling: Behandling) {
        if (behandling.type != BehandlingType.TEKNISK_OPPHØR) {
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
                    listeAvFeil.add("Vurderingen har ingen perioder hvor alle påkrevde vilkår er vurdert.")
                }
            }

            val barna = persongrunnlagService.hentBarna(behandling)
            barna.map { barn ->
                behandlingResultat.personResultater
                        .flatMap { it.vilkårResultater }
                        .filter { it.personResultat.personIdent == barn.personIdent.ident }
                        .forEach { vilkårResultat ->
                            if (vilkårResultat.resultat == Resultat.JA && vilkårResultat.periodeFom == null) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato} mangler fom dato.")
                            }
                            if (vilkårResultat.periodeFom != null && vilkårResultat.periodeFom.isBefore(barn.fødselsdato)) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato} har fra-og-med dato før barnets fødselsdato.")
                            }
                            if (vilkårResultat.periodeFom != null &&
                                vilkårResultat.periodeFom.isAfter(barn.fødselsdato.plusYears(18))) {
                                listeAvFeil.add("Vilkår '${vilkårResultat.vilkårType}' for barn med fødselsdato ${barn.fødselsdato} har fra-og-med dato etter barnet har fylt 18.")
                            }
                        }
            }

            if (listeAvFeil.isNotEmpty()) {
                throw Feil(message = "Validering feilet for behandling ${behandling.id}",
                           frontendFeilmelding = RessursUtils.lagFrontendMelding("Vilkårsvurderingen er ugyldig med følgende feil:",
                                                                                 listeAvFeil)
                )
            }
        }
    }
}