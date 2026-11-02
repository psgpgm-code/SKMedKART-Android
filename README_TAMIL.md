# SKMedKART Native Android – Foundation v1

இது பழைய Admin Website-ஐ WebView-ல் திறக்கும் APK அல்ல; தனி Kotlin Android project.

சேர்க்கப்பட்டது:
- Android project, Kotlin, Java 17, Gradle 8.9 workflow
- Firebase Auth/Firestore/Storage dependencies
- Shop-scoped Firestore path structure
- Transaction-based stock movement with duplicate-operation protection
- Transaction-based invoice counter + batch stock deduction
- Starter Firestore security rules
- Billing/Purchase/Stock/Customers-Reminders/H-H1/Reports/Backup module entry buttons

முழுமையாக இன்னும் சேர்க்கப்படாதவை:
UI மற்றும் முழு business flows இன்னும் placeholder மட்டுமே. Purchase edit/delete, sales/supplier returns, reminder scheduling, report/PDF/WhatsApp, backup JSON importer, authentication screens, pagination UI ஆகியவை production feature-complete அல்ல. ஆகவே இதை இன்னும் live production app என்று பயன்படுத்த வேண்டாம்.

Firebase setup:
1. Firebase project skmedkart-ல் Android app package `com.srikrishnamedicals.skmedkart` register செய்யவும்.
2. `google-services.json`-ஐ `app/`-ல் சேர்க்கவும். அதை public repository-ல் commit செய்ய வேண்டாம்.
3. Firebase Auth provider, Firestore enable செய்யவும்.
4. firestore.rules-ஐ deploy செய்யவும்; owner/member provisioning பாதுகாப்பாக செய்யப்பட வேண்டும்.
5. GitHub Actions-ல் APK build இயக்கவும்.

Stock rules:
- quantity Long; பணம் paise Long.
- Negative stock transaction-ல் மறுக்கப்படும்.
- Invoice counter, invoice write, batch deduction ஒரே transaction.
- Hard-coded bill/purchase count limit இல்லை; production list screens-க்கு cursor pagination தேவை.
- பழைய Admin repository/data மாற்றப்படாது.

இந்த project இங்கு Android SDK build மூலம் compile செய்து சோதிக்கப்படவில்லை. GitHub Actions build முடிந்த பிறகே compile வெற்றி உறுதி செய்யலாம்.
