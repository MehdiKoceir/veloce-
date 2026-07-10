package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalorieCalculator
import com.example.data.CalorieCalculator.ActivityType
import com.example.data.CalorieCalculator.TrackPoint
import com.example.data.VeloceRepository
import com.example.data.VeloceFirebaseManager
import com.example.data.VeloceUser
import com.example.data.database.SocialFeedItem
import com.example.data.database.SportActivity
import com.example.data.database.UserProfile
import com.google.android.gms.location.*
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.abs

class VeloceViewModel(private val repository: VeloceRepository, private val context: Context) : ViewModel() {

    // --- Tab Navigation State ---
    enum class AppTab {
        FEED, TRACKING, HISTORY, PROFILE
    }
    
    private val _currentTab = MutableStateFlow(AppTab.TRACKING)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- Dynamic Dark / Light Professional Theme State ---
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // --- Welcome Landing Screen State ---
    private val sharedPrefs = context.getSharedPreferences("veloce_prefs", Context.MODE_PRIVATE)
    private val _hasSeenWelcome = MutableStateFlow(sharedPrefs.getBoolean("has_seen_welcome", false))
    val hasSeenWelcome: StateFlow<Boolean> = _hasSeenWelcome.asStateFlow()

    fun setSeenWelcome(seen: Boolean) {
        _hasSeenWelcome.value = seen
        sharedPrefs.edit().putBoolean("has_seen_welcome", seen).apply()
    }

    // --- Firebase Authentication States & Flows ---
    val firebaseUser: StateFlow<VeloceUser?> = VeloceFirebaseManager.currentUser
    val isRealFirebaseActive: Boolean = VeloceFirebaseManager.isRealFirebaseActive

