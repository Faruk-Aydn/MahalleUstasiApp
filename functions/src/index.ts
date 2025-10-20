import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();
const fcm = admin.messaging();

async function getUserToken(uid: string): Promise<string | null> {
  const userDoc = await db.collection("users").doc(uid).get();
  const data = userDoc.data() as { fcmToken?: string } | undefined;
  return data?.fcmToken ?? null;
}

export const onOfferCreated = functions.firestore
  .document("job_offers/{offerId}")
  .onCreate(async (snap: functions.firestore.QueryDocumentSnapshot, context: functions.EventContext) => {
    const offer = snap.data() as {
      jobId: string;
      proId: string;
      amount?: number;
      note?: string;
    };
    if (!offer?.jobId) return;

    const jobDoc = await db.collection("jobs").doc(offer.jobId).get();
    if (!jobDoc.exists) return;
    const job = jobDoc.data() as { ownerId?: string; title?: string };
    const ownerId = job?.ownerId;
    if (!ownerId) return;

    const token = await getUserToken(ownerId);
    if (!token) return;

    const payload: admin.messaging.MessagingPayload = {
      notification: {
        title: "Yeni teklif",
        body: `${job.title ?? "İş"} için yeni bir teklif geldi` ,
      },
      data: {
        type: "new_offer",
        jobId: offer.jobId,
        offerId: context.params.offerId,
      },
    };

    await fcm.sendToDevice(token, payload);
  });

export const onOfferUpdated = functions.firestore
  .document("job_offers/{offerId}")
  .onUpdate(async (change: functions.Change<functions.firestore.QueryDocumentSnapshot>, context: functions.EventContext) => {
    const before = change.before.data() as { status?: string; proId: string; jobId: string };
    const after = change.after.data() as { status?: string; proId: string; jobId: string };

    if (before.status === "pending" && after.status === "accepted") {
      const token = await getUserToken(after.proId);
      if (!token) return;

      const payload: admin.messaging.MessagingPayload = {
        notification: {
          title: "Teklif kabul edildi",
          body: "Teklifin kabul edildi. Sohbete başlayabilirsin.",
        },
        data: {
          type: "offer_accepted",
          jobId: after.jobId,
          offerId: context.params.offerId,
        },
      };
      await fcm.sendToDevice(token, payload);
    }
  });
