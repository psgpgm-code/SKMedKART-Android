package com.srikrishnamedicals.skmedkart.data
object FirestorePaths {
 fun shop(id:String)="shops/$id"
 fun invoices(id:String)="${shop(id)}/invoices"
 fun purchases(id:String)="${shop(id)}/purchases"
 fun products(id:String)="${shop(id)}/products"
 fun batches(id:String)="${shop(id)}/batches"
 fun movements(id:String)="${shop(id)}/stockMovements"
 fun customers(id:String)="${shop(id)}/customers"
 fun reminders(id:String)="${shop(id)}/reminders"
 fun suppliers(id:String)="${shop(id)}/suppliers"
 fun counters(id:String)="${shop(id)}/counters"
}
