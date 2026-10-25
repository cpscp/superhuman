# Superhuman

App Android pessoal (Kotlin + Jetpack Compose) para o dia a dia: Dashboard, Tasks, Agenda, Finanças, Supermercado, Invest, Saúde, Notas, Casa, Objetivos, Assistente AI, Revisões e widgets.

## Compilar

1. Abre a pasta `Superhuman` no Android Studio (Ladybug ou mais recente).
2. Espera pelo *Gradle sync* (descarrega as dependências na primeira vez).
3. `Build → Build App Bundle(s) / APK(s) → Build APK(s)`.
   O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.
4. Ou corre no emulador/telemóvel com ▶.

Alternativa sem Android Studio: põe o projeto num repositório GitHub. O workflow em `.github/workflows/build.yml` compila o APK em cada push e deixa-o em *Actions → Artifacts*.

## Primeira configuração (dentro da app)

| Onde | O quê |
|---|---|
| Arranque | Aceita as permissões de calendário e notificações |
| Definições → Localização | Procura a tua cidade para o tempo |
| Definições → Calendários | Dá etiquetas ("Trabalho", "Pessoal"), escolhe o calendário padrão para slots |
| Finanças → Ligar Google Wallet | Ativa o acesso a notificações do Superhuman |
| Saúde → Ligar Health Connect | Passos, sono e treinos (Google Fit e Strava sincronizam para o Health Connect) |
| Definições → Strava (opcional) | Client ID/Secret de strava.com/settings/api, *Authorization Callback Domain* = `localhost` |
| Supermercado → ↑ | Importa o histórico do Excel em CSV (`data;supermercado;valor`) |
| Ecrã inicial do Android | Widgets "Superhuman · Hoje" e "Superhuman · Plafond e cervejas" |

## Como funciona o Invest diário

A app tenta primeiro ler um feed JSON publicado todos os dias (URL nas Definições, por defeito
`https://raw.githubusercontent.com/cpscp/superhuman-data/main/invest/latest.json`).
O feed é gerado todas as manhãs (06:34) por uma tarefa agendada do Claude, sem custos extra.
O formato do feed está em `feed-template/invest/latest.json`.

Uma tarefa agendada da Claude pode publicar esse ficheiro todas as manhãs num repositório público `cpscp/superhuman-data`.

As análises são informação, não aconselhamento financeiro personalizado.

## Estrutura

```
app/src/main/java/com/carlos/superhuman/
  data/       Modelos, armazenamento (JSON local), cálculos
  net/        Tempo (Open-Meteo), Claude API + agente, mercado (Yahoo), Strava
  device/     Calendário do telemóvel, Health Connect
  service/    Leitura de pagamentos do Google Wallet
  work/       Revisões (21:30 e sexta 18:00), brief da manhã, atualizações
  widget/     Widgets Glance
  ui/         Tema (Inter), componentes, gráficos, ecrãs
```
