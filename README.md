# Partidas — quadros de partidas do Glass TV, no Pixel 9a (app + widgets)

Os mesmos quadros do Glass Launcher (TV), adaptados ao telemóvel:

- quadro na cor da marca (verde CP / amarelo Carris com azul) com reflexo de vidro;
- coluna à esquerda: logótipo CP / Carris, título, sentido ou nº de paragens, relógio, "TEMPO REAL"
  (passa a "A ATUALIZAR…" ou "OFFLINE · hh:mm" quando é o caso);
- mosaicos à direita: minutos em destaque, hora (riscada se suprimido) + hora antiga riscada se atrasado,
  linha/carreira, destino, detalhe (tempo de viagem e nº do comboio / paragem), pílula PREVISTO · ATRASADO +n · SUPRIMIDO;
  o primeiro mosaico em destaque; na app deslizas para ver até 10;
- fonte Inter; app em cinzento escuro.

O widget é desenhado como imagem (`widget/BoardPainter.kt`) porque o launcher do Pixel ignora fontes
de apps em widgets (trocava para Roboto). A imagem é feita ao tamanho real do widget em retrato e a
grelha adapta-se: mosaicos com pelo menos ~96 dp de largura; uma segunda (e terceira) fila quando o
widget é alto; o detalhe (tempo de viagem, nº, paragem) só aparece quando cabe. Os mosaicos estão numa
grelha com scroll vertical (até 10 partidas); quando há mais, a fila seguinte fica a espreitar em baixo.
(O scroll é vertical porque o gesto lateral no ecrã principal muda de página.)
Nomes com hífen partem antes do hífen: "Alcântara" / "-Mar". O relógio é redesenhado
a cada mudança de minuto (alarme que não acorda o telefone).

## Fontes de dados (a mesma lógica do Glass TV)

| Quadro | Fonte | Configuração |
|---|---|---|
| Comboio · CP | serviços do cp.pt (fe-config.json + travel API) | estação + sentido ("Cascais" inclui todos os comboios para oeste) |
| Autocarro · Carris | paragem.alx.carris.pt (Carris Lisboa) | uma ou mais paragens; junta todos os autocarros que chegam a elas |
| Carris Metropolitana | api.carrismetropolitana.pt | uma ou mais paragens |

No primeiro arranque a app cria os dois quadros do Glass TV: **Alcântara-Mar · Sentido Cascais** e
**Alcântara** (paragens 10210, 10209, 10203, 10202, 10201).

Logótipos: `app/src/main/assets/logos/cp.png` e `carris.png` (os mesmos da TV). O da CP é pintado de branco.

## Compilar e instalar (Android Studio, Windows)

1. Descomprime o zip e em Android Studio: **File › Open** › pasta `Partidas`.
2. Espera pelo *Gradle sync* (descarrega Gradle 8.11.1, AGP 8.7.3, Kotlin 2.1.0). Se o Studio sugerir
   atualizar o AGP, podes aceitar.
3. No Pixel: Definições › Sistema › Opções de programador › Depuração USB (ou depuração sem fios).
4. **Run ▶** com o Pixel selecionado. Alternativa: **Build › Build APK(s)** e instala o
   `app/build/outputs/apk/debug/app-debug.apk`.

Requisitos: Android 12+ (minSdk 31).

## Criar a página "a um swipe"

1. Na app: os quadros do Glass TV já lá estão. Para outros: **Novo quadro** › Comboio (estação + sentido)
   ou Autocarro (paragens). A pré-visualização já mostra o quadro final.
2. No quadro, **Adicionar ao ecrã principal** — o Pixel pergunta onde o pôr.
   (Também dá pelo ecrã: manter premido › Widgets › Partidas › escolher o quadro.)
3. Arrasta o widget para a margem direita para criar uma página nova e junta lá os outros.
4. Tamanho: 4×2 → 2 mosaicos com detalhe; 4×3 → 2×2 mosaicos; widgets mais largos (tablet/paisagem) até 4 por fila.

Nota: a página mais à esquerda do Pixel Launcher é o Google Discover e não aceita widgets.

## Como se mantém atualizado

| Situação | O que acontece |
|---|---|
| Ecrã ligado | a cada minuto recalcula os "x min" (sem rede); se os dados têm >3 min, vai buscar novos |
| Ecrã desligado | atualização a cada 15 min (WorkManager), não acorda o telefone de propósito |
| Tocar em ↻ | atualiza esse cartão já |
| Tocar no widget | abre esse quadro na app |

Estado (como na TV): pílula PREVISTO, **laranja** ATRASADO +n (com a hora original riscada), **vermelha** SUPRIMIDO.

## Estrutura

```
data/TrainApi.kt    CP (cp.pt) — porto do CpRepo do Glass TV
data/BusApi.kt      CarrisApi (Carris Lisboa, porto do CarrisRepo) + BusApi (Carris Metropolitana)
data/Store.kt       quadros, ligação widget→quadro, cache por quadro, filtro
data/BoardLook.kt   cores, estados e logótipos comuns à app e aos widgets
widget/*            AppWidgetProvider, desenho RemoteViews, WorkManager, alarme de 1 min
ui/BoardView.kt     o quadro em Compose (mesmo desenho do widget)
ui/*                lista de quadros, ecrã inteiro, editor, escolha de quadro para widget
```

## Se algo falhar

- **"Sem dados"**: o cp.pt e a Carris não têm APIs oficiais documentadas e podem mudar. O erro aparece
  no quadro; manda-me a mensagem e eu ajusto o `TrainApi.kt` / `BusApi.kt` (o Glass TV usa os mesmos serviços).
- **Carris Metropolitana**: a primeira pesquisa descarrega a lista de paragens (alguns MB).
