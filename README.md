# Gerçek Zamanlı Kredi Kartı Dolandırıcılık Tespit Servisi

Bankacılık işlemlerinde gerçek zamanlı dolandırıcılık tespiti için üretim ortamına hazır REST API servisi. Bu servis, modüler kural tabanlı algoritmalar kullanarak kredi kartı işlemlerini analiz eder ve potansiyel dolandırıcılık faaliyetlerini tespit eder.

## Teknoloji Yığını

- **Java 17**
- **Spring Boot 3.2.0**
- **Maven**
- **REST API**
- **Spring Data JPA** - Veritabanı erişimi
- **PostgreSQL 15** - İlişkisel veritabanı
- **OpenAPI/Swagger** - API dokümantasyonu
- **Jakarta Validation** - İstek doğrulama
- **Micrometer/Prometheus** - Metrikler

## Mimari

Proje, **modüler ve genişletilebilir mimari** prensiplerini takip eder:

```
com.frauddetection
├── controller/          # REST API endpoints (HTTP katmanı)
├── service/            
│   ├── FraudDetectionService      # Orchestrator (koordinasyon)
│   ├── RiskScoreCalculator        # Risk skoru hesaplama
│   ├── TransactionMapper          # DTO-Domain dönüştürücü
│   └── rules/                     # Fraud kuralları (Strategy Pattern)
│       ├── FraudRule              # Interface
│       ├── TimeOfDayRule          # Gün saati kuralı
│       ├── AmountRule             # Tutar kuralı
│       └── FrequencyRule          # Sıklık kuralı
├── domain/            
│   ├── Transaction, FraudDecision  # Domain modelleri (iş mantığı)
│   └── entity/                    # JPA Entity (persistence)
│       └── TransactionEntity       # Veritabanı entity
├── dto/                # Data Transfer Objects (API kontratları)
├── repository/         # JPA Repository interfaces
├── config/             # Yapılandırma sınıfları
└── exception/          # Global exception handling
```

## Modüler Mimari Özellikleri

### Strategy Pattern ile Kural Tabanlı Sistem

Her dolandırıcılık tespit kuralı bağımsız bir strateji olarak uygulanır:

- **FraudRule Interface**: Tüm kuralların uyguladığı arayüz
- **Bağımsız Kurallar**: Her kural kendi risk katkısını hesaplar
- **Kolay Genişletilebilirlik**: Yeni kural eklemek için sadece FraudRule implement etmek yeterli
- **Açıklanabilir Kararlar**: Her kural kendi risk sebebini üretir

### Mevcut Kurallar

1. **TimeOfDayRule** - Gün Saati Kuralı
   - 00:00-05:00 arası işlemler şüpheli
   - Risk katkısı: %30 ağırlık
   - Config'den saat aralığı yapılandırılabilir

2. **AmountRule** - Tutar Kuralı
   - Yüksek tutarlı işlemler (>10,000) riski artırır
   - Çok yüksek tutarlı işlemler (>50,000) son derece risklidir
   - Risk katkısı: %35 ağırlık
   - Doğrusal ölçekleme ile esnek hesaplama

3. **FrequencyRule** - Sıklık Kuralı
   - Aynı karttan 15 dakika içinde 3+ işlem = yüksek risk
   - Risk katkısı: %35 ağırlık
   - In-memory çözüm (üretimde Redis/Hazelcast önerilir)

### Risk Hesaplama

**RiskScoreCalculator** servisi:
- Tüm kuralları Spring ile otomatik toplar
- Her kuralın ağırlıklı risk skorunu hesaplar
- Nihai risk skorunu 0.0-1.0 aralığına normalize eder
- Her kuralın katkısını loglar

### Karar Mantığı

Risk skorları 0.0 ile 1.0 arasında hesaplanır:

- **Risk Skoru < 0.4** → `APPROVE` (İşlem onaylandı)
- **Risk Skoru 0.4 - 0.7** → `REVIEW` (Manuel inceleme gerektirir)
- **Risk Skoru > 0.7** → `BLOCK` (İşlem engellendi)

## Veritabanı

### PostgreSQL Schema

Proje **PostgreSQL 15** kullanır. İşlem verileri `transactions` tablosunda saklanır:

```sql
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    fraudulent BOOLEAN NOT NULL,
    triggered_rule VARCHAR(500),
    risk_score NUMERIC(5,4),
    decision VARCHAR(20),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_account_id ON transactions(account_id);
CREATE INDEX idx_timestamp ON transactions(timestamp);
CREATE INDEX idx_fraudulent ON transactions(fraudulent);
```

### Mimari Yaklaşım: Rules + Persistence Separation

