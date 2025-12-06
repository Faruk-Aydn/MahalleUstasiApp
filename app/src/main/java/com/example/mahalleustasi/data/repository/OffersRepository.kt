package com.example.mahalleustasi.data.repository

import com.example.mahalleustasi.data.model.Offer
import com.example.mahalleustasi.data.model.AppNotification
import com.example.mahalleustasi.data.model.Job
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

interface OffersRepositoryContract {
    suspend fun listOffersForJob(jobId: String): List<Offer>
    suspend fun listOffersByPro(proId: String = ""): List<Offer>
    suspend fun submitOffer(offer: Offer): String
    suspend fun updateStatus(offerId: String, status: String)
    suspend fun acceptOffer(offerId: String)
}

class OffersRepository : OffersRepositoryContract {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    override suspend fun listOffersForJob(jobId: String): List<Offer> {
        val snap = db.collection("job_offers")
            .whereEqualTo("jobId", jobId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Offer::class.java)?.copy(id = it.id) }
    }

    override suspend fun listOffersByPro(proId: String): List<Offer> {
        val actualProId = if (proId.isNotEmpty()) proId else auth.currentUser?.uid ?: ""
        if (actualProId.isEmpty()) return emptyList()
        val snap = db.collection("job_offers")
            .whereEqualTo("proId", actualProId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        return snap.documents.mapNotNull { it.toObject(Offer::class.java)?.copy(id = it.id) }
    }

    override suspend fun submitOffer(offer: Offer): String {
        val proId = offer.proId.ifEmpty { auth.currentUser?.uid ?: throw IllegalStateException("Not logged in") }

        // Aynı ustanın aynı işe aktif (pending/accepted) bir teklifi varsa yeni teklif oluşturma
        val existingSnap = db.collection("job_offers")
            .whereEqualTo("jobId", offer.jobId)
            .whereEqualTo("proId", proId)
            .whereIn("status", listOf("pending", "accepted"))
            .limit(1)
            .get()
            .await()

        if (!existingSnap.isEmpty) {
            throw IllegalStateException("Bu iş için zaten aktif bir teklifin var. Önce mevcut teklifini geri çekmelisin.")
        }

        // Belge id'sini önce oluştur ve id alanını dokümana yaz
        val ref = db.collection("job_offers").document()
        ref.set(offer.copy(proId = proId, id = ref.id)).await()

        // İş sahibine bildirim oluştur
        runCatching {
            val jobSnap = db.collection("jobs").document(offer.jobId).get().await()
            val job = jobSnap.toObject(Job::class.java)
            val ownerId = job?.ownerId
            if (!ownerId.isNullOrBlank()) {
                val notif = AppNotification(
                    userId = ownerId,
                    type = "new_offer",
                    title = "Yeni teklif",
                    body = "İşine yeni bir teklif geldi.",
                    data = mapOf("jobId" to offer.jobId, "offerId" to ref.id)
                )
                val notifRef = db.collection("notifications").document()
                db.collection("notifications").document(notifRef.id)
                    .set(notif.copy(id = notifRef.id))
                    .await()
            }
        }
        return ref.id
    }

    override suspend fun updateStatus(offerId: String, status: String) {
        db.collection("job_offers").document(offerId).update("status", status).await()
    }

    override suspend fun acceptOffer(offerId: String) {
        val dbRef = db
        Firebase.firestore.runTransaction { tr ->
            val offerDoc = tr.get(dbRef.collection("job_offers").document(offerId))
            if (!offerDoc.exists()) throw IllegalStateException("Offer not found")
            val offer = offerDoc.toObject<Offer>() ?: throw IllegalStateException("Invalid offer")
            val jobId = offer.jobId
            val proId = offer.proId

            // offer -> accepted
            tr.update(offerDoc.reference, mapOf("status" to "accepted"))
            // job -> assigned + assignedProId
            val jobRef = dbRef.collection("jobs").document(jobId)
            tr.update(jobRef, mapOf("status" to "assigned", "assignedProId" to proId))
        }.await()

        // Aynı işe ait diğer bekleyen teklifleri reddet
        runCatching {
            val acceptedOffer = db.collection("job_offers").document(offerId).get().await().toObject(Offer::class.java)
            val jobId = acceptedOffer?.jobId ?: return@runCatching
            val others = db.collection("job_offers")
                .whereEqualTo("jobId", jobId)
                .whereEqualTo("status", "pending")
                .get().await()
            others.documents
                .filter { it.id != offerId }
                .forEach { doc ->
                    db.collection("job_offers").document(doc.id).update("status", "rejected").await()
                }
        }

        // Teklifi kabul edilen ustaya bildirim oluştur
        runCatching {
            val offerSnap = db.collection("job_offers").document(offerId).get().await()
            val offer = offerSnap.toObject(Offer::class.java) ?: return@runCatching
            val notif = AppNotification(
                userId = offer.proId,
                type = "offer_accepted",
                title = "Teklif kabul edildi",
                body = "Teklifin kabul edildi. Sohbete başla.",
                data = mapOf("jobId" to offer.jobId, "offerId" to offerId)
            )
            val doc = db.collection("notifications").document()
            db.collection("notifications").document(doc.id).set(notif.copy(id = doc.id)).await()
        }
    }
}
