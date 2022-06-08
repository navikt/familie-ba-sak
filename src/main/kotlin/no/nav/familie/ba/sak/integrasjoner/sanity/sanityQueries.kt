package no.nav.familie.ba.sak.integrasjoner.sanity

const val hentBegrunnelser =
    "*[_type == \"begrunnelse\" && behandlingstema != \"EØS\" && apiNavn != null && navnISystem != null]{" +
        "apiNavn," +
        "navnISystem," +
        "hjemler," +
        "hjemlerFolketrygdloven," +
        "vilkaar," +
        "rolle," +
        "lovligOppholdTriggere," +
        "bosattIRiketTriggere," +
        "giftPartnerskapTriggere," +
        "borMedSokerTriggere," +
        "ovrigeTriggere," +
        "endretUtbetalingsperiodeTriggere," +
        "endretUtbetalingsperiodeDeltBostedUtbetalingTrigger," +
        "endringsaarsaker," +
        "utvidetBarnetrygdTriggere" +
        "}"

const val hentEØSBegrunnelser =
    "*[_type == \"begrunnelse\" && behandlingstema == \"EØS\" && apiNavn != null && navnISystem != null]{" +
        "apiNavn," +
        "navnISystem," +
        "hjemler," +
        "hjemlerFolketrygdloven," +
        "hjemlerEOSForordningen883," +
        "hjemlerEOSForordningen987," +
        "hjemlerSeperasjonsavtalenStorbritannina," +
        "annenForeldersAktivitet," +
        "barnetsBostedsland," +
        "kompetanseResultat" +
        "}"
