package com.srikrishnamedicals.skmedkart.data
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
data class InvoiceLine(val productId:String,val batchId:String,val name:String,val quantity:Long,val unitPricePaise:Long)
data class InvoiceDraft(val customerName:String,val customerMobile:String,val lines:List<InvoiceLine>,val subtotalPaise:Long,val discountPaise:Long,val gstPaise:Long,val roundOffPaise:Long,val totalPaise:Long)
class InvoiceRepository(private val db:FirebaseFirestore=FirebaseFirestore.getInstance()) {
 suspend fun create(shopId:String, invoiceId:String, draft:InvoiceDraft):String {
  require(draft.lines.isNotEmpty() && draft.lines.all { it.quantity>0 && it.unitPricePaise>=0 })
  val counter=db.document("${FirestorePaths.counters(shopId)}/invoice")
  val invoice=db.document("${FirestorePaths.invoices(shopId)}/$invoiceId")
  val refs=draft.lines.map { db.document("${FirestorePaths.batches(shopId)}/${it.batchId}") }
  return db.runTransaction { tx ->
   check(!tx.get(invoice).exists()) { "Duplicate invoice ID" }
   val c=tx.get(counter); val n=(c.getLong("lastNumber")?:0L)+1L
   val snaps=refs.map { tx.get(it) }
   draft.lines.forEachIndexed { i,line -> check(snaps[i].exists() && (snaps[i].getLong("quantity")?:0L)>=line.quantity) { "Batch missing or insufficient stock: ${line.name}" } }
   draft.lines.forEachIndexed { i,line -> tx.update(refs[i],mapOf("quantity" to (snaps[i].getLong("quantity")!!-line.quantity),"updatedAt" to FieldValue.serverTimestamp())) }
   tx.set(counter,mapOf("lastNumber" to n))
   tx.set(invoice,mapOf("invoiceId" to invoiceId,"invoiceNumber" to "SKM-${n.toString().padStart(6,'0')}","customerName" to draft.customerName,"customerMobile" to draft.customerMobile,"lines" to draft.lines.map { mapOf("productId" to it.productId,"batchId" to it.batchId,"name" to it.name,"quantity" to it.quantity,"unitPricePaise" to it.unitPricePaise) },"subtotalPaise" to draft.subtotalPaise,"discountPaise" to draft.discountPaise,"gstPaise" to draft.gstPaise,"roundOffPaise" to draft.roundOffPaise,"totalPaise" to draft.totalPaise,"createdAt" to FieldValue.serverTimestamp()))
   "SKM-${n.toString().padStart(6,'0')}"
  }.await()
 }
}