Proje, **iş mantığı (fraud rules) ve persistence katmanını** birbirinden ayırır:

- **Domain Model (`Transaction`)**: İş mantığı için kullanılır, fraud detection kuralları bu model üzerinde çalışır
- **Entity (`TransactionEntity`)**: Sadece veritabanı persistence için kullanılır
- **TransactionPersistenceService**: Domain → Entity dönüşümünü yönetir
- **FraudDetectionService**: Fraud analizi tamamlandıktan sonra persistence katmanına kayıt yapar

Bu yaklaşım sayesinde:
- Fraud detection kuralları veritabanı bağımlılığından bağımsızdır
- Test edilebilirlik artar (persistence optional injection)
- Persistence hataları fraud detection'ı etkilemez

## API Endpoint'leri

### POST /api/fraud/check

Bir işlemi analiz eder ve dolandırıcılık risk değerlendirmesi döner.

**İstek Gövdesi:**
```json
{
  "transactionId": "TXN-123456",
  "cardNumber": "4111111111111111",
  "amount": 15000.00,
  "merchantName": "Online Store",
  "merchantCategory": "E-COMMERCE",
  "transactionTimestamp": "2024-12-27T02:30:00",
  "currency": "USD",
  "cardHolderName": "John Doe"
}
```

**Yanıt:**
```json
{
  "transactionId": "TXN-123456",
  "decision": "REVIEW",
  "riskScore": 0.6250,
  "message": "İşlem manuel inceleme gerektirir",
  "analyzedAt": "2024-12-27T10:30:00",
  "reason": "İşlem şüpheli saatlerde gerçekleşti (00:00-05:00 arası, gerçekleşme: 02:30); Yüksek işlem tutarı tespit edildi (15000.00, eşik: 10000.00)"
}
```

### GET /api/fraud/health

Sağlık kontrolü endpoint'i.

### GET /api/transactions/{accountId}

Belirli bir hesap ID'sine ait işlem geçmişini sayfalanmış olarak döndürür.

**Path Parameters:**
- `accountId` (String): Hesap ID'si

**Query Parameters:**
- `page` (int, default: 0): Sayfa numarası (0-indexed)
- `size` (int, default: 20): Sayfa başına kayıt sayısı

**Örnek İstek:**
```bash
curl -X GET "http://localhost:8080/api/transactions/****1234?page=0&size=20"
```

**Yanıt:**
```json
{
  "transactions": [
    {
      "id": 1,
      "accountId": "****1234",
      "amount": 15000.00,
      "timestamp": "2024-12-27T02:30:00",
      "fraudulent": true,
      "triggeredRule": "İşlem şüpheli saatlerde gerçekleşti",
      "riskScore": 0.6250,
      "decision": "REVIEW",
      "createdAt": "2024-12-27T10:30:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## API Dokümantasyonu

Uygulama çalıştıktan sonra Swagger UI'ya şu adresten erişebilirsiniz:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## Yapılandırma

Yapılandırma `application.yml` dosyası üzerinden yönetilir:

```yaml
fraud:
  detection:
    # Risk skoru eşikleri
    thresholds:
      approve-max: 0.4
      review-min: 0.4
      review-max: 0.7
      block-min: 0.7
    
    # Kural ağırlıkları (toplam yaklaşık 1.0 olmalı)
    rules:
      time-of-day-weight: 0.30
      amount-weight: 0.35
      frequency-weight: 0.35
      time-of-day:
        start-hour: 0
        end-hour: 5
    
    # Tutar eşikleri
    amount:
      high-threshold: 10000.0
      very-high-threshold: 50000.0
    
    # Sıklık kontrolü
    frequency:
      time-window-minutes: 15
      suspicious-count: 3
```

## Derleme ve Çalıştırma

### Ön Gereksinimler

- Java 17 veya üzeri
- Maven 3.6+
- Docker (isteğe bağlı, konteynerleştirilmiş dağıtım için)

### Yerel Geliştirme

#### Derleme

```bash
mvn clean install
```

#### Çalıştırma

```bash
mvn spring-boot:run
```

Veya JAR dosyasını çalıştırın:

```bash
java -jar target/fraud-detection-service-1.0.0.jar
```

Servis 8080 portunda başlayacaktır.

### Docker ile Çalıştırma

#### Docker Image Oluşturma

```bash
docker build -t fraud-detection-service:1.0.0 .
```

#### Docker ile Çalıştırma

```bash
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  fraud-detection-service:1.0.0
```

#### Docker Compose ile Çalıştırma

```bash
# Servisi başlat
docker-compose up -d

# Logları görüntüle
docker-compose logs -f

