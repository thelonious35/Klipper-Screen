# KlipperScreen Remote

Bu Android uygulaması, Raspberry Pi üzerinde çalışan KlipperScreen X11 oturumuna
noVNC üzerinden bağlanmak için hazırlanmıştır.

## Varsayılan bağlantı
- Host: klipper.local
- Port: 6080
- Yol: /vnc.html
- Otomatik bağlantı: açık
- Ekran yönü: yatay
- Tam ekran / immersive: açık
- Ekranı açık tutma: açık
- Otomatik yeniden bağlanma: açık

## Uygulama ayarları
Sağ üstteki dişli düğmesine basın:
- Pi adresi
- noVNC portu
- noVNC yolu
- VNC şifresi
- Yatay / Dikey / Otomatik yön
- noVNC resize modu
- %50 - %150 uygulama ölçeği
- Tam ekran
- Android gezinme çubuğunu gizleme
- Ekranı açık tutma
- Otomatik yeniden bağlanma

## Raspberry Pi tarafı
KlipperScreen tek başına telefona görüntü vermez. Pi üzerinde X11 oturumunu
x11vnc ile yayınlayıp noVNC/websockify ile web arayüzüne çevirmek gerekir.

Önerilen akış:

KlipperScreen (X11 :0)
       ↓
x11vnc :5900
       ↓
noVNC/websockify :6080
       ↓
Android KlipperScreen Remote

Pi tarafı hazır olduğunda uygulama açılır açılmaz bağlanır.
