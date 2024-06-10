package com.example.kleine.viewmodel.quiz

import android.os.CountDownTimer
import android.os.Handler
import android.view.View
import android.widget.Button
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.kleine.R
import com.google.firebase.firestore.FirebaseFirestore

class PlayViewModel(private val materialDocID: String?, private val setID: String?) : ViewModel() {

    var questionList = mutableListOf<String>()
    private var chooseList = mutableListOf<String>()
    private var correctList = mutableListOf<String>()

    private var currentQuestion = 0
    private val _scorePlayer = MutableLiveData<Int>()
    val scorePlayer: LiveData<Int>
        get() = _scorePlayer
    private var isClickBtn = false
    private var valueChoose = ""
    private lateinit var btnClick: Button

    private val _questionText = MutableLiveData<String>()
    val questionText: LiveData<String>
        get() = _questionText

    private val _btnChoice1Text = MutableLiveData<String>()
    val btnChoice1Text: LiveData<String>
        get() = _btnChoice1Text

    private val _btnChoice2Text = MutableLiveData<String>()
    val btnChoice2Text: LiveData<String>
        get() = _btnChoice2Text

    private val _btnChoice3Text = MutableLiveData<String>()
    val btnChoice3Text: LiveData<String>
        get() = _btnChoice3Text

    private val _btnChoice4Text = MutableLiveData<String>()
    val btnChoice4Text: LiveData<String>
        get() = _btnChoice4Text

    private val _cptQuestionText = MutableLiveData<String>()
    val cptQuestionText: LiveData<String>
        get() = _cptQuestionText

    private val _btnBackground = MutableLiveData<Int>(R.drawable.background_btn_choose)
    val btnBackground: LiveData<Int>
        get() = _btnBackground

    private val _navigateToResult = MutableLiveData<Boolean>()
    val navigateToResult: LiveData<Boolean>
        get() = _navigateToResult

    private val _toastMessage = MutableLiveData<String>()
    val toastMessage: LiveData<String>
        get() = _toastMessage

    private val _remainingTime = MutableLiveData<String>()
    val remainingTime: LiveData<String>
        get() = _remainingTime

    private val _showDialogEvent = MutableLiveData<Boolean>()
    val showDialogEvent: LiveData<Boolean>
        get() = _showDialogEvent

    private val _showToastMsg = MutableLiveData<Boolean>()
    val showToastMsg: LiveData<Boolean>
        get() = _showToastMsg

    private lateinit var timer: CountDownTimer

    private val db = FirebaseFirestore.getInstance()

    init {
        _scorePlayer.value = 0
        // Check if materialDocID and setID are not null before fetching questions
        if(materialDocID != null && setID != null) {
            fetchQuestions(materialDocID, setID)
        }
    }

    private fun fetchQuestions(materialID: String, setID: String) {
        db.collection("Materials")
            .document(materialID)
            .collection("Sets")
            .document(setID)
            .collection("Questions")
            .get()
            .addOnSuccessListener { querySnapshot ->
                // Clear old data
                questionList.clear()
                chooseList.clear()
                correctList.clear()

                // Populate new data
                for (document in querySnapshot.documents) {
                    val questionText = document.getString("questionText") ?: ""
                    questionList.add(questionText)

                    val optionA = document.getString("optionA") ?: ""
                    val optionB = document.getString("optionB") ?: ""
                    val optionC = document.getString("optionC") ?: ""
                    val optionD = document.getString("optionD") ?: ""
                    chooseList.addAll(listOf(optionA, optionB, optionC, optionD))

                    val correctAnswerOption = document.getString("correctAnswer") ?: ""
                    // Get the actual correct answer based on the option label
                    val correctAnswer = document.getString(correctAnswerOption) ?: ""
                    correctList.add(correctAnswer)
                }

                // Check if data exists before proceeding
                if (questionList.isNotEmpty() && chooseList.isNotEmpty() && correctList.isNotEmpty()) {
                    fillData()
                    startTimer()
                }
            }
            .addOnFailureListener { exception ->
                // Handle any errors here
            }
    }

    private fun startTimer() {
        timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                _remainingTime.value = (millisUntilFinished / 1000).toString()
            }

            override fun onFinish() {
                // Handle time's up
                _showDialogEvent.value = true
            }
        }.start()
    }

    // don't forget to cancel the timer when needed
    override fun onCleared() {
        super.onCleared()
        timer.cancel()
    }

    fun nextQuestion() {
        _showToastMsg.value = true

        if (isClickBtn) {
            isClickBtn = false
            timer.cancel()

            if (valueChoose != correctList[currentQuestion]) {
                _toastMessage.value = "wrong"
                btnClick.setBackgroundResource(R.drawable.background_btn_erreur)
            } else {
                _toastMessage.value = "correct"
                btnClick.setBackgroundResource(R.drawable.background_btn_correct)
                _scorePlayer.value = _scorePlayer.value?.plus(1)
            }

            Handler().postDelayed({
                if (currentQuestion != questionList.size - 1) {
                    currentQuestion++
                    fillData()
                    valueChoose = ""
                    _btnBackground.value = R.drawable.background_btn_choose
                    startTimer()
                } else {
                    // Navigate to ResultActivity using navigation component
                    _navigateToResult.value = true
                }
            }, 2000)
        } else {
            _toastMessage.value = "Please select your answer"
        }
    }

    private fun fillData() {
        _cptQuestionText.value = "${currentQuestion + 1}/${questionList.size}"
        _questionText.value = questionList[currentQuestion]

        _btnChoice1Text.value = chooseList[4 * currentQuestion]
        _btnChoice2Text.value = chooseList[4 * currentQuestion + 1]
        _btnChoice3Text.value = chooseList[4 * currentQuestion + 2]
        _btnChoice4Text.value = chooseList[4 * currentQuestion + 3]
    }

    fun clickChoose(view: View) {
        btnClick = view as Button

        if (isClickBtn) {
            _btnBackground.value = R.drawable.background_btn_choose
        }

        chooseBtn()
    }

    private fun chooseBtn() {
        btnClick.setBackgroundResource(R.drawable.background_btn_choose_color)
        isClickBtn = true
        valueChoose = btnClick.text.toString()
    }

    fun resetShowToastMsg() {
        _showToastMsg.value = false
    }

}
