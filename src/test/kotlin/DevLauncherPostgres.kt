import org.springframework.boot.builder.SpringApplicationBuilder

class DevLauncherPostgres

fun main(args: Array<String>) {
    SpringApplicationBuilder(DevLauncherPostgres::class.java).profiles(
        "postgres",
        "mock-brev-klient",
        "mock-økonomi",
        "mock-infotrygd-feed",
        "mock-infotrygd-barnetrygd",
        "mock-pdl",
        "mock-ident-client",
        "mock-tilbakekreving-klient",
        "task-scheduling"
    ).run(*args)
}
