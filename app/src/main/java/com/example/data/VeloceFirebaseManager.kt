package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.database.SportActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VeloceUser(
    val uid: String,
    val email: String,
    val displayName: String = ""
)

object VeloceFirebaseManager {
    private const val TAG = "VeloceFirebase"
    
    var isRealFirebaseActive: Boolean = false
        private set

    private val _currentUser = MutableStateFlow<VeloceUser?>(null)
    val currentUser: StateFlow<VeloceUser?> = _currentUser.asStateFlow()

    // Simulated local database for emulation mode
    private val simulatedUsersDb = mutableMapOf<String, Pair<String, String>>() // email -> (password, displayName)
    private val simulatedActivitiesDb = mutableListOf<SportActivity>()

    fun initialize(context: Context) {
        try {
            val app = if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            } else {
                FirebaseApp.getInstance()
            }
            
            if (app != null) {
                isRealFirebaseActive = true
                Log.d(TAG, "Real Firebase initialized successfully!")
                
                // Set up auth state listener
                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        _currentUser.value = VeloceUser(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Athlète"
                        )
                    } else {
                        _currentUser.value = null
                    }
                }
            }
        } catch (e: Exception) {
            isRealFirebaseActive = false
            Log.w(TAG, "Real Firebase not initialized (missing google-services.json). Using emulated Firebase.", e)
            
            // Load saved session from preferences to preserve user logins
            val prefs = context.getSharedPreferences("veloce_firebase_sim", Context.MODE_PRIVATE)
            val savedUid = prefs.getString("current_uid", null)
            val savedEmail = prefs.getString("current_email", null)
            val savedName = prefs.getString("current_name", null)
            if (savedUid != null && savedEmail != null) {
                _currentUser.value = VeloceUser(savedUid, savedEmail, savedName ?: "")
            }
        }
    }

    fun signUp(context: Context, email: String, password: String, displayName: String, onResult: (Result<VeloceUser>) -> Unit) {
        if (isRealFirebaseActive) {
            try {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fUser = task.result?.user
                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setDisplayName(displayName)
                                .build()
                            
                            fUser?.updateProfile(profileUpdates)
                                ?.addOnCompleteListener { updateTask ->
                                    val finalUser = VeloceUser(
                                        uid = fUser.uid,
                                        email = email,
                                        displayName = displayName
                                    )
                                    _currentUser.value = finalUser
                                    onResult(Result.success(finalUser))
                                } ?: run {
                                    val finalUser = fUser?.let {
                                        VeloceUser(uid = it.uid, email = email, displayName = displayName)
                                    } ?: VeloceUser("", email, displayName)
                                    _currentUser.value = finalUser
                                    onResult(Result.success(finalUser))
                                }
                        } else {
                            onResult(Result.failure(task.exception ?: Exception("Erreur d'inscription Firebase Auth")))
                        }
                    }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        } else {
            // Local emulation Mode
            if (email.isBlank() || password.length < 6) {
                onResult(Result.failure(Exception("L'adresse email doit être valide et le mot de passe doit contenir au moins 6 caractères.")))
                return
            }
            
            val prefs = context.getSharedPreferences("veloce_firebase_sim", Context.MODE_PRIVATE)
            if (prefs.contains("user_pwd_$email")) {
                onResult(Result.failure(Exception("Cet email est déjà lié à un profil athlète Veloce.")))
                return
            }

            val uid = "athlete_" + email.replace(".", "_")
            prefs.edit()
                .putString("user_pwd_$email", password)
                .putString("user_name_$email", displayName)
                .putString("current_uid", uid)
                .putString("current_email", email)
                .putString("current_name", displayName)
                .apply()

            val newUser = VeloceUser(uid, email, displayName)
            _currentUser.value = newUser
            onResult(Result.success(newUser))
        }
    }

    fun signIn(context: Context, email: String, password: String, onResult: (Result<VeloceUser>) -> Unit) {
        if (isRealFirebaseActive) {
            try {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val fUser = task.result?.user
                            val finalUser = VeloceUser(
                                uid = fUser?.uid ?: "",
                                email = fUser?.email ?: "",
                                displayName = fUser?.displayName ?: fUser?.email?.substringBefore("@") ?: "Athlète"
                            )
                            _currentUser.value = finalUser
                            onResult(Result.success(finalUser))
                        } else {
                            onResult(Result.failure(task.exception ?: Exception("Erreur de connexion Firebase Auth")))
                        }
                    }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        } else {
            // Local emulation Mode
            val prefs = context.getSharedPreferences("veloce_firebase_sim", Context.MODE_PRIVATE)
            val storedPwd = prefs.getString("user_pwd_$email", null)
            val storedName = prefs.getString("user_name_$email", "Athlète Élite") ?: "Athlète Élite"
            
            if (storedPwd == null) {
                onResult(Result.failure(Exception("Aucun compte trouvé avec cet email. Veuillez créer un compte.")))
                return
            }
            if (storedPwd != password) {
                onResult(Result.failure(Exception("Mot de passe incorrect. Veuillez réessayer.")))
                return
            }

            val uid = "athlete_" + email.replace(".", "_")
            prefs.edit()
                .putString("current_uid", uid)
                .putString("current_email", email)
                .putString("current_name", storedName)
                .apply()

            val loggedInUser = VeloceUser(uid, email, storedName)
            _currentUser.value = loggedInUser
            onResult(Result.success(loggedInUser))
        }
    }

    fun signOut(context: Context) {
        if (isRealFirebaseActive) {
            try {
                FirebaseAuth.getInstance().signOut()
                _currentUser.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out", e)
            }
        } else {
            _currentUser.value = null
            val prefs = context.getSharedPreferences("veloce_firebase_sim", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("current_uid")
                .remove("current_email")
                .remove("current_name")
                .apply()
        }
    }

    // --- Firestore Activity CRUD Sync ---

    fun saveActivityToFirestore(context: Context, activity: SportActivity, onResult: (Result<Unit>) -> Unit = {}) {
        val user = _currentUser.value ?: run {
            onResult(Result.failure(Exception("Athlète non authentifié")))
            return
        }

        if (isRealFirebaseActive) {
            try {
                val db = FirebaseFirestore.getInstance()
                val docData = mapOf(
                    "id" to activity.id,
                    "activityType" to activity.activityType,
                    "startTime" to activity.startTime,
                    "durationMs" to activity.durationMs,
                    "distanceMeters" to activity.distanceMeters,
                    "calories" to activity.calories,
                    "elevationGain" to activity.elevationGain,
                    "title" to activity.title,
                    "notes" to activity.notes,
                    "isSynced" to activity.isSynced,
                    "verificationMessage" to activity.verificationMessage
                )
                db.collection("users")
                    .document(user.uid)
                    .collection("activities")
                    .document(activity.id.toString())
                    .set(docData)
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        onResult(Result.failure(e))
                    }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        } else {
            // Emulated database storage
            simulatedActivitiesDb.removeAll { it.id == activity.id }
            simulatedActivitiesDb.add(activity.copy(userId = user.uid))
            onResult(Result.success(Unit))
        }
    }

    fun deleteActivityFromFirestore(context: Context, activityId: Int, onResult: (Result<Unit>) -> Unit = {}) {
        val user = _currentUser.value ?: run {
            onResult(Result.failure(Exception("Athlète non authentifié")))
            return
        }

        if (isRealFirebaseActive) {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("users")
                    .document(user.uid)
                    .collection("activities")
                    .document(activityId.toString())
                    .delete()
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener { e ->
                        onResult(Result.failure(e))
                    }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        } else {
            simulatedActivitiesDb.removeAll { it.id == activityId }
            onResult(Result.success(Unit))
        }
    }
}
