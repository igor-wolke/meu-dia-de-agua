# Meu Dia de Água — projeto Android

Configuração inicial:
- Último dia informado com abastecimento: 12/11/2026
- Ciclo: dia sim, dia não (intervalo de 2 dias)
- Aviso: 07:00
- Funciona sem internet depois de instalado.

## Como compilar
1. Abra a pasta do projeto no Android Studio.
2. Aguarde o Gradle sincronizar (a primeira compilação precisa de internet para baixar as ferramentas).
3. Escolha Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Instale o APK no seu Android.
5. Permita notificações e permita alarmes exatos quando o Android solicitar.

## Uso
- "Chegou água hoje — reiniciar ciclo" marca hoje como dia de água e reinicia o ciclo de 48 horas.
- "Não chegou água hoje" registra a falha sem alterar automaticamente a previsão.
- "Reprogramar a próxima data de água" permite escolher a nova data prevista; o ciclo de 48 horas passa a contar dessa data.
- "Corrigir a última data de abastecimento" permite corrigir o ciclo.
- "Testar alarme agora" testa a notificação e o som.
- O app programa um alarme diário às 07:00; só mostra o aviso nos dias calculados como dia de água.

## Observação importante
O Android pode aplicar restrições de bateria específicas do fabricante. Para maior confiabilidade, permita notificações e alarmes exatos e não restrinja a bateria do aplicativo. O alarme usa o som padrão de alarme configurado no telefone.


## Compilar online pelo GitHub Actions (sem Android Studio)
1. Crie uma conta no GitHub e um repositório novo.
2. Envie todos os arquivos e pastas deste projeto para o repositório (incluindo `.github/workflows/android.yml`).
3. Abra a aba **Actions** do repositório.
4. Se aparecer a opção, confirme **I understand my workflows, go ahead and enable them**.
5. Escolha **Build Android APK** e clique em **Run workflow**. Também compila automaticamente a cada envio para a branch `main`.
6. Quando terminar com sucesso, abra a execução e baixe **meu-dia-de-agua-apk** em **Artifacts**.
7. Extraia o ZIP baixado e instale `app-debug.apk` no Android.

A compilação na nuvem requer internet e uma conta GitHub. O aplicativo instalado foi projetado para funcionar offline.
