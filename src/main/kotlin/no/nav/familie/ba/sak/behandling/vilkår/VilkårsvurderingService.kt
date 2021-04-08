package no.nav.familie.ba.sak.behandling.vilkår

import no.nav.familie.ba.sak.annenvurdering.AnnenVurderingType
import no.nav.familie.ba.sak.annenvurdering.leggTilBlankAnnenVurdering
import no.nav.familie.ba.sak.common.Feil
import no.nav.familie.ba.sak.nare.Resultat
import no.nav.familie.ba.sak.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class VilkårsvurderingService(private val vilkårsvurderingRepository: VilkårsvurderingRepository) {

    fun hentAktivForBehandling(behandlingId: Long): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
    }

    fun hentBehandlingResultatForBehandling(behandlingId: Long): List<Vilkårsvurdering> {
        return vilkårsvurderingRepository.finnBehandlingResultater(behandlingId = behandlingId)
    }

    fun finnPersonerMedEksplisittAvslagPåBehandling(behandlingId: Long): List<String> {
        val eksplisistteAvslagPåBehandling = hentEksplisitteAvslagPåBehandling(behandlingId)
        return eksplisistteAvslagPåBehandling.map { it.personResultat!!.personIdent }.distinct()
    }

    private fun hentEksplisitteAvslagPåBehandling(behandlingId: Long): List<VilkårResultat> {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingAndAktiv(behandlingId)
        return vilkårsvurdering?.personResultater?.flatMap { it.vilkårResultater }
                       ?.filter { it.erEksplisittAvslagPåSøknad ?: false } ?: emptyList()
    }

    fun oppdater(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppdaterer vilkårsvurdering $vilkårsvurdering")
        return vilkårsvurderingRepository.saveAndFlush(vilkårsvurdering)
    }

    fun lagreNyOgDeaktiverGammel(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivBehandlingResultat = hentAktivForBehandling(vilkårsvurdering.behandling.id)

        if (aktivBehandlingResultat != null) {
            vilkårsvurderingRepository.saveAndFlush(aktivBehandlingResultat.also { it.aktiv = false })
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun lagreInitielt(vilkårsvurdering: Vilkårsvurdering): Vilkårsvurdering {
        LOG.info("${SikkerhetContext.hentSaksbehandlerNavn()} oppretter vilkårsvurdering $vilkårsvurdering")

        val aktivBehandlingResultat = hentAktivForBehandling(vilkårsvurdering.behandling.id)
        if (aktivBehandlingResultat != null) {
            error("Det finnes allerede et aktivt vilkårsvurdering for behandling ${vilkårsvurdering.behandling.id}")
        }

        return vilkårsvurderingRepository.save(vilkårsvurdering)
    }

    fun opprettOglagreBlankAnnenVurdering(annenVurderingType: AnnenVurderingType, behandlingId: Long) {
        val vilkårVurdering = hentAktivForBehandling(behandlingId = behandlingId)

        if (vilkårVurdering != null) {
            vilkårVurdering.personResultater
                    .forEach { it.leggTilBlankAnnenVurdering(annenVurderingType = AnnenVurderingType.OPPLYSNINGSPLIKT) }

            oppdater(vilkårVurdering)
        }
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java)

        fun matchVilkårResultater(vilkårsvurdering1: Vilkårsvurdering,
                                  vilkårsvurdering2: Vilkårsvurdering): List<Pair<VilkårResultat?, VilkårResultat?>> {
            val vilkårResultater =
                    (vilkårsvurdering1.personResultater.map { it.vilkårResultater } + vilkårsvurdering2.personResultater.map { it.vilkårResultater }).flatten()

            data class Match(
                    val personIdent: String,
                    val vilkårType: Vilkår,
                    val resultat: Resultat,
                    val periodeFom: LocalDate?,
                    val periodeTom: LocalDate?,
                    val begrunnelse: String,
                    val erEksplisittAvslagPåSøknad: Boolean?)

            val gruppert = vilkårResultater.groupBy {
                Match(personIdent = it.personResultat?.personIdent ?: error("VilkårResultat mangler PersonResultat"),
                      vilkårType = it.vilkårType,
                      resultat = it.resultat,
                      periodeFom = it.periodeFom,
                      periodeTom = it.periodeTom,
                      begrunnelse = it.begrunnelse,
                      erEksplisittAvslagPåSøknad = it.erEksplisittAvslagPåSøknad)
            }
            return gruppert.map { (_, gruppe) ->
                if (gruppe.size > 2) throw Feil("Finnes flere like vilkår i én vilkårsvurdering")
                val vilkår1 = gruppe.find { it.personResultat!!.vilkårsvurdering == vilkårsvurdering1 }
                val vilkår2 = gruppe.find { it.personResultat!!.vilkårsvurdering == vilkårsvurdering2 }
                if (vilkår1 == null && vilkår2 == null) error("Vilkårresultater mangler tilknytning til vilkårsvurdering")
                Pair(vilkår1, vilkår2)
            }
        }
    }
}