<p align="center">
  <img src="src/main/resources/com/mrcdprm/dataviz/icon.png" alt="Veri Görselleştirme ikonu" width="96">
</p>

<h1 align="center">Veri Görselleştirme</h1>

<p align="center">
  <a href="README.md">English</a> | <b>Türkçe</b>
</p>

<p align="center">
  Java ve JavaFX ile yazılmış masaüstü veri görselleştirme uygulaması: CSV dosyası açın ya da tablo yapıştırın,<br>
  uygulama sütun türlerini tanısın; gruplama, toplama ve filtrelerle <b>grafiklerden oluşan bir pano</b> kurun.
</p>

<p align="center">
  <a href="https://github.com/MrcDprm/data-visualization-system/releases/latest"><b>⬇️ Windows için indir</b></a>
</p>

<p align="center">
  <img src="docs/co2.png" alt="CO₂ salımı grafiklerinden oluşan pano" width="820">
</p>

> Bu bir portfolyo projesidir. Uygulama yerelde çalışır ve verinizi hiçbir yere göndermez. Hemen denenebilmesi için uygulamayla birlikte üç gerçek açık veri seti gelir; kaynakları ve lisansları [aşağıda](#veri-kaynakları) yazıyor.

## Özellikler

**Veriyi içe aktarma**
- **CSV / TSV dosyaları** ve **Excel'den ya da bir web sayfasından kopyalanan tablolar**
- **Karakter kodlaması** (UTF-8, BOM'lu UTF-8, UTF-16, Türkçe Windows-1254), **ayırıcı** (`,` `;` sekme `|`), **başlık satırı** ve **sayı biçimi** (`1,234.56` ya da Türkçe `1.234,56`) otomatik bulunur
- **Sütun türleri** (sayı, tarih, metin) değerlerden tahmin edilir; `2024-02-19`, `19.02.2024`, `02/19/2024` ya da `2024-02` gibi tarihler tanınır. Her tahmin içe aktarma penceresinde ya da Veri sekmesinde değiştirilebilir
- Dağınık dosyalar okunur: içinde ayırıcı ve satır sonu olan tırnaklı hücreler, eksik ya da tekrar eden sütun adları, hücre sayısı eksik ya da fazla satırlar, yüzde ve para işaretleri. Uygulama hata vermek yerine uyarı gösterir

**Grafikler**
- **Çubuk, yığılmış çubuk, çizgi, alan, pasta, dağılım ve histogram**
- X ekseni, bir ya da birden fazla Y sütunu, seriler için **kırılım sütunu** ve **toplama** (toplam, ortalama, adet, en küçük, en büyük)
- Tarihleri **güne, aya, çeyreğe ya da yıla göre gruplama**
- Sıralama, **ilk N** (pastada geri kalanı "Diğer" dilimi olur), kenarları okunaklı histogram aralıkları
- Başlık ve eksen adları, renk körlüğüne uygun olan dahil **renk paletleri**
- Grafik düzenleyicide **canlı önizleme**; yeni veri setine uygun grafikler otomatik eklenir

**Pano**
- Yan yana duran, dar ya da geniş gösterilebilen, çoğaltılıp taşınabilen grafik kartları
- Bütün grafiklere uygulanan **pano filtreleri** ve tek grafiğe uygulanan **grafik filtreleri**: metin sütununda değer seçimi (aramalı), sayı ve tarihte aralık
- **Veriyi tablo olarak gösterme** ve CSV olarak kaydetme, **grafiği 2 kat çözünürlükte PNG olarak kaydetme**
- **Proje dosyaları** (`.dvz`) veri setinin yerini, sütun türlerini, filtreleri ve grafikleri saklar; proje dosyasına çift tıklayınca açılır. Veri dosyası taşındıysa uygulama yerini sorar

**Veri sekmesi**
- Verinin kendisi; seçili türe uymayan hücreler kırmızı görünür
- Sütun başına: tür, dolu ve boş hücre, farklı değer sayısı, en küçük, en büyük, ortalama ve toplam, ilk ve son tarih, en sık geçen değerler

**Güvenlik**
- Dosyalara güvenilmez: boyut, satır, sütun ve hücre uzunluğu sınırları var; proje dosyaları alan alan doğrulanır
- CSV dışa aktarma **formül enjeksiyonuna** karşı korunur: `=`, `+`, `-` ya da `@` ile başlayan metin hücreleri Excel'de formül olarak çalışamaz
- Beklenmeyen hatalarda kısa bir mesaj gösterilir, asla stack trace değil

**Masaüstü uygulaması**
- Türkçe ve İngilizce, açık ve koyu tema (AtlantaFX)
- Ayarlar ve son projeler kullanıcı klasöründe (`%APPDATA%\MrcDprm\DataVisualizer`) tutulur
- Windows kurulum dosyası, ikon, sürüm ve Hakkında penceresi

## Ekran Görüntüleri

| Başlangıç ekranı | Grafik düzenleyici |
|:---:|:---:|
| <img src="docs/start.png" alt="Örnek veri setleriyle başlangıç ekranı" width="420"> | <img src="docs/editor.png" alt="Canlı önizlemeli grafik düzenleyici" width="420"> |
| **Türkçe CSV içe aktarma** | **Veri sekmesi** |
| <img src="docs/import.png" alt="Tahmin edilen ayarlarla içe aktarma penceresi" width="420"> | <img src="docs/data.png" alt="Veri tablosu ve sütun özeti" width="420"> |
| **İstanbul barajları** | **Küresel sıcaklık** |
| <img src="docs/dams.png" alt="Baraj doluluk panosu" width="420"> | <img src="docs/temp.png" alt="Sıcaklık farkı panosu" width="420"> |

<p align="center">
  <img src="docs/co2-dark.png" alt="Koyu tema" width="620">
</p>

## Kurulum

1. [Releases](https://github.com/MrcDprm/data-visualization-system/releases/latest) sayfasından `DataVisualizer-1.0.0-Setup.exe` dosyasını indirip çalıştırın. Java kurmanız gerekmez, uygulamayla birlikte gelir.
2. Başlangıç ekranındaki üç örnek panodan birini ya da kendi CSV dosyanızı açın.
3. Panoyu `.dvz` proje dosyası olarak saklamak için **Kaydet**'i kullanın.

Windows "Uygulamalar" ayarlarından kaldırabilirsiniz; ayar klasörü silinmez, isterseniz elle silebilirsiniz.

## Veri Kaynakları

Örnek veri setleri gerçek açık verilerdir ve lisanslarına uygun şekilde uygulamaya eklenmiştir. Uygulama için kırpılıp yeniden biçimlendirildiler (notlara bakın).

| Veri seti | Kaynak | Lisans |
|---|---|---|
| Ülkelere göre CO₂ salımı (1950-2024) | [Our World in Data, CO₂ and Greenhouse Gas Emissions](https://github.com/owid/co2-data); Global Carbon Project ve Energy Institute verilerine dayanır | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) |
| Küresel sıcaklık farkı (1880-bugün) | [GISTEMP Team, NASA Goddard Institute for Space Studies, GISTEMP v4](https://data.giss.nasa.gov/gistemp/) | Kamu malı (ABD hükümeti eseri) |
| İstanbul baraj doluluk oranları (2000-2024) | [İBB Açık Veri Portalı, İstanbul Barajları Günlük Doluluk Oranları](https://data.ibb.gov.tr/dataset/istanbul-barajlari-gunluk-doluluk-oranlari), İSKİ | [İBB Açık Veri Lisansı](https://data.ibb.gov.tr/license) |

Notlar: CO₂ dosyasında sadece ülkeler (bölge toplamları olmadan) ve 11 sütun bırakıldı, değerler 4 ondalığa yuvarlandı. Sıcaklık tablosu yıl başına bir satırdan ay başına bir satıra çevrildi. Baraj dosyası Excel'den noktalı virgülle ayrılmış CSV'ye, yüzde değerlere ve Türkçe sayı ve tarih biçimine çevrildi; tarihi metin olarak girilmiş ve değeri zaten yüzde olan 130 satır düzeltildi. Bu uygulamanın bu kurumlarla bir bağı yoktur ve onlar tarafından onaylanmamıştır.

## Kullanılan Teknolojiler

- **Java**, **Maven**
- **JavaFX**: kullanıcı arayüzü ve grafikler
- **AtlantaFX** (tema), **Ikonli + Feather** (simgeler)
- **Gson**: proje dosyaları (JSON)
- **JUnit**: testler
- **jpackage** + **Inno Setup**: Windows kurulum dosyası

## Proje Yapısı

```
src/main/java/com/mrcdprm/dataviz/
├── data/      CSV okuyucu, kodlama/ayırıcı/başlık tanıma, sayı ve tarih çözme, sütunlar, istatistikler, örnek veri setleri
├── chart/     Grafik tanımı, filtreler, gruplama ve toplama motoru, histogram, dağılımda örnekleme, paletler, öneriler
├── project/   Doğrulamalı proje dosyası (JSON), açık çalışma
├── ui/        Başlangıç, içe aktarma, pano, grafik kartları ve düzenleyici, filtreler, veri sekmesi, dışa aktarma, tema, çeviriler
├── DataVizApp.java
└── Launcher.java
src/main/resources/   Stiller, ikon, Türkçe ve İngilizce metinler, örnek veri setleri
src/test/java/        JUnit testleri
installer/            Derleme betiği, Inno Setup betiği ve ikon
```

## Kaynaktan Derleme

JDK (sürümü `pom.xml` dosyasında) ve Maven gerekir.

```
mvn test          # 28 test
mvn javafx:run    # uygulamayı çalıştır
```

### Kurulum dosyasını oluşturma

[Inno Setup](https://jrsoftware.org/isinfo.php) gerekir.

```
powershell -ExecutionPolicy Bypass -File installer\build.ps1
ISCC installer\DataVisualizer.iss
```

`build.ps1` testleri çalıştırır, uygulamanın ihtiyaç duyduğu JDK modüllerini `jdeps` ile bulur ve `jpackage` ile küçültülmüş bir Java çalışma ortamıyla `dist\DataVisualizer` klasörünü oluşturur. Kurulum dosyası `installer\Output\` içinde oluşur ve `.dvz` dosya türünü o kullanıcı için kaydeder.

**Yeni sürüm çıkarırken:** `pom.xml`, `DataVizApp.VERSION`, `installer/build.ps1` ve `installer/DataVisualizer.iss` içindeki sürümü güncelleyin, iki komutu çalıştırın ve kurulum dosyasını yeni bir GitHub Release'e yükleyin.

## Öğrendiklerim

- **Gerçek dosyalar dağınıktır.** İstanbul baraj tablosunda 130 satırda tarih metin olarak yazılmış ve değer zaten yüzde olarak girilmişti; diğer bütün satırlarda değer 0-1 arası orandı. Hazırlık betiğini iki durumu da tanıyacak ve tarihlerin sırasını kontrol edecek şekilde yazdım. İçe aktarıcı da tırnakları, hücre içindeki satır sonlarını, eksik başlıkları, tekrar eden adları ve yanlış uzunluktaki satırları kaldırıyor; hata vermek yerine neyi düzelttiğini söylüyor.
- **Sormak yerine tahmin etmek.** Baytlar hatasız çözülüyorsa kodlama UTF-8, değilse Türkçe Windows-1254; ayırıcı, ilk 50 satırın çoğunda aynı sayıda sütun veren karakter; bir sütun ancak örneklerin en az %95'i öyle okunuyorsa sayı ya da tarih. `1,234`'ün bin mi yoksa bir virgül bir mi olduğuna karar vermek için bütün dosyaya bakmak gerekiyor; bu yüzden sayı biçimi dosya başına bir kez seçiliyor ve noktalı virgül Türkçe Excel çıktısı için ipucu sayılıyor. Her tahmin değiştirilebiliyor.
- **Küçük bir sorgu motoru.** Filtreler eşleşen satırların bit kümesine dönüşüyor; satırlar X değerine göre gruplanıyor (tarihler aya, çeyreğe ya da yıla yuvarlanıyor) ve her grup ile seri için toplam, adet, en küçük ve en büyük tutuluyor; böylece her toplama türü veri üzerinden tek geçişte hesaplanıyor. Sıralama, "Diğer" dilimli ilk N ve kategori, seri ve nokta sınırları grafikleri okunur ve hızlı tutuyor.
- **Okunur sayılar.** Histogram aralık genişliği 1, 2, 2,5 ya da 5 x 10^n'e yuvarlanıyor, böylece kenarlar `-0,4 – -0,3` gibi okunuyor; eksen etiketlerinde bütün eksen tek birim kullanıyor ("25 bin", "1,25 Mn"); yıllar asla binlik ayırıcıyla yazılmıyor.
- **Hiçbir dosyaya, kendi dosyama bile güvenmemek.** Proje dosyası herkesten gelebilir: bilinmeyen grafik türleri, sınır dışı sayılar, tekrar eden kimlikler, eksik sütunlar ve daha yeni sürümler ele alınıyor; testler yükleyiciye bozuk ve kötü niyetli JSON veriyor. CSV dışa aktarma formül enjeksiyonuna karşı korunuyor.
- **JavaFX ayrıntıları.** Grafikler her palet için üretilen bir `data:` stil dosyasıyla renkleniyor. Tema, diyalogları CSS'te 600 piksele sınırlıyordu ve kodla verilen değer bunu ezmedi; satır içi stil kullandım. Onay kutuları `_` karakterini klavye kısayolu işareti sayıyor; mnemonic ayrıştırmayı kapatana kadar sütun adlarındaki alt çizgiler kayboluyordu.
- **Portfolyo için bile lisans önemli.** Veriyi eklemeden önce her lisansı kontrol ettim; istedikleri kaynak gösterimini uygulamaya, README'ye ve Hakkında penceresine koydum ve dosyalarda yaptığım her değişikliği yazdım.

## Lisans

[MIT](LICENSE) © 2026 Miraç Deprem. Uygulamayla gelen veri setleri kendi lisanslarına tabidir ([Veri Kaynakları](#veri-kaynakları)).
