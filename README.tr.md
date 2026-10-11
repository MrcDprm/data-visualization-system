# Data Visualization System

[English](README.md) | **Türkçe**

Java ve JavaFX ile yazılmış masaüstü veri görselleştirme uygulaması: CSV dosyası yükleyin ya da tablo yapıştırın, uygulama sütun türlerini tanısın; gruplama, toplama ve filtrelerle grafiklerden oluşan bir pano kurun.

> Geliştirme aşamasında.

## Planlanan Özellikler (MVP)

- Ayırıcıyı, karakter kodlamasını, başlık satırını ve sütun türlerini otomatik tanıyan CSV / TSV içe aktarma (sayı, tarih, metin; `1.234,56` gibi Türkçe sayı biçimleri)
- Excel'den ya da bir web sayfasından kopyalanan tabloyu yapıştırma
- Veri önizleme tablosu ve sütun özetleri (en küçük, en büyük, ortalama, boş hücre)
- Grafik oluşturucu: çubuk, yığılmış çubuk, çizgi, alan, pasta, dağılım ve histogram
- Gruplama ve toplama (toplam, ortalama, adet, en küçük, en büyük), tarihleri güne / aya / yıla göre gruplama, filtreler
- Birden fazla grafik kartından oluşan pano; proje dosyasına kaydedilip yeniden açılır
- Grafikleri PNG olarak dışa aktarma
- Açık veri kaynaklarından hazır örnek veri setleri; kaynak ve lisans uygulamada gösterilir
- Türkçe ve İngilizce, açık ve koyu tema, Windows kurulum dosyası

## Gelecek Planları

- Excel (.xlsx) içe aktarma
- Canlı veri kaynakları (API, veritabanı)
- Panoyu PDF olarak dışa aktarma
