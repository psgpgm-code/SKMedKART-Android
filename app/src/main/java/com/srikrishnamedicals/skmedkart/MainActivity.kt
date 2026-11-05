package com.srikrishnamedicals.skmedkart

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) login() else dashboard()
    }

    private fun page(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(TextView(this).apply {
            text = title; textSize = 24f; setTextColor(0xFF126B48.toInt())
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        if (auth.currentUser != null) top.addView(Button(this).apply {
            text = "Logout"; setOnClickListener { auth.signOut(); login() }
        })
        root.addView(top)
        status = TextView(this).apply { textSize = 13f; setPadding(0, 8, 0, 8) }
        root.addView(status)
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(body, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun input(hint: String, type: Int = 1) = EditText(this).apply {
        this.hint = hint; inputType = type
    }

    private fun login() {
        page("SKMedKART")
        body.addView(TextView(this).apply {
            text = "Sri Krishna Medicals • Pennagaram\\nNative Android • Live Firebase"
            textSize = 15f
        })
        val email = input("Staff email", 33)
        val pass = input("Password (6+ characters)", 129)
        body.addView(email); body.addView(pass)
        body.addView(Button(this).apply {
            text = "Login"; setOnClickListener {
                val e = email.text.toString().trim(); val p = pass.text.toString()
                if (e.isBlank() || p.length < 6) { status.text = "Enter valid email and password."; return@setOnClickListener }
                status.text = "Signing in..."
                auth.signInWithEmailAndPassword(e, p).addOnSuccessListener { dashboard() }
                    .addOnFailureListener { status.text = "Login failed: " + (it.message ?: "check Firebase Authentication") }
            }
        })
        body.addView(Button(this).apply {
            text = "Create staff account"; setOnClickListener {
                val e = email.text.toString().trim(); val p = pass.text.toString()
                if (e.isBlank() || p.length < 6) { status.text = "Enter email and password."; return@setOnClickListener }
                status.text = "Creating account..."
                auth.createUserWithEmailAndPassword(e, p).addOnSuccessListener {
                    val u = auth.currentUser!!.uid
                    db.collection("staff").document(u).set(
                        mapOf("email" to e, "createdAt" to FieldValue.serverTimestamp()),
                        SetOptions.merge()
                    ).addOnCompleteListener { dashboard() }
                }.addOnFailureListener {
                    status.text = "Account creation failed: " + (it.message ?: "enable Email/Password in Firebase")
                }
            }
        })
        body.addView(TextView(this).apply {
            text = "\\nSeparate app: the old SKMedKART Admin app is not modified."
        })
    }

    private fun dashboard() {
        page("SKMedKART Dashboard")
        status.text = "Live Firebase • " + (auth.currentUser?.email ?: "")
        nav("Billing") { billing() }
        nav("Purchase Entry") { purchase() }
        nav("Batch-wise Stock") { stock() }
        nav("Bill History") { history("bills") }
        nav("Purchase History") { history("purchases") }
        nav("Customers & Reminders") { customers() }
        nav("H / H1 Register") { register() }
        nav("Reports") { reports() }
    }

    private fun nav(text: String, action: () -> Unit) {
        body.addView(Button(this).apply { this.text = text; isAllCaps = false; setOnClickListener { action() } })
    }

    private fun back() {
        body.addView(Button(this).apply { text = "← Back"; setOnClickListener { dashboard() } })
    }

    private fun billing() {
        page("Billing"); back()
        val customer = input("Customer name")
        val mobile = input("Mobile number", 2)
        val medicine = input("Medicine")
        val batch = input("Batch No")
        val expiry = input("Expiry MM/YYYY")
        val qty = input("Quantity", 2)
        val price = input("Selling price / MRP", 8194)
        val discount = input("Discount %", 8194)
        val gst = input("GST %", 8194)
        val payment = input("Payment mode")
        listOf(customer,mobile,medicine,batch,expiry,qty,price,discount,gst,payment).forEach { body.addView(it) }
        body.addView(Button(this).apply {
            text = "Save Bill"
            setOnClickListener {
                val q = qty.text.toString().toDoubleOrNull() ?: 0.0
                val p = price.text.toString().toDoubleOrNull() ?: 0.0
                val d = discount.text.toString().toDoubleOrNull() ?: 0.0
                val g = gst.text.toString().toDoubleOrNull() ?: 0.0
                if (medicine.text.isBlank() || batch.text.isBlank() || q <= 0) {
                    status.text = "Medicine, batch and quantity are required."; return@setOnClickListener
                }
                val subtotal = q * p
                val afterDiscount = subtotal - subtotal * d / 100.0
                val total = afterDiscount + afterDiscount * g / 100.0
                val net = round(total)
                status.text = "Total ₹%.2f • Round off ₹%.2f • Net ₹%.0f".format(Locale.US, total, net-total, net)
                saveBill(customer.text.toString(), mobile.text.toString(), medicine.text.toString(),
                    batch.text.toString(), expiry.text.toString(), q, p, d, g, payment.text.toString(), net)
            }
        })
    }

    private fun saveBill(customer:String, mobile:String, med:String, batch:String, expiry:String,
                         qty:Double, price:Double, discount:Double, gst:Double, payment:String, total:Double) {
        status.text = "Saving bill..."
        val counter = db.collection("meta").document("counters")
        db.runTransaction { tx ->
            val c = tx.get(counter)
            val no = (c.getLong("invoice") ?: 0L) + 1L
            tx.set(counter, mapOf("invoice" to no), SetOptions.merge())
            val bill = db.collection("bills").document()
            tx.set(bill, mapOf(
                "invoiceNo" to "SKM-%03d".format(no),
                "customerName" to customer, "mobile" to mobile, "medicine" to med,
                "batchNo" to batch, "expiry" to expiry, "qty" to qty, "price" to price,
                "discountPercent" to discount, "gstPercent" to gst, "total" to total,
                "paymentMode" to payment, "createdAt" to FieldValue.serverTimestamp(),
                "createdBy" to auth.currentUser?.uid
            ))
            val s = db.collection("stock").document(stockId(med,batch))
            val old = tx.get(s)
            val available = old.getDouble("availableQty") ?: 0.0
            if (available < qty) throw IllegalStateException("Insufficient stock. Available: " + available)
            tx.update(s, mapOf(
                "availableQty" to available - qty,
                "soldQty" to (old.getDouble("soldQty") ?: 0.0) + qty,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            null
        }.addOnSuccessListener { status.text = "Bill saved successfully." }
         .addOnFailureListener { status.text = "Bill not saved: " + (it.message ?: "") }
    }

    private fun purchase() {
        page("Purchase Entry"); back()
        val supplier = input("Supplier")
        val inv = input("Purchase invoice No")
        val date = input("Purchase date")
        val med = input("Medicine")
        val batch = input("Batch No")
        val expiry = input("Expiry MM/YYYY")
        val pp = input("Purchase price", 8194)
        val mrp = input("MRP", 8194)
        val qty = input("Quantity", 2)
        listOf(supplier,inv,date,med,batch,expiry,pp,mrp,qty).forEach { body.addView(it) }
        body.addView(Button(this).apply {
            text = "Save Purchase & Increase Stock"
            setOnClickListener {
                val q = qty.text.toString().toDoubleOrNull() ?: 0.0
                if (med.text.isBlank() || batch.text.isBlank() || q <= 0) {
                    status.text = "Medicine, batch and quantity are required."; return@setOnClickListener
                }
                val purchaseRef = db.collection("purchases").document()
                val stockRef = db.collection("stock").document(stockId(med.text.toString(),batch.text.toString()))
                db.runTransaction { tx ->
                    val old = tx.get(stockRef)
                    val available = old.getDouble("availableQty") ?: 0.0
                    tx.set(purchaseRef, mapOf(
                        "supplier" to supplier.text.toString(), "invoiceNo" to inv.text.toString(),
                        "purchaseDate" to date.text.toString(), "medicine" to med.text.toString(),
                        "batchNo" to batch.text.toString(), "expiry" to expiry.text.toString(),
                        "purchasePrice" to (pp.text.toString().toDoubleOrNull() ?: 0.0),
                        "mrp" to (mrp.text.toString().toDoubleOrNull() ?: 0.0), "qty" to q,
                        "createdAt" to FieldValue.serverTimestamp(), "createdBy" to auth.currentUser?.uid
                    ))
                    tx.set(stockRef, mapOf(
                        "medicine" to med.text.toString(), "batchNo" to batch.text.toString(),
                        "expiry" to expiry.text.toString(),
                        "purchasePrice" to (pp.text.toString().toDoubleOrNull() ?: 0.0),
                        "mrp" to (mrp.text.toString().toDoubleOrNull() ?: 0.0),
                        "availableQty" to available + q,
                        "soldQty" to (old.getDouble("soldQty") ?: 0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ), SetOptions.merge())
                    null
                }.addOnSuccessListener { status.text = "Purchase saved. Stock increased." }
                 .addOnFailureListener { status.text = "Purchase failed: " + (it.message ?: "") }
            }
        })
    }

    private fun stock() {
        page("Batch-wise Stock"); back(); status.text = "Loading live stock..."
        db.collection("stock").get().addOnSuccessListener { snap ->
            status.text = snap.size().toString() + " stock batches"
            snap.documents.forEach { d ->
                body.addView(TextView(this).apply {
                    text = (d.getString("medicine") ?: "") + "\\nBatch: " + (d.getString("batchNo") ?: "") +
                        " • Exp: " + (d.getString("expiry") ?: "") + "\\nAvailable: " +
                        (d.getDouble("availableQty") ?: 0.0) + " • Sold: " + (d.getDouble("soldQty") ?: 0.0)
                    setPadding(8,10,8,10)
                })
            }
        }.addOnFailureListener { status.text = "Stock load failed: " + (it.message ?: "") }
    }

    private fun history(collection: String) {
        page(if (collection == "bills") "Bill History" else "Purchase History"); back()
        db.collection(collection).get().addOnSuccessListener { snap ->
            status.text = snap.size().toString() + " records"
            snap.documents.forEach { d ->
                body.addView(TextView(this).apply {
                    text = (d.getString("invoiceNo") ?: d.id) + "\\n" +
                        (d.getString("medicine") ?: "") + " • Qty " + (d.getDouble("qty") ?: 0.0) +
                        " • ₹" + (d.getDouble("total") ?: d.getDouble("mrp") ?: 0.0)
                    setPadding(8,10,8,10)
                })
            }
        }.addOnFailureListener { status.text = "Load failed: " + (it.message ?: "") }
    }

    private fun customers() {
        page("Customers & Reminders"); back()
        val name = input("Customer name"); val mobile = input("Mobile",2)
        val med = input("Medicine / monthly plan")
        val instructions = input("Before/With/After food + Morning/Afternoon/Evening/Night")
        listOf(name,mobile,med,instructions).forEach { body.addView(it) }
        body.addView(Button(this).apply {
            text = "Save Customer Reminder"
            setOnClickListener {
                if (name.text.isBlank() || mobile.text.isBlank()) { status.text = "Name and mobile required."; return@setOnClickListener }
                db.collection("customers").document(mobile.text.toString().trim()).set(mapOf(
                    "name" to name.text.toString().trim(), "mobile" to mobile.text.toString().trim(),
                    "medicinePlan" to med.text.toString(), "instructions" to instructions.text.toString(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ), SetOptions.merge()).addOnSuccessListener { status.text = "Customer reminder saved." }
            }
        })
        body.addView(Button(this).apply {
            text = "View Customers"
            setOnClickListener {
                db.collection("customers").get().addOnSuccessListener { snap ->
                    status.text = snap.size().toString() + " customers"
                    snap.documents.forEach { d -> body.addView(TextView(this@MainActivity).apply {
                        text = (d.getString("name") ?: "") + " • " + (d.getString("mobile") ?: "") +
                            "\\n" + (d.getString("medicinePlan") ?: "") + "\\n" + (d.getString("instructions") ?: "")
                        setPadding(8,10,8,10)
                    }) }
                }
            }
        })
    }

    private fun register() {
        page("H / H1 Register"); back()
        val type=input("H or H1"); val med=input("Medicine"); val qty=input("Quantity",2); val patient=input("Customer / patient")
        listOf(type,med,qty,patient).forEach { body.addView(it) }
        body.addView(Button(this).apply {
            text="Save Register Entry"; setOnClickListener {
                db.collection("h_register").add(mapOf(
                    "type" to type.text.toString(), "medicine" to med.text.toString(),
                    "qty" to (qty.text.toString().toDoubleOrNull() ?: 0.0),
                    "customer" to patient.text.toString(), "createdAt" to FieldValue.serverTimestamp()
                )).addOnSuccessListener { status.text="Register entry saved." }
            }
        })
    }

    private fun reports() {
        page("Reports"); back(); status.text="Loading live report..."
        db.collection("bills").get().addOnSuccessListener { bills ->
            var sales=0.0; var qty=0.0
            bills.documents.forEach { sales += it.getDouble("total") ?: 0.0; qty += it.getDouble("qty") ?: 0.0 }
            db.collection("purchases").get().addOnSuccessListener { purchases ->
                var pq=0.0; purchases.documents.forEach { pq += it.getDouble("qty") ?: 0.0 }
                body.addView(TextView(this).apply {
                    text = "Bills: " + bills.size() + "\\nSales Qty: " + qty +
                        "\\nSales Value: ₹%.2f".format(Locale.US,sales) +
                        "\\nPurchase Entries: " + purchases.size() + "\\nPurchase Qty: " + pq
                    textSize=17f
                })
                status.text="Live report loaded."
            }
        }.addOnFailureListener { status.text="Report failed: " + (it.message ?: "") }
    }

    private fun stockId(medicine: String, batch: String): String =
        (medicine.trim().lowercase(Locale.US) + "__" + batch.trim().lowercase(Locale.US))
            .replace(Regex("[^a-z0-9_-]"), "_").take(140)
}
