"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
exports.onOfferUpdated = exports.onOfferCreated = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
admin.initializeApp();
const db = admin.firestore();
const fcm = admin.messaging();
async function getUserToken(uid) {
    const userDoc = await db.collection("users").doc(uid).get();
    const data = userDoc.data();
    return data?.fcmToken ?? null;
}
exports.onOfferCreated = functions.firestore
    .document("job_offers/{offerId}")
    .onCreate(async (snap, context) => {
    const offer = snap.data();
    if (!offer?.jobId)
        return;
    const jobDoc = await db.collection("jobs").doc(offer.jobId).get();
    if (!jobDoc.exists)
        return;
    const job = jobDoc.data();
    const ownerId = job?.ownerId;
    if (!ownerId)
        return;
    const token = await getUserToken(ownerId);
    if (!token)
        return;
    const payload = {
        notification: {
            title: "Yeni teklif",
            body: `${job.title ?? "İş"} için yeni bir teklif geldi`,
        },
        data: {
            type: "new_offer",
            jobId: offer.jobId,
            offerId: context.params.offerId,
        },
    };
    await fcm.sendToDevice(token, payload);
});
exports.onOfferUpdated = functions.firestore
    .document("job_offers/{offerId}")
    .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    if (before.status === "pending" && after.status === "accepted") {
        const token = await getUserToken(after.proId);
        if (!token)
            return;
        const payload = {
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
