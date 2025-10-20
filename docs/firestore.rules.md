# Firestore Security Rules (Taslak)

Aşağıdaki kurallar MVP için temel güvenliği sağlar. Yayına çıkmadan önce gereksinimlerinize göre daraltın.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isSignedIn() {
      return request.auth != null;
    }

    function isOwner(uid) {
      return isSignedIn() && request.auth.uid == uid;
    }

    // users/{uid}
    match /users/{uid} {
      allow read: if isSignedIn();
      allow create: if isSignedIn() && request.resource.data.uid == request.auth.uid;
      allow update, delete: if isOwner(uid);
    }

    // jobs/{jobId}
    match /jobs/{jobId} {
      allow read: if true; // herkese açık listeleme (MVP)
      allow create: if isSignedIn() && request.resource.data.ownerId == request.auth.uid;
      allow update: if isSignedIn() && (
        // sahibi durum güncelleyebilir
        (request.resource.data.ownerId == request.auth.uid) ||
        // atanan usta sadece belirli alanları güncelleyebilir
        (
          request.resource.data.assignedProId == request.auth.uid &&
          request.resource.data.status in ['assigned','completed']
        )
      );
      allow delete: if isSignedIn() && resource.data.ownerId == request.auth.uid;
    }

    // job_offers/{offerId}
    match /job_offers/{offerId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn() && request.resource.data.proId == request.auth.uid;
      allow update: if isSignedIn() && (
        // Teklif sahibi kendi teklifini geri çekebilir
        (resource.data.proId == request.auth.uid && request.resource.data.status in ['withdrawn']) ||
        // İş sahibi teklifi kabul/ret edebilir
        (
          exists(/databases/$(database)/documents/jobs/$(request.resource.data.jobId)) &&
          get(/databases/$(database)/documents/jobs/$(request.resource.data.jobId)).data.ownerId == request.auth.uid &&
          request.resource.data.status in ['accepted','rejected']
        )
      );
    }

    // payments/{paymentId}
    match /payments/{paymentId} {
      allow read: if isSignedIn() && (
        resource.data.payerId == request.auth.uid ||
        resource.data.payeeId == request.auth.uid
      );
      allow create: if isSignedIn() && request.resource.data.payerId == request.auth.uid;
      allow update: if isSignedIn() && (
        // ödeyen kendi onayını güncelleyebilir
        (resource.data.payerId == request.auth.uid) ||
        // alan kendi onayını güncelleyebilir
        (resource.data.payeeId == request.auth.uid)
      );
    }

    // reviews/{reviewId}
    match /reviews/{reviewId} {
      allow read: if true;
      allow create: if isSignedIn() && request.resource.data.reviewerId == request.auth.uid;
    }
  }
}
```

Notlar:
- jobs.read herkese açık bırakıldı (MVP). İsterseniz konum bazlı/şehir bazlı filtreler geldiğinde daraltabilirsiniz.
- job_offers.update kabul/ret işlemleri için iş sahibine yetki tanınmıştır.
- payments alanında `payerId` ve `payeeId` erişim kısıtları uygulanmıştır.
