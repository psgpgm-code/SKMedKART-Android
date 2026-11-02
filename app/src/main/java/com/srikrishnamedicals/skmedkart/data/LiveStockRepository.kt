package com.srikrishnamedicals.skmedkart.data
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
class LiveStockRepository(private val db:FirebaseFirestore=FirebaseFirestore.getInstance()) {
 suspend fun applyMovement(shopId:String, operationId:String, batchId:String, type:String, sourceId:String, delta:Long) {
  require(operationId.isNotBlank() && batchId.isNotBlank() && delta!=0L)
  val batch=db.document("${FirestorePaths.batches(shopId)}/$batchId")
  val movement=db.document("${FirestorePaths.movements(shopId)}/$operationId")
  db.runTransaction { tx ->
   if(tx.get(movement).exists()) return@runTransaction null
   val snap=tx.get(batch); check(snap.exists()) { "Batch not found" }
   val next=(snap.getLong("quantity")?:0L)+delta; check(next>=0L) { "Insufficient stock" }
   tx.update(batch,mapOf("quantity" to next,"updatedAt" to FieldValue.serverTimestamp()))
   tx.set(movement,mapOf("movementId" to operationId,"sourceType" to type,"sourceId" to sourceId,"batchId" to batchId,"quantityDelta" to delta,"createdAt" to FieldValue.serverTimestamp()))
   null
  }.await()
 }
}
