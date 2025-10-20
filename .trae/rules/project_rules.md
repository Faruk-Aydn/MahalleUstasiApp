
## Genel Kurallar

1. **İletişim dili:** Tüm kod içi açıklamalar, commit mesajları, pull request (PR) açıklamaları ve projeyle ilgili yazışmalar **Türkçe** olacak.
2. **Her geliştirme bir commit + açıklama:** Her mantıksal değişiklik için tek bir commit yapılacak. Commit mesajı Türkçe ve aşağıdaki şablona uygun olacak. PR açıklamasında *ne* değiştirildiği ve *neden* yapıldığı kısa olarak yazılacak.
3. **Branch stratejisi:** `main` (veya `master`) her zaman deploy edilebilir durumda olacak. Yeni özellikler için `feature/<isim>`, hatalar için `bugfix/<isim>`, acil düzeltmeler için `hotfix/<isim>` kullan.
4. **Kod incelemesi:** Hiçbir PR doğrudan merge edilmeyecek — en az 1 başka geliştiricinin onayı şart.
5. **Otomatik kalite kontrolleri:** CI pipeline'ında `ktlint`, `detekt`, `unit tests` ve `build` adımları zorunlu.

---

## Commit Mesajı Şablonu (Zorunlu)

Her commit mesajı aşağıdaki formatta olmalıdır (Türkçe):

```
<kısa-etiket>: <özet (imperatif kip, 50 karakteri geçmesin>)

Detay: <Değişikliğin kısa açıklaması — ne yapıldı>
Neden: <Bu değişikliğin nedeni — problem veya amaç>
```

**Örnek:**

```
feat: Kullanıcı giriş ViewModel eklendi

Detay: Giriş ekranı için LoginViewModel, repository arayüzü ve temel validasyon eklendi.
Neden: MVVM'de iş mantığını ViewModel'e taşıyarak UI testlerini ve yeniden kullanılabilirliği artırmak.
```

PR açıklamasında ayrıca yapılacak değişikliklerin kısa bir "nasıl test edileceği" bölümü (manual test adımları veya test senaryoları) eklenmelidir.

---

## MVVM Mimarisi Kuralları (Detaylı)

Proje **domain**, **data**, **presentation** katmanlarına ayrılacaktır. Her katmanın sorumlulukları ve kuralları aşağıdadır.

### 1) Katman yapısı (paketleme)

* `domain`:

  * Use case / interactor sınıfları (tek bir iş sorumluluğu).
  * Saf Kotlin, platforma bağlılıklardan arındırılmış.
  * Domain modelleri.
  * Repository arayüzleri.

* `data`:

  * Repository arayüzlerinin implementasyonları.
  * Network (API) ve local (Room, DataStore vb.) veri kaynakları.
  * DTO <-> Domain model mapper'ları.
  * Veri katmanına ait testler.

* `presentation`:

  * View (Activity/Fragment/Compose) ve ViewModel.
  * UI State modelleri (immutable data class).
  * UI event/intent yönetimi.
  * UI-only mapper'lar.

### 2) ViewModel (Sorumluluklar)

* UI ile ilgili tüm iş mantığı ViewModel içinde olacak.
* ViewModel:

  * Durumları `StateFlow` veya `LiveData` ile dışarıya sunar (tercih `StateFlow`/`SharedFlow` modern yaklaşım).
  * UI event'leri `Channel` veya `SharedFlow` ile yayınlar.
  * Repository (domain/usecase) arayüzünü constructor ile alır (Dependency Injection ile).
  * CoroutineScope olarak `viewModelScope` kullanılacak; hiçbir zaman GlobalScope kullanılmayacak.

### 3) Repository & Use Case

* Repository bir arayüz tanımlar; data katmanı bu arayüzü implement eder.
* Business logic (birkaç repository çağrısının kombinasyonu vb.) mümkünse `UseCase` sınıflarında toplanır.
* Repository, ağ ve yerel veri kaynakları arasındaki öncelik ve cache stratejisini kapsar.

### 4) UI State & Events

* UI için tek bir `UiState` data class'ı kullan; tüm ekran durumları (yükleniyor, hata, içerik) burada tanımlı olsun.
* Tekrarlı ve mutable state'ler yerine immutable data class'lar ve kopyalama (`copy()`) kullan.
* UI olaylarını (buton tıklaması, kaydırma, yeniden dene vb.) intent/ucase şeklinde ViewModel'e gönder.

### 5) Mapping & DTO'lar

* Network ve DB modelleri Domain modellerine mapper sınıfları ile çevrilecek.
* `data` paketinde mapper fonksiyonları merkezileştirilecek (extension function veya mapper sınıfı).

### 6) Hata Yönetimi

* Network hataları ve veri hataları `Result`/`Either`/special sealed class ile temsil edilecek.
* ViewModel hatayı kullanıcıya okunabilir şekilde çevirecek (kullanıcıya verilecek mesajlar string kaynaklardan alınacak).

### 7) Testler

* Domain ve ViewModel seviyesinde birim testleri yazılacak.
* Repository implementasyonları için integration/isolated testler eklenmeli (Fake data source kullanımı).

---

## Vibe Coding Kuralları (Kod Stili ve Geliştirme Etiği)

*Vibe Coding* burada şu pratikleri kapsar:

1. **Okunabilirlik öncelikli:** Değişken/işlev isimleri açıklayıcı olacak. Kısaltmalardan kaçın.
2. **Küçük, anlamlı commitler:** Her commit tek bir fikri taşır.
3. **Fonksiyon boyutu:** Fonksiyonlar mümkün olduğunca kısa ve tek sorumluluklu olacak.
4. **Null-safety & immutability:** `val` tercih edilir, nullable yapıların kullanımına dikkat.
5. **Asenkron programlama:** Coroutines + Flow tercih edilir. Blocking çağrı UI thread'inde olmamalı.
6. **Dependency Injection:** Hilt/Dagger veya tercih edilen DI kütüphanesi kullanılacak.
7. **Lint & Formatting:** `ktlint` ve proje kod formatı zorunlu; commit öncesi otomatik format denetimi önerilir.
8. **Dokümantasyon:** Public API'ler ve karmaşık akışlar kısa KDoc ile belgelenmeli.

---

## CI & Kod Kalitesi

* `ktlint` ve `detekt` pipeline'da çalışacak.
* Unit test coverage minimum bir sayı gerektirebilir (ekstra kurallara bağlı olarak tanımlanır).
* Ana dal (main) sadece başarılı CI sonucu sonrası merge edilecek.

---

## PR (Pull Request) Şablonu (Kısa)

* **Başlık (Türkçe):** feat/bugfix/refactor: kısa özet
* **Açıklama:** Ne yapıldı? Neden? Nasıl test edilir?
* **Commit mesajları:** Yukarıdaki şablona uygun olmalı.
* **İlgili issue:** (varsa) referans
