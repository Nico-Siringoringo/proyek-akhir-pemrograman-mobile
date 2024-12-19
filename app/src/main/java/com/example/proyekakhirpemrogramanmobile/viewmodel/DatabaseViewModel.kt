package com.example.proyekakhirpemrogramanmobile.viewmodel

import android.icu.util.Calendar
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.proyekakhirpemrogramanmobile.data.model.AnnouncementModel
import com.example.proyekakhirpemrogramanmobile.data.model.LectureModel
import com.example.proyekakhirpemrogramanmobile.data.model.TaskModel
import com.example.proyekakhirpemrogramanmobile.data.model.UserModel
import com.example.proyekakhirpemrogramanmobile.data.model.CourseModel
import com.example.proyekakhirpemrogramanmobile.data.model.LectureStatus
import com.example.proyekakhirpemrogramanmobile.data.model.ModuleModel
import com.example.proyekakhirpemrogramanmobile.util.formatDate
import com.example.proyekakhirpemrogramanmobile.util.formatDateForId
import com.example.proyekakhirpemrogramanmobile.util.formatDay
import com.example.proyekakhirpemrogramanmobile.util.formatText
import com.example.proyekakhirpemrogramanmobile.util.formatTime
import com.example.proyekakhirpemrogramanmobile.util.getCurrentMilliseconds
import com.example.proyekakhirpemrogramanmobile.util.getFirstLetter
import com.example.proyekakhirpemrogramanmobile.util.getFirstLetters
import com.example.proyekakhirpemrogramanmobile.util.getFirstWord
import com.example.proyekakhirpemrogramanmobile.util.parseDateAndTime
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseViewModel : ViewModel() {

    private val database: FirebaseFirestore = Firebase.firestore
    private val userReference = database.collection("user")
    private val lectureReference = database.collection("lecture")
    private val courseReference = database.collection("course")
    private val taskReference = database.collection("task")
    private val moduleReference = database.collection("module")
    private val announcementReference = database.collection("announcement")

    private var _userState = MutableStateFlow<UserModel?>(null)
    val userState: StateFlow<UserModel?> = _userState.asStateFlow()

    private var _lectureState = MutableStateFlow<List<LectureModel>>(emptyList())
    val lectureState: StateFlow<List<LectureModel>> = _lectureState.asStateFlow()

    private var _courseState = MutableStateFlow<List<CourseModel>>(emptyList())
    val courseState: StateFlow<List<CourseModel>> = _courseState.asStateFlow()

    private var _taskState = MutableStateFlow<List<TaskModel>>(emptyList())
    val taskState: StateFlow<List<TaskModel>> = _taskState.asStateFlow()

    private var _moduleState = MutableStateFlow<List<ModuleModel>>(emptyList())
    val moduleState: StateFlow<List<ModuleModel>> = _moduleState.asStateFlow()

    private var _announcementState = MutableStateFlow<List<AnnouncementModel>>(emptyList())
    val announcementState: StateFlow<List<AnnouncementModel>> = _announcementState.asStateFlow()

    private var _selectedCourseIdState = MutableStateFlow("")
    val selectedCourseIdState: StateFlow<String> = _selectedCourseIdState.asStateFlow()

    private var _selectedTaskIdState = MutableStateFlow("")
    val selectedTaskIdState: StateFlow<String> = _selectedTaskIdState.asStateFlow()

    fun addUserToDatabase(
        userId: String,
        email: String,
        fullName: String,
        studentId: String,
        gender: String,
        showLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val cleanName = formatText(fullName)
        val firstWord = getFirstWord(cleanName)
        val firstLetter = getFirstLetter(cleanName).toString()
        val day = formatDay(getCurrentMilliseconds())
        val date = formatDate(getCurrentMilliseconds())
        val time = formatTime(getCurrentMilliseconds())

        val newUser = UserModel(
            userId = userId,
            email = email,
            gender = gender,
            fullName = cleanName,
            studentId = studentId,
            firstWord = firstWord,
            firstLetter = firstLetter,
            coursesId = emptyList(),
            created = mapOf(
                "day" to day,
                "date" to date,
                "time" to time,
            ),
        )

        showLoading(true)
        userReference
            .document(userId)
            .set(newUser)
            .addOnSuccessListener {
                showLoading(false)
                _userState.value = newUser
                getAllData()
                onSuccess()
            }
            .addOnFailureListener {
                showLoading(false)
                _userState.value = null
                deleteAllData()
                onFailure()
            }
    }

    fun getUserFromDatabase(
        userId: String,
        showLoading: (Boolean) -> Unit,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        showLoading(true)
        userReference
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                _userState.value = document.toObject(UserModel::class.java)
                getAllData()
                onSuccess()
            }
            .addOnFailureListener {
                showLoading(false)
                _userState.value = null
                deleteAllData()
                onFailure()
            }
    }

    fun checkUserFromDatabase(
        userId: String,
        showLoading: (Boolean) -> Unit,
        onUserExist: () -> Unit,
        onUserNotExist: () -> Unit,
        onFailure: () -> Unit
    ) {
        showLoading(true)
        userReference
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    onUserExist()
                } else {
                    onUserNotExist()
                }
            }
            .addOnFailureListener {
                showLoading(false)
                onFailure()
            }
    }

    fun addCourseToDatabase(
        name: String,
        credits: String,
        lecturer: String,
        semester: String,
        year: String,
        major: String,
        faculty: String,
        building: String,
        floor: String,
        room: String,
        startDate: String,
        startTime: String,
        endTime: String,
    ) {
        Log.d("noled", "in database")

        val newCourse = CourseModel(
            courseId = "${getFirstLetters(name)}_${formatDateForId(getCurrentMilliseconds())}",
            courseName = formatText(name),
            credits = credits,
            major = major,
            faculty = faculty,
            leader = _userState.value?.fullName ?: "Unknown User",
            lecturer = lecturer,
            semester = semester,
            year = year,
            amount = mapOf(
                "announcements" to "0",
                "lectures" to "16",
                "modules" to "0",
                "students" to "1",
                "tasks" to "0",
            ),
            created = mapOf(
                "date" to formatDate(getCurrentMilliseconds()),
                "day" to formatDay(getCurrentMilliseconds()),
                "time" to formatTime(getCurrentMilliseconds())
            ),
            location = mapOf(
                "building" to building,
                "floor" to floor,
                "class" to room
            ),
            schedule = mapOf(
                "day" to formatDay(parseDateAndTime("$startDate $startTime")),
                "time" to "$startTime - $endTime"
            ),
        )

        courseReference
            .add(newCourse)
            .addOnSuccessListener {
                Log.d("noled", "success")
                userReference
                    .document(_userState.value!!.userId)
                    .update("coursesId", FieldValue.arrayUnion(newCourse.courseId))

                val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                val calendar = Calendar.getInstance()
                calendar.time = dateFormat.parse(startDate) ?: Date()

                for (i in 1 until 17) {
                    val schedule = mapOf(
                        "date" to formatDate(calendar.timeInMillis),
                        "day" to newCourse.schedule["day"]!!,
                        "time" to newCourse.schedule["time"]!!
                    )

                    addLectureToDatabase(
                        courseId = newCourse.courseId,
                        courseName = newCourse.courseName,
                        number = i.toString(),
                        schedule = schedule,
                        location = newCourse.location
                    )

                    calendar.add(Calendar.DAY_OF_MONTH, 7)
                    if (i == 8) {
                        calendar.add(Calendar.DAY_OF_MONTH, 7)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.d("noled", e.toString())
            }
    }

    private fun addLectureToDatabase(
        courseId: String,
        courseName: String,
        number: String,
        schedule: Map<String, String>,
        location: Map<String, String>,
    ) {
        Log.d("noled", "lecture called")
        val newLecture = LectureModel(
            courseId = courseId,
            courseName = courseName,
            number = number,
            notes = "Tidak ada konfirmasi perkuliahan dari dosen. Tapi tetap masuk sesuai dengan jadwal.",
            summary = "Admin masih belum menambahkan rangkuman perkuliahan pada pertemuan ini.",
            status = LectureStatus.UNKNOWN.name,
            schedule = schedule,
            location = location,
        )

        lectureReference.add(newLecture)
    }

    private fun getLectureData() {
        _userState.value?.coursesId?.let {
            lectureReference
                .whereIn("courseId", it)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        _lectureState.value = snapshot?.toObjects(LectureModel::class.java) ?: emptyList()
                    } else {
                        _lectureState.value = emptyList()
                    }
                }
        }
    }

    private fun getCourseData() {
        _userState.value?.coursesId?.let {
            courseReference
                .whereIn("courseId", it)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        _courseState.value = snapshot?.toObjects(CourseModel::class.java) ?: emptyList()
                    } else {
                        _courseState.value = emptyList()
                    }
                }
        }
    }

    private fun getTaskData() {
        _userState.value?.coursesId?.let {
            taskReference
                .whereIn("courseId", it)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        _taskState.value = snapshot?.toObjects(TaskModel::class.java) ?: emptyList()
                    } else {
                        _taskState.value = emptyList()
                    }
                }
        }
    }

    private fun getModuleData() {
        _userState.value?.coursesId?.let {
            moduleReference
                .whereIn("courseId", it)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        _moduleState.value = snapshot?.toObjects(ModuleModel::class.java) ?: emptyList()
                    } else {
                        _moduleState.value = emptyList()
                    }
                }
        }
    }

    private fun getAnnouncementData() {
        _userState.value?.coursesId?.let {
            announcementReference
                .whereIn("courseId", it)
                .addSnapshotListener { snapshot, e ->
                    if (e == null) {
                        _announcementState.value = snapshot?.toObjects(AnnouncementModel::class.java) ?: emptyList()
                    } else {
                        _announcementState.value = emptyList()
                    }
                }
        }
    }

    fun setSelectedCourseIdState(courseId: String) {
        _selectedCourseIdState.value = courseId
    }

    fun setSelectedTaskIdState(taskId: String) {
        _selectedTaskIdState.value = taskId
    }

    private fun getAllData() {
        getLectureData()
        getCourseData()
        getTaskData()
        getModuleData()
        getAnnouncementData()
    }

    private fun deleteAllData() {
        _lectureState.value = emptyList()
        _courseState.value = emptyList()
        _taskState.value = emptyList()
        _moduleState.value = emptyList()
        _announcementState.value = emptyList()
        _selectedCourseIdState.value = ""
        _selectedTaskIdState.value = ""
    }

    fun logout() {
        _userState.value = null
        deleteAllData()
    }

}