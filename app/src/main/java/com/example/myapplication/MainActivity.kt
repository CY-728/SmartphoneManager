package com.example.myapplication

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.unit.dp
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.myapplication.ui.theme.MyApplicationTheme


@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 권한 요청
        requestPermissions()

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // 앱 시작 화면 텍스트 추가
                    StartScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        // 권한 요청 후 일정 시간 후 MainScreenActivity로 이동
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainScreenActivity::class.java)
            startActivity(intent)
            finish()  // 현재 Activity 종료
        }, 5000) // 5초 후 MainScreenActivity로 이동
    }

    // "다른 앱 위에 그리기"와 "알림" 권한 요청
    private fun requestPermissions() {
        // "다른 앱 위에 그리기" 권한 확인
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            startActivityForResult(intent, 123)
        }

        // 알림 권한 요청 (Android 13 이상에서만)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    // 권한 요청 결과 처리
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}



@Composable
fun StartScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_start2),
            contentDescription = "시작 화면 이미지",
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }

}



@Preview(showBackground = true)
@Composable
fun StartScreenPreview() {
    MyApplicationTheme {
        StartScreen()
    }
}


