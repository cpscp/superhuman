# Painel: nada de reflexão; as regras por omissão do Android chegam.
-keep class pt.carlos.painel.widgets.PainelHostView { *; }
# Google Home APIs: os tipos e traits são resolvidos por fábricas registadas.
-keep class com.google.home.** { *; }
-keep class com.google.android.gms.home.** { *; }
-keep class com.google.nest.** { *; }
-dontwarn com.google.home.**
-dontwarn com.google.nest.**
-dontwarn com.google.android.gms.home.**
