# MGL Vision Mobile — Android

Aplicativo Android nativo com painel WebView personalizado para MGL Vision.

## Recursos
- Tela inicial com endereço/IP e porta configuráveis.
- Padrão: `https://mgltech.duckdns.org:18443`.
- Salva a conexão no dispositivo e permite trocar o servidor.
- Navegação WebView com JavaScript, cookies e armazenamento de sessão.
- Solicita permissões de câmera/microfone para páginas HTTPS do servidor configurado.
- Seletor de arquivos, atualização e botão voltar.
- Links externos abrem no navegador; certificados TLS inválidos **não** são ignorados.

## Gerar APK
1. Abra a pasta `MGLVisionMobile` no **Android Studio**.
2. Instale o Android SDK 35 quando solicitado e aguarde o Gradle sincronizar.
3. Menu **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Instale o arquivo `app/build/outputs/apk/debug/app-debug.apk` em seu Android.
5. Para distribuição, gere um APK/AAB assinado com uma chave de assinatura sua.

## Observações
- O app utiliza o painel web existente, portanto o layout precisa ser responsivo para telas pequenas.
- WebRTC e captura de mídia dependem de HTTPS válido, permissões Android, conectividade e suporte do WebView do aparelho.
- O modo HTTP é permitido apenas após aviso e não garante câmera/microfone.
- Este pacote contém **código-fonte**, não um APK já compilado.
