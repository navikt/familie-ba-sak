package no.nav.familie.ba.sak.integrasjoner.sanity

import no.nav.familie.ba.sak.kjerne.brev.domene.ISanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityBegrunnelse
import no.nav.familie.ba.sak.kjerne.brev.domene.SanityEØSBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.EØSStandardbegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.IVedtakBegrunnelse
import no.nav.familie.ba.sak.kjerne.vedtak.begrunnelser.Standardbegrunnelse
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class SanityService(
    private val sanityKlient: SanityKlient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private var sanityBegrunnelseCache: List<SanityBegrunnelse> = emptyList()
    private var sanityEØSBegrunnelseCache: List<SanityEØSBegrunnelse> = emptyList()

    @Cacheable("sanityBegrunnelser", cacheManager = "shortCache")
    fun hentSanityBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk: Boolean = false): Map<Standardbegrunnelse, SanityBegrunnelse> {
        val enumPåApiNavn = Standardbegrunnelse.entries.associateBy { it.sanityApiNavn }

        val sanityBegrunnelser =
            hentBegrunnelserMedCache(
                hentBegrunnelser = { sanityKlient.hentBegrunnelser() },
                cache = sanityBegrunnelseCache,
            ) { oppdatertCache -> sanityBegrunnelseCache = oppdatertCache }

        logManglerSanityBegrunnelseForEnum(enumPåApiNavn, sanityBegrunnelser, "SanityBegrunnelse")

        val mapAvSanityBegrunnelser =
            sanityBegrunnelser
                .mapNotNull {
                    val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                    if (begrunnelseEnum == null) {
                        logger.warn("Finner ikke Standardbegrunnelse for ${it.apiNavn}")
                        null
                    } else {
                        begrunnelseEnum to it
                    }
                }.toMap()

        return when (filtrerBortBegrunnelserSomIkkeErIBruk) {
            true -> mapAvSanityBegrunnelser.filterValues { !it.ikkeIBruk }
            false -> mapAvSanityBegrunnelser
        }
    }

    @Cacheable("sanityEØSBegrunnelser", cacheManager = "shortCache")
    fun hentSanityEØSBegrunnelser(filtrerBortBegrunnelserSomIkkeErIBruk: Boolean = false): Map<EØSStandardbegrunnelse, SanityEØSBegrunnelse> {
        val enumPåApiNavn = EØSStandardbegrunnelse.entries.associateBy { it.sanityApiNavn }

        val sanityEØSBegrunnelser =
            hentBegrunnelserMedCache(
                hentBegrunnelser = { sanityKlient.hentEØSBegrunnelser() },
                cache = sanityEØSBegrunnelseCache,
            ) { oppdatertCache -> sanityEØSBegrunnelseCache = oppdatertCache }

        logManglerSanityBegrunnelseForEnum(enumPåApiNavn, sanityEØSBegrunnelser, "SanityEØSBegrunnelse")

        val mapAvSanityEøsBegrunnelser =
            sanityEØSBegrunnelser
                .mapNotNull {
                    val begrunnelseEnum = enumPåApiNavn[it.apiNavn]
                    if (begrunnelseEnum == null) {
                        logger.warn("Finner ikke EØSStandardbegrunnelse for ${it.apiNavn}")
                        null
                    } else {
                        begrunnelseEnum to it
                    }
                }.toMap()

        return when (filtrerBortBegrunnelserSomIkkeErIBruk) {
            true -> mapAvSanityEøsBegrunnelser.filterValues { !it.ikkeIBruk }
            false -> mapAvSanityEøsBegrunnelser
        }
    }

    private fun logManglerSanityBegrunnelseForEnum(
        enumPåApiNavn: Map<String, IVedtakBegrunnelse>,
        sanityBegrunnelser: List<ISanityBegrunnelse>,
        navn: String,
    ) {
        val sanityBegrunnelseApiNavn = sanityBegrunnelser.map { it.apiNavn }.toSet()
        enumPåApiNavn.values.forEach {
            if (!sanityBegrunnelseApiNavn.contains(it.sanityApiNavn)) {
                logger.warn("Finner ikke $navn for enum=$it sanityApiNavn=${it.sanityApiNavn}")
            }
        }
    }

    private fun <T> hentBegrunnelserMedCache(
        hentBegrunnelser: () -> List<T>,
        cache: List<T>,
        oppdaterCache: (List<T>) -> Unit,
    ): List<T> =
        try {
            hentBegrunnelser().also { oppdaterCache(it) }
        } catch (e: Exception) {
            if (cache.isEmpty()) {
                throw e
            }
            logger.warn("Kunne ikke hente begrunnelser fra Sanity, bruker siste cachet begrunnelser", e)
            cache
        }
}
