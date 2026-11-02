package com.srikrishnamedicals.skmedkart
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
class MainActivity: AppCompatActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val box=LinearLayout(this).apply { orientation=1; gravity=Gravity.TOP; setPadding(28,36,28,24) }
  box.addView(TextView(this).apply { text="SKMedKART"; textSize=28f; setTextColor(0xFF126B48.toInt()) })
  box.addView(TextView(this).apply { text="Sri Krishna Medicals • Pennagaram\nNative Android • Live Firebase"; textSize=15f })
  val info=TextView(this).apply { textSize=14f; setPadding(0,16,0,16) }; box.addView(info)
  listOf("Billing","Purchase Update","Batch-wise Stock","Customers & Reminders","H / H1 Register","Reports","Backup / Import").forEach { label ->
   box.addView(Button(this).apply { text=label; isAllCaps=false; setOnClickListener { info.text="$label screen is a placeholder in this foundation build." } })
  }
  info.text=if(FirebaseApp.getApps(this).isNotEmpty()) "Firebase initialized; authentication and shop membership setup still required." else "Firebase setup pending: add app/google-services.json."
  setContentView(box)
 }
}
