package com.carlos.superhuman.data

/**
 * Dados iniciais dos cartões e trajetos.
 *
 * ➜ Substitui estes valores pelos mesmos do Glass Launcher (número do cartão, paragens e destinos).
 *   Também os podes editar dentro da app em Transportes → editar; estes valores só são usados na primeira instalação.
 */
object TransportDefaults {
    val cards = listOf(
        TransportCard(
            id = "card-cp",
            operator = "CP",
            title = "CP · Passe",
            number = "[preencher]",
            holder = "Carlos Pereira",
            validUntil = "[preencher]",
            colorHex = "#2E7D32",
        ),
        TransportCard(
            id = "card-carris",
            operator = "Carris",
            title = "Navegante",
            number = "[preencher]",
            holder = "Carlos Pereira",
            validUntil = "[preencher]",
            colorHex = "#F2B705",
        ),
    )

    val routes = listOf(
        TransportRoute(
            id = "route-cp-1",
            operator = "CP",
            line = "Linha [preencher]",
            from = "[origem]",
            to = "[destino]",
            departures = emptyList(),
            url = "https://www.cp.pt/passageiros/pt/consultar-horarios",
        ),
        TransportRoute(
            id = "route-carris-1",
            operator = "Carris",
            line = "[linha]",
            from = "[paragem]",
            to = "[destino]",
            departures = emptyList(),
            url = "https://www.carris.pt/viaje/horarios/",
        ),
    )
}
