package com.example.myapplication

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import com.example.myapplication.ui.theme.MyApplicationTheme
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.PixelFormat
import android.widget.LinearLayout
import androidx.compose.foundation.clickable
import androidx.core.app.NotificationCompat
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog



@Suppress("DEPRECATION")
class MainScreenActivity : ComponentActivity() {

    private lateinit var screenReceiver: BroadcastReceiver
    private val handler = Handler(Looper.getMainLooper())
    private var totalUsageTimeTextView: TextView? = null
    private var screenOnCountTextView: TextView? = null
    private var alertMessageTextView: TextView? = null // 알림 메시지를 표시할 텍스트 뷰 추가
    private var totalSeconds: Long = 0 // 총 사용 시간 (초 단위)
    private var screenOnCount: Int = 0 // 화면 켜짐 횟수
    private var overlayView: View? = null // 오버레이 뷰를 변수로 저장
    private var isCatOverlay: Boolean = false
    private var isDogOverlay: Boolean = false
    private var isNotificationSent1 = false
    private var isNotificationSent2 = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 잠금 화면에서도 이 Activity가 나타날 수 있도록 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        // BroadcastReceiver를 등록하여 화면 이벤트를 감지
        registerScreenReceiver()

        // 사용 기록 접근 권한 요청
        if (!checkUsageStatsPermission()) {
            requestUsageStatsPermission()
        }

        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // BroadcastReceiver 해제
        unregisterReceiver(screenReceiver)