    fun signUp(email: String, password: String, displayName: String, onResult: (Result<VeloceUser>) -> Unit) {
        viewModelScope.launch {
            VeloceFirebaseManager.signUp(context, email, password, displayName) { result ->
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        viewModelScope.launch {
                            // Update local room profile with authenticated athlete's name
                            val current = repository.getProfileOneShot()
                            repository.updateProfile(current.copy(name = user.displayName))
                        }
                    }
                }
                onResult(result)
            }
        }
    }

    fun signIn(email: String, password: String, onResult: (Result<VeloceUser>) -> Unit) {
        viewModelScope.launch {
            VeloceFirebaseManager.signIn(context, email, password) { result ->
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    if (user != null) {
                        viewModelScope.launch {
                            // Update local room profile with authenticated athlete's name
                            val current = repository.getProfileOneShot()
                            repository.updateProfile(current.copy(name = user.displayName))
                        }
                    }
                }
                onResult(result)
            }
        }
    }

    fun signOut() {
        VeloceFirebaseManager.signOut(context)
    }

    // --- Onboarding / Profile State ---
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(name: String, weight: Double, height: Double, age: Int, gender: String, activityLevel: String, metric: Boolean) {
        viewModelScope.launch {
            val current = repository.getProfileOneShot()
            repository.updateProfile(current.copy(
                name = name,
                weightKg = weight,
                heightCm = height,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                metricUnits = metric
            ))
        }
    }

    // --- Tracking Engine States ---
    enum class TrackingState {
        IDLE, RECORDING, PAUSED
    }

    private val _trackingState = MutableStateFlow(TrackingState.IDLE)
    val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    private val _selectedActivityType = MutableStateFlow(ActivityType.RUNNING)
    val selectedActivityType: StateFlow<ActivityType> = _selectedActivityType.asStateFlow()

    private val _useSimulation = MutableStateFlow(false) // Default to false so real tracking is used by default
    val useSimulation: StateFlow<Boolean> = _useSimulation.asStateFlow()

    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var stepDetector: Sensor? = null
    private var sensorEventListener: SensorEventListener? = null
    private var lastMovementTime = System.currentTimeMillis()

    // Active workout metrics
    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _distanceMeters = MutableStateFlow(0.0)
    val distanceMeters: StateFlow<Double> = _distanceMeters.asStateFlow()

    private val _elevationGainMeters = MutableStateFlow(0.0)
    val elevationGainMeters: StateFlow<Double> = _elevationGainMeters.asStateFlow()

    private val _caloriesBurned = MutableStateFlow(0.0)
    val caloriesBurned: StateFlow<Double> = _caloriesBurned.asStateFlow()

    private val _trackedPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val trackedPoints: StateFlow<List<TrackPoint>> = _trackedPoints.asStateFlow()

    private val _avgSpeedKmH = MutableStateFlow(0.0)
    val avgSpeedKmH: StateFlow<Double> = _avgSpeedKmH.asStateFlow()

    fun selectActivityType(type: ActivityType) {
        if (_trackingState.value == TrackingState.IDLE) {
            _selectedActivityType.value = type
        }
    }

    fun toggleSimulation(enabled: Boolean) {
        _useSimulation.value = enabled
    }

    // Location & Tickers
    private var trackingJob: Job? = null
    private var locationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    // Base simulation coordinate (Paris Tour Eiffel as start)
    private var simLat = 48.8584
    private var simLng = 2.2945
    private var simAlt = 35.0
    private var simHeadingAngle = 0.0

    fun startTracking() {
        if (_trackingState.value == TrackingState.IDLE) {
            // Reset metrics
            _durationSeconds.value = 0L
            _distanceMeters.value = 0.0
            _elevationGainMeters.value = 0.0
            _caloriesBurned.value = 0.0
            _trackedPoints.value = emptyList()
            _avgSpeedKmH.value = 0.0

            // Initialize simulation points randomly near Paris/Marseille/Algiers for variety
            val locations = listOf(
                Pair(48.8584, 2.2945), // Paris
                Pair(43.2965, 5.3698), // Marseille Vieux Port
                Pair(36.7538, 3.0588)  // Algiers
            )
            val baseLoc = locations.random()
            simLat = baseLoc.first
            simLng = baseLoc.second
            simAlt = 50.0
            simHeadingAngle = Math.random() * 2 * Math.PI
        }

        _trackingState.value = TrackingState.RECORDING

        // Start core updates
        startTrackingPipeline()
    }

    fun pauseTracking() {
        _trackingState.value = TrackingState.PAUSED
        stopTrackingPipelineJobs()
    }

    fun resumeTracking() {
        _trackingState.value = TrackingState.RECORDING
        startTrackingPipeline()
    }

    private fun startSensorUpdates() {
        if (sensorManager == null) return
        
        // If simulation mode is active, we bypass sensor checks (always simulate movement)
        if (_useSimulation.value) {
            _isMoving.value = true
            return
        }

        _isMoving.value = false
        lastMovementTime = System.currentTimeMillis()

        sensorEventListener = object : SensorEventListener {
            private val SMOOTHING_FACTOR = 0.9f
            private var gravity = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Isolate linear acceleration (remove gravity using high-pass filter)
                    gravity[0] = SMOOTHING_FACTOR * gravity[0] + (1 - SMOOTHING_FACTOR) * x
                    gravity[1] = SMOOTHING_FACTOR * gravity[1] + (1 - SMOOTHING_FACTOR) * y
                    gravity[2] = SMOOTHING_FACTOR * gravity[2] + (1 - SMOOTHING_FACTOR) * z

                    val linearX = x - gravity[0]
                    val linearY = y - gravity[1]
                    val linearZ = z - gravity[2]

                    val magnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)
                    
                    // Threshold of 0.6f corresponds to gentle physical motion
                    if (magnitude > 0.6f) {
                        lastMovementTime = System.currentTimeMillis()
                        _isMoving.value = true
                    } else {
                        // If no significant movement for 4 seconds, mark as stationary
                        if (System.currentTimeMillis() - lastMovementTime > 4000) {
                            _isMoving.value = false
                        }
                    }
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    lastMovementTime = System.currentTimeMillis()
                    _isMoving.value = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        stepDetector?.let {
            sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun stopSensorUpdates() {
        sensorEventListener?.let {
            sensorManager?.unregisterListener(it)
        }
        sensorEventListener = null
        _isMoving.value = false
    }

    private fun startTrackingPipeline() {
        stopTrackingPipelineJobs()

        // Start motion sensors
        startSensorUpdates()

        // 1. Ticker and/or simulation job
        trackingJob = viewModelScope.launch {
            while (_trackingState.value == TrackingState.RECORDING) {
                delay(1000)
                _durationSeconds.value += 1

                if (_useSimulation.value) {
                    simulateStep()
                }
            }
        }

        // 2. Real GPS updates if simulation is off
        if (!_useSimulation.value) {
            startRealGpsUpdates()
        }
    }

    private fun stopTrackingPipelineJobs() {
        trackingJob?.cancel()
        trackingJob = null
        stopRealGpsUpdates()
        stopSensorUpdates()
    }

    private fun simulateStep() {
        val durationSec = _durationSeconds.value
        val type = _selectedActivityType.value
        
        // Dynamic speed based on activity
        val speedFactor = when (type) {
            ActivityType.WALKING -> 1.4 // ~5 km/h
            ActivityType.RUNNING -> 2.8 // ~10 km/h
            ActivityType.CYCLING -> 6.0 // ~22 km/h
            ActivityType.HIKING -> 1.1 // ~4 km/h
        }

        // Add speed variation (drift)
        val currentSpeedMs = speedFactor + sin(durationSec.toDouble() / 15.0) * (speedFactor * 0.2)
        val currentSpeedKmH = currentSpeedMs * 3.6

        // Compute step distance
        val stepDistance = currentSpeedMs * 1.0 // 1 second step

        // Move coordinate (simple planar approximation)
        simHeadingAngle += (Math.random() - 0.5) * 0.4 // gentle curving path
        val latChange = (stepDistance * cos(simHeadingAngle)) / 111111.0
        val lngChange = (stepDistance * sin(simHeadingAngle)) / (111111.0 * cos(simLat * Math.PI / 180.0))

        simLat += latChange
        simLng += lngChange

        // Elevation changes (climbing a nice dynamic hill)
        val elevationChange = sin(durationSec.toDouble() / 25.0) * 0.6 // up and down
        simAlt += elevationChange
        if (simAlt < 0.0) simAlt = 0.0

        val newPoint = TrackPoint(
            latitude = simLat,
            longitude = simLng,
            altitude = simAlt,
            timestamp = System.currentTimeMillis()
        )

        // Append point
        val currentPoints = _trackedPoints.value.toMutableList()
        currentPoints.add(newPoint)
        _trackedPoints.value = currentPoints

        // Calculate dynamic calorie calculation on the list
        viewModelScope.launch {
            val weight = repository.getProfileOneShot().weightKg
            val totalCal = CalorieCalculator.calculateCalories(currentPoints, weight, type)
            _caloriesBurned.value = totalCal

            // Compute overall metrics
            _distanceMeters.value += stepDistance
            if (elevationChange > 0) {
                _elevationGainMeters.value += elevationChange
            }

            _avgSpeedKmH.value = (_distanceMeters.value / 1000.0) / ((durationSec.toDouble() / 3600.0))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRealGpsUpdates() {
        try {
            if (locationClient == null) {
                locationClient = LocationServices.getFusedLocationProviderClient(context)
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateDistanceMeters(1.5f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    addRealGpsPoint(loc)
                }
            }

            locationClient?.requestLocationUpdates(
                request,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRealGpsUpdates() {
        locationCallback?.let {
            locationClient?.removeLocationUpdates(it)
        }
        locationCallback = null
    }

    private fun addRealGpsPoint(location: Location) {
        // GPS noise/drift filter: ignore low accuracy points (extremely common indoors)
        if (location.hasAccuracy() && location.accuracy > 30f) {
            return
        }

        val newPoint = TrackPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            timestamp = System.currentTimeMillis()
        )

        val currentPoints = _trackedPoints.value.toMutableList()
        val prevPoint = currentPoints.lastOrNull()

        if (prevPoint != null) {
            val dist = CalorieCalculator.calculateDistance(
                prevPoint.latitude, prevPoint.longitude,
                newPoint.latitude, newPoint.longitude
            )

            // Prevent GPS drift: only accumulate if we detect physical motion
            // We consider the user moving if our motion sensor says so, or if reported GPS speed is above 0.3 m/s
            val isGpsMoving = dist >= 1.5 && (_isMoving.value || (location.hasSpeed() && location.speed > 0.3f))

            if (isGpsMoving) {
                currentPoints.add(newPoint)
                _trackedPoints.value = currentPoints

                val elevDiff = newPoint.altitude - prevPoint.altitude

                _distanceMeters.value += dist
                if (elevDiff > 0) {
                    _elevationGainMeters.value += elevDiff
                }

                viewModelScope.launch {
                    val weight = repository.getProfileOneShot().weightKg
                    val totalCal = CalorieCalculator.calculateCalories(currentPoints, weight, _selectedActivityType.value)
                    _caloriesBurned.value = totalCal
                    
                    val durSec = _durationSeconds.value
                    if (durSec > 0) {
                        _avgSpeedKmH.value = (_distanceMeters.value / 1000.0) / (durSec.toDouble() / 3600.0)
                    }
                }
            }
        } else {
            // First point is always added to start the route
            currentPoints.add(newPoint)
            _trackedPoints.value = currentPoints
        }
    }

    /**
     * Complete tracking session, save locally to Room, reset state
     */
    fun saveTrackingSession(title: String, notes: String) {
        stopTrackingPipelineJobs()
        
        val points = _trackedPoints.value
        val distance = _distanceMeters.value
        val duration = _durationSeconds.value * 1000L // to millis
        val calories = _caloriesBurned.value
        val elevation = _elevationGainMeters.value
        val type = _selectedActivityType.value

        viewModelScope.launch {
            val user = VeloceFirebaseManager.currentUser.value
            val activity = SportActivity(
                userId = user?.uid ?: "",
                activityType = type.name,
                startTime = System.currentTimeMillis() - duration,
                durationMs = duration,
                distanceMeters = distance,
                calories = calories,
                elevationGain = elevation,
                title = title.ifBlank { "Session ${activityTypeFrenchName(type)}" },
                notes = notes,
                routePointsJson = SportActivity.serializePoints(points),
                isSynced = false
            )
            
            // 1. Insert to local database (always save locally first)
            val id = repository.insertActivity(activity)
            val finalActivity = activity.copy(id = id.toInt())

            val online = isNetworkAvailable()
            if (online) {
                // 2. Automatically trigger virtual Cloud-Function verification & publish to Social Feed
                repository.syncActivityToCloud(id.toInt())

                // 3. Securely store in Firebase Firestore
                VeloceFirebaseManager.saveActivityToFirestore(context, finalActivity)
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Séance enregistrée et synchronisée avec succès !", Toast.LENGTH_LONG).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Hors-ligne : Séance enregistrée localement. Elle se synchronisera dès le retour d'Internet !", Toast.LENGTH_LONG).show()
                }
            }

            // Reset states
            _trackingState.value = TrackingState.IDLE
            _durationSeconds.value = 0L
            _distanceMeters.value = 0.0
            _elevationGainMeters.value = 0.0
            _caloriesBurned.value = 0.0
            _trackedPoints.value = emptyList()
            _avgSpeedKmH.value = 0.0

            // Switch to History list tab to view recorded workout
            _currentTab.value = AppTab.HISTORY
        }
    }

    fun discardTrackingSession() {
        stopTrackingPipelineJobs()
        _trackingState.value = TrackingState.IDLE
        _durationSeconds.value = 0L
        _distanceMeters.value = 0.0
        _elevationGainMeters.value = 0.0
        _caloriesBurned.value = 0.0
        _trackedPoints.value = emptyList()
        _avgSpeedKmH.value = 0.0
    }

    private fun activityTypeFrenchName(type: ActivityType): String {
        return when (type) {
            ActivityType.RUNNING -> "Course"
            ActivityType.CYCLING -> "Vélo"
            ActivityType.WALKING -> "Marche"
            ActivityType.HIKING -> "Rando"
        }
    }

    // --- History Lists & Stats ---
    val activities: StateFlow<List<SportActivity>> = repository.activities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateActivity(activity: SportActivity) {
        viewModelScope.launch {
            repository.updateActivity(activity)
            VeloceFirebaseManager.saveActivityToFirestore(context, activity)
        }
    }

    fun deleteActivity(id: Int) {
        viewModelScope.launch {
            repository.deleteActivity(id)
            VeloceFirebaseManager.deleteActivityFromFirestore(context, id)
        }
    }

    // --- Social Feed Interactions ---
    val feedItems: StateFlow<List<SocialFeedItem>> = repository.feedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun publishActivityToFeed(activityId: Int, customTitle: String? = null) {
        viewModelScope.launch {
            val success = repository.publishActivityToFeed(activityId, customTitle)
            if (success) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Activité publiée sur le fil d'actualités !", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun toggleKudos(itemId: Int) {
        viewModelScope.launch {
            repository.toggleKudos(itemId)
        }
    }

    fun deleteFeedItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteFeedItem(itemId)
        }
    }

    fun postComment(itemId: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val profile = repository.getProfileOneShot()
            repository.addComment(itemId, profile.name, text)
            
            // Interactive visual surprise: when user comments on Karim or Sarah's workout,
            // they simulate-reply back to the user within a 3-second delay!
            simulateSocialReply(itemId, text)
        }
    }

    private fun simulateSocialReply(itemId: Int, userText: String) {
        viewModelScope.launch {
            delay(3000)
            val feedItem = repository.feedItems.first().find { it.id == itemId } ?: return@launch
            
            val comments = feedItem.getComments()
            // Make sure the last comment was indeed by the user
            if (comments.lastOrNull()?.author == userProfile.value?.name) {
                val replier = feedItem.athleteName
                val replyText = when {
                    "super" in userText.lowercase() || "bravo" in userText.lowercase() -> "Merci beaucoup ! On lâche rien 💪"
                    "route" in userText.lowercase() || "parcours" in userText.lowercase() -> "Oui, le parcours est incroyable ! Je te le conseille vraiment."
                    else -> "Merci pour le commentaire ! À bientôt sur les routes 😉"
                }
                repository.addComment(itemId, replier, replyText)
            }
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return true
                }
            }
        }
        return false
    }

    fun syncOfflineActivities() {
        if (!isNetworkAvailable() || _isSyncing.value) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val allActivities = repository.activities.firstOrNull() ?: emptyList()
                val unsynced = allActivities.filter { !it.isSynced }
                if (unsynced.isNotEmpty()) {
                    var successCount = 0
                    for (activity in unsynced) {
                        // 1. Trigger simulated cloud verification
                        val verificationResult = repository.syncActivityToCloud(activity.id)
                        
                        // 2. Fetch updated activity from Room to save to Firestore
                        val updatedActivity = repository.getActivityById(activity.id)
                        if (updatedActivity != null && verificationResult.isValid) {
                            VeloceFirebaseManager.saveActivityToFirestore(context, updatedActivity)
                            successCount++
                        }
                    }
                    if (successCount > 0) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "$successCount séances synchronisées avec succès !", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTrackingPipelineJobs()
    }

    // --- Initialization & Setup ---
    init {
        VeloceFirebaseManager.initialize(context)
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        stepDetector = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        viewModelScope.launch {
            repository.initializeAppDataIfEmpty()
        }

        // Setup real-time network connectivity monitoring
        _isOnline.value = isNetworkAvailable()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            try {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _isOnline.value = true
                        // Auto-sync offline workouts when internet returns
                        syncOfflineActivities()
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                    }
                })
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class VeloceViewModelFactory(
    private val repository: VeloceRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VeloceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VeloceViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
