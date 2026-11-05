package com.srikrishnamedicals.skmedkart

import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.android.gms.tasks.Tasks
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private lateinit var status: TextView
    private val backupCollections = listOf("products","batches","bills","purchases","suppliers","customers","reminders","orders","stockMovements","stock","h_register","meta","staff")
    private val restorePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        try {
            val text = contentResolver.openInputStream(uri)?.use { BufferedReader(InputStreamReader(it, Charsets.UTF_8)).readText() }
                ?: error("Could not read backup file")
            restoreAdminBackup(text)
        } catch (e: Exception) { status.text = "Restore failed: " + (e.message ?: "Invalid backup file") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebase()
        if (auth.currentUser == null) login() else dashboard()
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
            .setApiKey("AIzaSyBdvOUiTVoBJHPE418iZqNzYftiN9yjooA")
            .setApplicationId("1:921893232974:web:e7fab8eae5eaaec6597e1f")
            .setProjectId("skmedkart")
            .setStorageBucket("skmedkart.firebasestorage.app").build())
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
            text = "Sri Krishna Medicals • Pennagaram\nNative Android • Live Firebase"
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
            text = "\nSeparate app: the old SKMedKART Admin app is not modified."
        })
    }

    private fun dashboard() {
        window.statusBarColor = 0xFF24629B.toInt()
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFF2F7FB.toInt())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 20, 20)
            setBackgroundColor(0xFF24629B.toInt())
        }
        header.addView(TextView(this).apply {
            text = "💊 SKMedKART"
            textSize = 30f
            setTextColor(0xFFFFFFFF.toInt())
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "Sri Krishna Medicals • Pennagaram"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(0, 6, 0, 0)
        })
        root.addView(header)

        val scroll = ScrollView(this)
        body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 24)
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(4, 8, 4, 6)
        }
        listOf(
            "🏠\nHome" to { dashboard() },
            "🧾\nBilling" to { billing() },
            "📊\nReports" to { reports() },
            "💊\nStock" to { stock() },
            "🏭\nPurchase" to { purchase() },
            "⚙️\nSettings" to { backupRestore() }
        ).forEach { (label, action) ->
            bottom.addView(TextView(this).apply {
                text = label
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(0xFF50606F.toInt())
                setPadding(2, 2, 2, 2)
                setOnClickListener { action() }
            }, LinearLayout.LayoutParams(0, 62, 1f))
        }
        root.addView(bottom)

        setContentView(root)
        status = TextView(this)
        status.text = "Live Firebase • " + (auth.currentUser?.email ?: "")
        status.setTextColor(0xFF6D7780.toInt())
        status.textSize = 14f
        body.addView(status)

        val safe = TextView(this).apply {
            text = "📱  Firebase Connected\nBills, purchases and stock are connected to Firebase. Existing offline Admin app is not modified."
            textSize = 16f
            setTextColor(0xFF6A5B1A.toInt())
            setPadding(18, 18, 18, 18)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFF8D9.toInt())
                setStroke(1, 0xFFE5D78A.toInt())
                cornerRadius = 26f
            }
        }
        body.addView(safe, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 })

        val cards = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cards.addView(dashboardCard("🧾", "Today's Sales", "₹0.00"), LinearLayout.LayoutParams(0, 155, 1f).apply { rightMargin = 8 })
        cards.addView(dashboardCard("📦", "Stock Batches", "Loading…"), LinearLayout.LayoutParams(0, 155, 1f).apply { leftMargin = 8 })
        body.addView(cards)
        body.addView(LinearLayout(this).apply { minimumHeight = 16 })

        val cards2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        cards2.addView(dashboardCard("⚠️", "Low Stock", "Loading…"), LinearLayout.LayoutParams(0, 155, 1f).apply { rightMargin = 8 })
        cards2.addView(dashboardCard("⏰", "Expiry Alerts", "Loading…"), LinearLayout.LayoutParams(0, 155, 1f).apply { leftMargin = 8 })
        body.addView(cards2)

        body.addView(TextView(this).apply {
            text = "⚡ Quick Actions"
            textSize = 28f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF263B52.toInt())
            setPadding(8, 24, 8, 12)
        })

        val q1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        q1.addView(quickButton("➕ New Bill") { billing() }, LinearLayout.LayoutParams(0, 62, 1f).apply { rightMargin = 8 })
        q1.addView(quickButton("🏭 Purchase Entry") { purchase() }, LinearLayout.LayoutParams(0, 62, 1f).apply { leftMargin = 8 })
        body.addView(q1)

        val q2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        q2.addView(quickButton("📦 Stock") { stock() }, LinearLayout.LayoutParams(0, 62, 1f).apply { rightMargin = 8 })
        q2.addView(quickButton("🔔 Reminders") { customers() }, LinearLayout.LayoutParams(0, 62, 1f).apply { leftMargin = 8 })
        body.addView(q2)

        body.addView(quickButton("💾 Backup & Restore") { backupRestore() }.apply {
            setBackgroundColor(0xFF238A68.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }, LinearLayout.LayoutParams(-1, 62).apply { topMargin = 14 })

        loadDashboardStats()
    }

    private fun dashboardCard(icon: String, title: String, value: String): TextView =
        TextView(this).apply {
            text = icon + "\n" + title + "\n" + value
            textSize = 17f
            setTextColor(0xFF2D4660.toInt())
            setPadding(22, 18, 12, 12)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFFFFFFF.toInt())
                cornerRadius = 34f
            }
        }

    private fun quickButton(text: String, action: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF31516D.toInt())
            setOnClickListener { action() }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0xFFE4EDF5.toInt())
                cornerRadius = 32f
            }
        }

    private fun loadDashboardStats() {
        db.collection("bills").get().addOnSuccessListener { bills ->
            val sales = bills.documents.sumOf { it.getDouble("total") ?: 0.0 }
            val salesCard = body.getChildAt(1) as? LinearLayout
            val first = salesCard?.getChildAt(0) as? TextView
            first?.text = "🧾\nToday's Sales\n₹%.2f".format(Locale.US, sales)
        }
        db.collection("stock").get().addOnSuccessListener { stock ->
            val row1 = body.getChildAt(2) as? LinearLayout
            val stockCard = row1?.getChildAt(1) as? TextView
            stockCard?.text = "📦\nStock Batches\n" + stock.size()
            var low = 0
            var expiry = 0
            val now = System.currentTimeMillis()
            stock.documents.forEach { d ->
                if ((d.getDouble("availableQty") ?: 0.0) <= 10.0) low++
                val exp = d.getString("expiry") ?: ""
                if (exp.isNotBlank()) {
                    val parts = exp.split("/", "-")
                    if (parts.size >= 2) {
                        val y = parts.last().toIntOrNull()
                        val m = parts.first().toIntOrNull()
                        if (y != null && m != null) {
                            val year = if (y < 100) 2000 + y else y
                            val cal = java.util.Calendar.getInstance().apply {
                                set(year, m - 1, 1, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
                            }
                            if (cal.timeInMillis <= now + 90L * 24 * 60 * 60 * 1000) expiry++
                        }
                    }
                }
            }
            val row2 = body.getChildAt(4) as? LinearLayout
            (row2?.getChildAt(0) as? TextView)?.text = "⚠️\nLow Stock\n" + low
            (row2?.getChildAt(1) as? TextView)?.text = "⏰\nExpiry Alerts\n" + expiry
        }
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
                    text = (d.getString("medicine") ?: "") + "\nBatch: " + (d.getString("batchNo") ?: "") +
                        " • Exp: " + (d.getString("expiry") ?: "") + "\nAvailable: " +
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
                    text = (d.getString("invoiceNo") ?: d.id) + "\n" +
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
                            "\n" + (d.getString("medicinePlan") ?: "") + "\n" + (d.getString("instructions") ?: "")
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
                    text = "Bills: " + bills.size() + "\nSales Qty: " + qty +
                        "\nSales Value: ₹%.2f".format(Locale.US,sales) +
                        "\nPurchase Entries: " + purchases.size() + "\nPurchase Qty: " + pq
                    textSize=17f
                })
                status.text="Live report loaded."
            }
        }.addOnFailureListener { status.text="Report failed: " + (it.message ?: "") }
    }

    private fun backupRestore() {
        page("Backup & Restore"); back()
        body.addView(TextView(this).apply {
            text = "Restore the JSON backup downloaded from the existing offline SKMedKART Admin app.\n\nSupported collections:\nProducts • Batches • Bills • Purchases • Suppliers • Customers • Reminders • Orders • Stock Movements\n\nThis app writes the backup into Firebase. The existing offline Admin app is not changed."
            textSize = 16f; setPadding(8, 8, 8, 16)
        })
        body.addView(Button(this).apply {
            text = "📂 Choose Admin Backup File"
            isAllCaps = false
            setOnClickListener { restorePicker.launch(arrayOf("application/json","text/json","text/plain")) }
        })
    }

    private fun restoredData() {
        page("Restored Admin Data"); back()
        status.text = "Loading Firebase data..."
        backupCollections.forEach { name ->
            db.collection(name).get().addOnSuccessListener { snap ->
                body.addView(TextView(this).apply {
                    text = name.uppercase(Locale.US) + " • " + snap.size() + " records"
                    textSize = 18f; setPadding(8, 12, 8, 4)
                })
                snap.documents.take(100).forEach { d ->
                    body.addView(TextView(this).apply {
                        text = d.id + "\n" + (d.data?.entries?.joinToString("\n") { it.key + ": " + it.value } ?: "")
                        setPadding(12, 6, 8, 8)
                    })
                }
            }.addOnFailureListener { status.text = "Firebase read failed for " + name + ": " + (it.message ?: "") }
        }
    }

    private fun restoreAdminBackup(backupText: String) {
        val root = JSONObject(backupText)
        val collections = root.optJSONObject("collections")
            ?: throw IllegalArgumentException("Invalid SKMedKART backup: collections not found")

        page("Restore Backup")
        val progress = TextView(this).apply {
            text = "Preparing Admin backup…"
            textSize = 17f
            setPadding(8, 8, 8, 16)
        }
        body.addView(progress)

        Thread {
            try {
                var total = 0
                val names = collections.keys().asSequence().toList()

                for (name in names) {
                    val rows = collections.optJSONArray(name) ?: continue
                    var start = 0

                    while (start < rows.length()) {
                        val end = minOf(start + 200, rows.length())
                        val batch = db.batch()

                        for (i in start until end) {
                            val row = rows.getJSONObject(i)
                            val id = backupDocumentId(name, row, i)
                            if (id.isBlank()) {
                                throw IllegalArgumentException("Could not create a document ID for " + name + " record " + (i + 1))
                            }

                            val data = jsonObjectToMap(row)
                            data.remove("id")
                            data.remove("docId")
                            data.remove("documentId")
                            data.remove("_id")
                            data["_originalBackupId"] = id

                            batch.set(
                                db.collection(name).document(id),
                                data,
                                SetOptions.merge()
                            )
                        }

                        runOnUiThread {
                            progress.text = "Restoring " + name + "… " + end + "/" + rows.length() + " records"
                        }

                        var committed = false
                        var lastError: Exception? = null
                        for (attempt in 1..3) {
                            try {
                                Tasks.await(batch.commit(), 30, TimeUnit.SECONDS)
                                committed = true
                                break
                            } catch (e: Exception) {
                                lastError = e
                                if (attempt < 3) Thread.sleep(attempt * 1000L)
                            }
                        }
                        if (!committed) {
                            throw IllegalStateException(
                                "Firebase write failed in " + name + " records " +
                                    (start + 1) + "-" + end + ": " +
                                    (lastError?.message ?: "timeout")
                            )
                        }

                        total += end - start
                        start = end

                        runOnUiThread {
                            progress.text = "Restoring " + name + "… " + start + "/" + rows.length() +
                                " records\nTotal restored: " + total
                        }
                    }

                    runOnUiThread {
                        progress.text = "Completed " + name + " • " + rows.length() + " records"
                    }
                }

                runOnUiThread {
                    status.text = "Restore completed • " + total + " records written to Firebase"
                    body.removeAllViews()
                    body.addView(TextView(this).apply {
                        text = "✅ Admin backup restored successfully.\n\nAll supported offline Admin collections were copied to Firebase.\n\nExisting Admin app: unchanged."
                        textSize = 18f
                        setPadding(8, 16, 8, 16)
                    })
                    nav("View Restored Data") { restoredData() }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Restore failed: " + (e.message ?: "Unknown Firebase error")
                    progress.text = "❌ Restore stopped.\n\n" +
                        (e.message ?: "Unknown Firebase error")
                }
            }
        }.start()
    }

    private fun backupDocumentId(collection: String, row: JSONObject, index: Int): String {
        val explicit = listOf("id","docId","documentId","_id").firstNotNullOfOrNull { key ->
            row.optString(key).takeIf { it.isNotBlank() }
        }
        if (explicit != null) {
            val cleaned = safeId(explicit)
            if (cleaned.isNotBlank()) return cleaned
        }
        val natural = when (collection) {
            "customers" -> row.optString("mobile").ifBlank { row.optString("phone") }.ifBlank { row.optString("name") }
            "products" -> row.optString("name").ifBlank { row.optString("medicine") }.ifBlank { row.optString("code") }
            "bills" -> row.optString("invoiceNo").ifBlank { row.optString("billNo") }
            "purchases" -> (row.optString("invoiceNo") + "_" + row.optString("batchNo")).trim('_')
            "batches","stock" -> (row.optString("medicine") + "__" + row.optString("batchNo")).trim('_')
            else -> ""
        }
        if (natural.isNotBlank()) return collection + "__" + safeId(natural)
        val digest = MessageDigest.getInstance("SHA-256").digest(row.toString().toByteArray(Charsets.UTF_8))
        return collection + "__" + digest.joinToString("") { "%02x".format(it) }.take(60) + "__" + index
    }

    private fun safeId(value: String): String =
        value.trim().lowercase(Locale.US).replace(Regex("[^a-z0-9_-]"), "_").take(120)

    private fun jsonObjectToMap(obj: JSONObject): MutableMap<String, Any?> {
        val out = mutableMapOf<String, Any?>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            out[key] = jsonValueToFirebase(obj.get(key))
        }
        return out
    }

    private fun jsonValueToFirebase(value: Any?): Any? = when (value) {
        JSONObject.NULL -> null
        is JSONObject -> jsonObjectToMap(value)
        is JSONArray -> {
            val list = ArrayList<Any?>()
            for (i in 0 until value.length()) list.add(jsonValueToFirebase(value.get(i)))
            list
        }
        else -> value
    }

    private fun stockId(medicine: String, batch: String): String =
        (medicine.trim().lowercase(Locale.US) + "__" + batch.trim().lowercase(Locale.US))
            .replace(Regex("[^a-z0-9_-]"), "_").take(140)
}