        // 오버레이 뷰 제거
        overlayView?.let {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(it)
        }
    }

    private fun registerScreenReceiver() {
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_ON) {
                    showCatOverlay()
                    showDogOverlay()
                    screenOnCount++  // 화면 켜짐 횟수 증가
                    updateScreenOnCount()  // 화면 켜짐 횟수 갱신
                    isNotificationSent1 = false
                    isNotificationSent2 = false
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        registerReceiver(screenReceiver, filter)
    }

    @SuppressLint("InflateParams", "MissingInflatedId")
    fun showCatOverlay() {
        if (isCatOverlay || isDogOverlay) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        }

        // 오버레이 뷰를 새로 생성하고 변수에 저장
        overlayView = LayoutInflater.from(this).inflate(R.layout.cat_overlay_layout, null)

        totalUsageTimeTextView = overlayView?.findViewById(R.id.total_usage_time_text)
        screenOnCountTextView = overlayView?.findViewById(R.id.screen_on_count_text)
        alertMessageTextView = overlayView?.findViewById(R.id.alert_message_text) // 알림 메시지 텍스트 뷰 참조 추가
        updateUsageTime()

        val closeButton: Button = overlayView?.findViewById(R.id.close_button) ?: return
        closeButton.visibility = View.INVISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            closeButton.visibility = View.VISIBLE  // 버튼을 보이도록 설정
            closeButton.alpha = 0f  // 처음에는 투명하게 설정 (alpha=0)

            // alpha 애니메이션 추가 (서서히 나타나게)
            val animator = ObjectAnimator.ofFloat(closeButton, "alpha", 0f, 1f)
            animator.duration = 3000 // 3초 동안 애니메이션 진행
            animator.start()
        }, 5000) // 5초 후에 애니메이션 시작


        closeButton.setOnClickListener {
            //오버레이 제거
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(overlayView)
            overlayView = null
            isCatOverlay=false
            handler.removeCallbacksAndMessages(null)
        }

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, layoutParams)

        isCatOverlay=true
    }

    private fun updateUsageTime() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                totalSeconds++
                val formattedTime = formatTime(totalSeconds)
                //totalUsageTimeTextView?.text = "오늘의 총 사용 시간: $formattedTime"


                if (totalSeconds >= 60 && totalSeconds < 70 && !isNotificationSent1) {
                    sendNotification(this@MainScreenActivity, "너무 많이 쓴거 아니야?")
                    isNotificationSent1 = true // 알림을 보냈으므로 상태를 변경
                } else if (totalSeconds >= 70 && totalSeconds<80 && !isNotificationSent2) {
                    sendNotification(this@MainScreenActivity, "조금만 참아보자!")
                    isNotificationSent2 = true // 알림을 보냈으므로 상태를 변경
                }


                // 알림 메시지 갱신
                if(isCatOverlay){
                    showCatAlertMessage()
                    totalUsageTimeTextView?.text = "오늘의 총 사용 시간은 \n $formattedTime 다냥!"

                } else if (isDogOverlay){
                    showDogAlertMessage()
                    totalUsageTimeTextView?.text = "오늘의 총 사용 시간은 \n $formattedTime 다멍!"
                }


                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun updateScreenOnCount() {
        screenOnCountTextView?.text = "화면 켜짐 횟수: $screenOnCount 회"
    }

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d시간 %02d분 %02d초", hours, minutes, remainingSeconds)
    }

    @SuppressLint("InflateParams")
    private fun showCatAlertMessage() {
        val alertMessage: String
        val catImageResId: Int
        val backgroundDrawableResId: Int

        when {
            totalSeconds >= 60 -> { // 2시간 이상
                alertMessage = "...더 쓸거라고? 너무 심하다냥!"
                catImageResId = R.drawable.ic_cat4
                backgroundDrawableResId = R.drawable.angryBackground  // 분홍색 배경
            }
            totalSeconds >= 30 -> { // 1시간 30분 이상
                alertMessage = "너무 많이 쓰는거 아니냥? 슬프다냥ㅜㅜ"
                catImageResId = R.drawable.ic_cat6
                backgroundDrawableResId = R.drawable.sadBackground  // 파란색 배경
            }
            totalSeconds >= 10 -> { // 1시간 이상
                alertMessage = "너 오늘 ${formatTime(totalSeconds)} 썼다냥. 주의하라냥!"
                catImageResId = R.drawable.ic_cat7
                backgroundDrawableResId = R.drawable.warningBackground  // 노란색 배경
            }
            else -> {
                alertMessage = "현재 총 사용 시간은 ${formatTime(totalSeconds)}라냥!"
                catImageResId = R.drawable.ic_cat5
                backgroundDrawableResId = R.drawable.normalBackground  // 흰색 배경
            }
        }

        // 알림 메시지가 있을 때만 화면에 표시
        alertMessageTextView?.text = alertMessage

        // 이미지 업데이트
        val catImageView = overlayView?.findViewById<ImageView>(R.id.cat_image_view)
        catImageView?.setImageResource(catImageResId)

        // 배경 색상 업데이트
        val backgroundView = overlayView?.findViewById<LinearLayout>(R.id.overlay_background)
        backgroundView?.setBackgroundResource(backgroundDrawableResId)
    }


    private fun checkUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    // 강아지 화면 오버레이 구현
    @SuppressLint("InflateParams", "MissingInflatedId")
    fun showDogOverlay() {
        if (isCatOverlay || isDogOverlay) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        // 오버레이 뷰를 새로 생성하고 변수에 저장
        overlayView = LayoutInflater.from(this).inflate(R.layout.dog_overlay_layout, null)

        totalUsageTimeTextView = overlayView?.findViewById(R.id.total_usage_time_text)
        screenOnCountTextView = overlayView?.findViewById(R.id.screen_on_count_text)
        alertMessageTextView = overlayView?.findViewById(R.id.alert_message_text) // 알림 메시지 텍스트 뷰 참조 추가
        updateUsageTime()

        val closeButton: Button = overlayView?.findViewById(R.id.close_button) ?: return
        closeButton.setOnClickListener {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.removeView(overlayView)
            overlayView = null
            isDogOverlay=false
            handler.removeCallbacksAndMessages(null)
        }

        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, layoutParams)

        isDogOverlay=true
    }

    fun sendNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "usage_alert_channel"

        // Notification Channel 설정 (Android 8.0 이상)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Usage Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // 알림 설정
        val notification: Notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("나만의 스마트폰 매니저")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // 알림 보내기
        notificationManager.notify(1, notification)
    }

    @SuppressLint("InflateParams")
    private fun showDogAlertMessage() {
        val alertMessage: String
        val dogImageResId: Int
        val backgroundDrawableResId: Int

        when {
            totalSeconds >= 60 -> { // 2시간 이상
                alertMessage="더는 못참아! 당장 그만 두지 못해 멍!"
                dogImageResId=R.drawable.ic_dog_angry3
                backgroundDrawableResId = R.drawable.angryBackground
            }
            totalSeconds >= 30 -> { // 1시간 30분 이상
                alertMessage="이게 무슨 일이냐 멍...? 날 실망 시키는 거냐?"
                dogImageResId=R.drawable.ic_dog_angry
                backgroundDrawableResId = R.drawable.sadBackground
            }
            totalSeconds >= 10 -> { // 1시간 이상
                alertMessage="잠깐만! ${formatTime(totalSeconds)} 썼다멍! 참아 보자멍!"
                dogImageResId=R.drawable.ic_dog_angry2
                backgroundDrawableResId = R.drawable.warningBackground
            }
            else -> {
                alertMessage = "현재 총 사용 시간은 ${formatTime(totalSeconds)}다멍!"
                dogImageResId = R.drawable.ic_dog1
                backgroundDrawableResId = R.drawable.normalBackground
            }
        }

        // 알림 메시지가 있을 때만 화면에 표시
        alertMessageTextView?.text = alertMessage

        // 이미지 업데이트
        val dogImageView = overlayView?.findViewById<ImageView>(R.id.dog_image_view)
        dogImageView?.setImageResource(dogImageResId)

        val backgroundView = overlayView?.findViewById<LinearLayout>(R.id.overlay_background)
        backgroundView?.setBackgroundResource(backgroundDrawableResId)
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var showCatOverlay by remember { mutableStateOf(false) }
    var showDogOverlay by remember { mutableStateOf(false) }
    var showIntermediateScreen by remember { mutableStateOf(false) }
    var selectedFriend by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // "함께할 친구를 선택해주세요!" 텍스트 추가
        Text(
            text = "함께할 친구를 선택해주세요!",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp), // 텍스트와 이미지 간의 간격 설정
            color = MaterialTheme.colorScheme.primary // 텍스트 색상 설정 (필요 시)
        )

        Text(
            text = "사진을 눌러 친구들의 성격을 알아보세요.",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 32.dp), // 텍스트와 이미지 간의 간격 설정
            color = MaterialTheme.colorScheme.primary // 텍스트 색상 설정 (필요 시)
        )

        // 고양이와 바둑이 이미지 나란히 배치
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp), // 이미지 사이 간격
            horizontalArrangement = Arrangement.Center
        ) {
            // 고양이 이미지
            Image(
                painter = painterResource(id = R.drawable.ic_cat5), // 고양이 이미지 리소스
                contentDescription = "치즈냥",
                modifier = Modifier
                    .size(200.dp) // 이미지 크기 조정
                    .padding(end = 16.dp) // 이미지 사이 간격
                    .clickable { showCatOverlay=true }
            )

            // 바둑이 이미지
            Image(
                painter = painterResource(id = R.drawable.ic_dog1), // 바둑이 이미지 리소스
                contentDescription = "바둑이",
                modifier = Modifier
                    .size(200.dp) // 이미지 크기 조정
                    .clickable { showDogOverlay=true }
            )
        }


        // 버튼을 나란히 배치
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            // 고양이 버튼
            Button(
                onClick = {
                    if (context is MainScreenActivity) {
                        context.showCatOverlay()
                    }
                },
                modifier = Modifier
                    .padding(end = 16.dp) // 버튼 사이 간격
                    .weight(1f) // 버튼을 같은 크기로 배치
            ) {
                Text(text = "귀여운 치즈냥")
            }

            // 바둑이 버튼
            Button(
                onClick = {
                    if (context is MainScreenActivity) {
                        context.showDogOverlay()
                    }
                },
                modifier = Modifier
                    .padding(start = 16.dp) // 버튼 사이 간격
                    .weight(1f) // 버튼을 같은 크기로 배치
            ) {
                Text(text = "나이스한 바둑이")
            }
        }
    }

    if (showIntermediateScreen) {
        IntermediateScreen(
            friendName = selectedFriend,
            onDismiss = { showIntermediateScreen = false },
            onProceed = {
                showIntermediateScreen = false
                if (context is MainScreenActivity) {
                    when (selectedFriend) {
                        "치즈냥" -> context.showCatOverlay()
                        "바둑이" -> context.showDogOverlay()
                    }
                }
            }
        )
    }

    if (showCatOverlay) {
        OverlayDialog(
            title = "치즈냥의 성격",
            description = "까칠해보이지만 누구보다도 당신을 생각하는 치즈냥. 생각보다 따뜻하게 당신을 대해줄 수도 있습니다. 스마트폰을 조금만 쓴다는 전제 하에 말이죠!",
            onDismiss = { showCatOverlay = false }
        )
    }

    // 강아지 성격 오버레이
    if (showDogOverlay) {
        OverlayDialog(
            title = "바둑이의 성격",
            description = "무조건적인 이해를 주는 바둑이. 그렇지만 또 몰라요, 많이 쓰면 쓸수록 바둑이의 숨겨진 면모가 드러나게 될지도…!",
            onDismiss = { showDogOverlay = false }
        )
    }
}

@Composable
fun IntermediateScreen(friendName: String, onDismiss: () -> Unit, onProceed: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
                    .fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$friendName",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "스마트폰 중독을 예방해 봐요!",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 24.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = onDismiss) {
                        Text(text = "취소")
                    }
                    Button(onClick = onProceed) {
                        Text(text = "확인")
                    }
                }
            }
        }
    }
}

@Composable
fun OverlayDialog(title: String, description: String, onDismiss: () -> Unit) {
    // Dialog로 오버레이 화면 구현
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .background(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp)
                    .fillMaxWidth(0.8f), // 2/3 크기
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(onClick = onDismiss) {
                    Text(text = "닫기")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MyApplicationTheme {
        MainScreen()
    }
}