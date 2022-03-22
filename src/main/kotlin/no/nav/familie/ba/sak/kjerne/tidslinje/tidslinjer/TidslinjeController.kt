package no.nav.familie.ba.sak.kjerne.tidslinje.tidslinjer

import no.nav.familie.ba.sak.kjerne.autovedtak.fødselshendelse.Resultat
import no.nav.familie.ba.sak.kjerne.tidslinje.tid.Tidspunkt
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tidslinjer")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class TidslinjeController(
    private val tidslinjeService: TidslinjeService
) {

    @GetMapping("/{behandlingId}")
    @Unprotected
    fun hentTidslinjer(@PathVariable behandlingId: Long): RestTidslinjer {
        val tidslinjer = tidslinjeService.hentTidslinjer(behandlingId)

        val barnasTidslinjer = tidslinjer.barnasTidslinjer()
        val restTidslinjer = RestTidslinjer(
            tidslinjer = barnasTidslinjer.entries.associate {
                it.key.aktivFødselsnummer() to RestTidslinjerForBarn(
                    vilkårTidslinjer = it.value.barnetsVilkårsresultatTidslinjer.map { vilkårsresultatTidslinje ->
                        vilkårsresultatTidslinje.perioder().map { periode ->
                            RestTidslinjePeriode(
                                fraOgMed = periode.fraOgMed,
                                tilOgMed = periode.tilOgMed,
                                innhold = periode.innhold!!
                            )
                        }
                    },
                    oppfyllerVilkårTidslinje = it.value.barnetIKombinasjonMedSøkerOppfyllerVilkårTidslinje.perioder()
                        .map { periode ->
                            RestTidslinjePeriode(
                                fraOgMed = periode.fraOgMed,
                                tilOgMed = periode.tilOgMed,
                                innhold = periode.innhold!!
                            )
                        },
                    erEøsPeriodeTidslinje = it.value.erEøsTidslinje.perioder().map { periode ->
                        RestTidslinjePeriode(
                            fraOgMed = periode.fraOgMed,
                            tilOgMed = periode.tilOgMed,
                            innhold = periode.innhold!!
                        )
                    },
                )
            }
        )

        return restTidslinjer
    }
}

data class RestTidslinjer(
    val tidslinjer: Map<String, RestTidslinjerForBarn>
)

data class RestTidslinjerForBarn(
    val vilkårTidslinjer: List<List<RestTidslinjePeriode<VilkårRegelverkResultat>>>,
    val oppfyllerVilkårTidslinje: List<RestTidslinjePeriode<Resultat>>,
    val erEøsPeriodeTidslinje: List<RestTidslinjePeriode<Boolean>>
)

data class RestTidslinjePeriode<T>(
    val fraOgMed: Tidspunkt,
    val tilOgMed: Tidspunkt,
    val innhold: T
)