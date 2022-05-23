package no.nav.familie.ba.sak.kjerne.eøs.felles.beregning

import no.nav.familie.ba.sak.kjerne.eøs.felles.PeriodeOgBarnSkjemaEntitet
import no.nav.familie.ba.sak.kjerne.eøs.felles.erLikBortsettFraBarn
import no.nav.familie.ba.sak.kjerne.eøs.felles.erLikBortsettFraBarnOgTilOgMed
import no.nav.familie.ba.sak.kjerne.eøs.felles.erLikBortsettFraTilOgMed
import no.nav.familie.ba.sak.kjerne.eøs.felles.harEkteDelmengdeAvBarna
import no.nav.familie.ba.sak.kjerne.eøs.felles.medBarnaSomForsvinnerFra
import no.nav.familie.ba.sak.kjerne.eøs.felles.periodeBlirLukketAv
import no.nav.familie.ba.sak.kjerne.eøs.felles.utenSkjemaHeretter

/**
 * Funksjon som inverterer en skjema-oppdatering,[this], som skal endre et sett av [gjeldendeSkjemaer]
 *
 * I tilfellet der oppdateringen:
 * 1. Gjelder ett gjeldende skjema
 * 2. Lukker periode på det gjeldende skjemaet, dvs til-og-med går fra <null> til en verdi, og/eller reduserer antall barn
 * så skal det lages en ny oppdatering med blankt skjema for det som ligger "utenfor" [oppdatering], dvs har
 * 1. Perioden som starter måneden etter ny til-og-med-dato, og er åpen fremover (til-og-med er <null>)
 * 2. Barnet/barna som blir fjernet
 *
 * Problemet som skal løses er at skjemaer som kun varierer i periode eller barn, slås sammen fordi de ellers er like
 * Lukking av periode eller fjerning av barn vil føre til en umiddelbar sammenslåing og nulle ut oppdateringen
 * Ved å lage den "motsatte" endringen med et tomt skjema "utenfor" det gjeldende skjemaet,
 * blir nettoeffekten at den ønskede oppdateringen oppstår, og et tomt skjema dekker området rundt
 *
 */
fun <T : PeriodeOgBarnSkjemaEntitet<T>> T.somInversOppdateringEllersNull(gjeldendeSkjemaer: Collection<T>): T? {

    val oppdatering = this

    val skjemaetDerPeriodeLukkes = gjeldendeSkjemaer.filter { gjeldende ->
        gjeldende.periodeBlirLukketAv(oppdatering) &&
            gjeldende.erLikBortsettFraTilOgMed(oppdatering)
    }.singleOrNull()

    val skjemaetDerBarnFjernes = gjeldendeSkjemaer.filter { gjeldende ->
        oppdatering.harEkteDelmengdeAvBarna(gjeldende) &&
            gjeldende.erLikBortsettFraBarn(oppdatering)
    }.singleOrNull()

    val skjemaetDerPeriodeLukkesOgBarnFjernes = gjeldendeSkjemaer.filter { gjeldende ->
        gjeldende.periodeBlirLukketAv(oppdatering) &&
            oppdatering.harEkteDelmengdeAvBarna(gjeldende) &&
            gjeldende.erLikBortsettFraBarnOgTilOgMed(oppdatering)
    }.singleOrNull()

    return when {
        skjemaetDerPeriodeLukkesOgBarnFjernes != null ->
            oppdatering.medBarnaSomForsvinnerFra(skjemaetDerPeriodeLukkesOgBarnFjernes).utenSkjemaHeretter()
        skjemaetDerBarnFjernes != null ->
            oppdatering.medBarnaSomForsvinnerFra(skjemaetDerBarnFjernes).utenSkjema()
        skjemaetDerPeriodeLukkes != null ->
            oppdatering.utenSkjemaHeretter()
        else -> null
    }
}