# Servisi durdur
docker-compose down
```

Docker Compose otomatik olarak:
- Image'ı build eder
- Container'ı başlatır
- Health check yapar
- Port mapping yapar (8080:8080)
- Environment variable'ları set eder

## Test Etme

### cURL ile Örnek İstek

```bash
curl -X POST http://localhost:8080/api/fraud/check \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-001",
    "cardNumber": "4111111111111111",
    "amount": 25000.00,
    "merchantName": "Test Merchant",
    "merchantCategory": "RETAIL",
    "transactionTimestamp": "2024-12-27T03:00:00",
    "currency": "USD",
    "cardHolderName": "Test User"
  }'
```

## Yeni Kural Ekleme

Yeni bir dolandırıcılık tespit kuralı eklemek için:

1. `FraudRule` interface'ini implement edin:

```java
@Component
public class NewFraudRule implements FraudRule {
    
    @Override
    public double calculateRisk(Transaction transaction) {
        // Risk hesaplama mantığı
        return riskScore; // 0.0 - 1.0 arası
    }
    
    @Override
    public String getRuleName() {
        return "Yeni Kural Adı";
    }
    
    @Override
    public String getRiskReason(Transaction transaction) {
        // Risk açıklaması
        return "Risk sebebi açıklaması";
    }
}
```

2. `application.yml`'e kural ağırlığını ekleyin:

```yaml
fraud:
  detection:
    rules:
      new-rule-weight: 0.20  # Yeni kural ağırlığı
```

3. `RiskScoreCalculator`'da kural ağırlığını map'e ekleyin.

Spring otomatik olarak yeni kuralı bulacak ve kullanacaktır!

## Kod Kalitesi

Bu proje şunları takip eder:

- ✅ **SOLID Prensipleri** - Her sınıf tek sorumluluğa sahip
- ✅ **Strategy Pattern** - Modüler kural yapısı
- ✅ **Clean Code** - Okunabilir ve bakımı kolay kod
- ✅ **Katmanlı Mimari** - Sorumlulukların net ayrımı
- ✅ **Açıklanabilir Kararlar** - Her karar için sebep üretimi
- ✅ **Yapılandırılabilirlik** - Magic number yok, tüm değerler config'den
- ✅ **Güvenlik** - Loglarda kart numarası maskeleme
- ✅ **Exception Handling** - Merkezi hata yönetimi

## Docker Özellikleri

### Multi-Stage Build

Dockerfile iki aşamalı build kullanır:
1. **Build Stage**: Maven ile uygulamayı derler
2. **Runtime Stage**: Sadece JRE içeren minimal image

Bu yaklaşım:
- Final image boyutunu küçültür (~200MB)
- Build araçlarını production image'ından çıkarır
- Güvenliği artırır (daha az attack surface)

### Güvenlik

- **Non-root user**: Container spring kullanıcısı ile çalışır
- **Minimal base image**: Alpine Linux kullanılır
- **Health check**: Otomatik sağlık kontrolü

### JVM Optimizasyonları

Container ortamı için optimize edilmiş JVM parametreleri:
- `-XX:+UseContainerSupport`: Container memory limitlerini algılar
- `-XX:MaxRAMPercentage=75.0`: RAM'in %75'ini kullanır
- `-XX:+UseG1GC`: G1 Garbage Collector kullanır

### Resource Limits

Docker Compose'da resource limitleri tanımlı:
- CPU: 1.0 core limit, 0.5 core reservation
- Memory: 512MB limit, 256MB reservation

## Üretim Düşünceleri

### FrequencyRule için Dağıtılmış Önbellek

Şu an `FrequencyRule` in-memory çözüm kullanmaktadır. Üretim ortamında:

**Redis Önerisi:**
```java
// RedisTemplate ile değiştirilebilir
@Autowired
private RedisTemplate<String, List<LocalDateTime>> redisTemplate;

// Key: cardNumber, Value: List<LocalDateTime>
// TTL: 24 saat
```

**Hazelcast Önerisi:**
```java
// Hazelcast IMap ile değiştirilebilir
@Autowired
private IMap<String, List<LocalDateTime>> transactionHistoryMap;
```

### Diğer Öneriler

1. **Veritabanı**: İşlem geçmişini kalıcı hale getirin
2. **Monitoring**: Prometheus metrikleri zaten entegre
3. **Logging**: Structured logging eklenebilir
4. **Security**: JWT/OAuth2 authentication eklenebilir
5. **Machine Learning**: ML modelleri ile kurallar geliştirilebilir

## Lisans

Özel - Yalnızca dahili kullanım

## Yazar

Dolandırıcılık Tespit Ekibi
