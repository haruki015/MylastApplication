package jp.ac.gifu_u.info.ishida.mylastapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

// OnMapReadyCallbackインターフェースを実装
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private CalendarView calendarView;
    private TextView selectedDateText, savedMemo;
    private EditText editMemo;
    private Button saveButton, deleteButton, addBikeLogButton;
    private String selectedDateKey = "";

    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;

    // GoogleMapオブジェクトを保持する変数を追加
    private GoogleMap mMap;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI要素の取得
        calendarView = findViewById(R.id.calendarView);
        selectedDateText = findViewById(R.id.selectedDateText);
        editMemo = findViewById(R.id.editMemo);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);
        addBikeLogButton = findViewById(R.id.addBikeLogButton);
        savedMemo = findViewById(R.id.savedMemo);

        // MapFragmentの取得と非同期コールバックの設定
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // カレンダーの日付選択処理
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            selectedDateKey = year + "-" + (month + 1) + "-" + dayOfMonth;
            selectedDateText.setText("選択日: " + selectedDateKey);

            SharedPreferences prefs = getSharedPreferences("MemoPrefs", MODE_PRIVATE);
            String memo = prefs.getString(selectedDateKey, "");
            savedMemo.setText(memo.isEmpty() ? "保存された記録はありません" : memo);
        });

        // メモ保存ボタン
        saveButton.setOnClickListener(v -> {
            String memo = editMemo.getText().toString().trim();
            if (!selectedDateKey.isEmpty() && !memo.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("MemoPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(selectedDateKey, memo);
                editor.apply();

                savedMemo.setText(memo);
                editMemo.setText("");
                Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
            }
        });

        // メモ削除ボタン
        deleteButton.setOnClickListener(v -> {
            if (!selectedDateKey.isEmpty()) {
                SharedPreferences prefs = getSharedPreferences("MemoPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(selectedDateKey);
                editor.apply();

                savedMemo.setText("記録は削除されました");
                editMemo.setText("");
                Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
            }
        });

        // 自転車記録追加ボタン
        addBikeLogButton.setOnClickListener(v -> {
            if (mMap == null) {
                Toast.makeText(this, "マップが準備できていません", Toast.LENGTH_SHORT).show();
                return;
            }
            if (checkLocationPermission()) {
                addBikeLog();
            }
        });
    }

    // onMapReadyコールバック：マップの準備が完了したときに呼ばれる
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermission();
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // パーミッションがない場合はリクエスト
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        } else {
            // パーミッションがある場合は現在地レイヤーを有効化
            mMap.setMyLocationEnabled(true);
            // 最後の位置情報を取得してカメラを移動
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    lastLocation = location;
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
                }
            });
            return true;
        }
    }

    private void addBikeLog() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return; // このチェックは念のため
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null && !selectedDateKey.isEmpty()) {
                double distanceKm = 0;
                // lastLocationがnullでなければ距離を計算
                if (lastLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            lastLocation.getLatitude(), lastLocation.getLongitude(),
                            location.getLatitude(), location.getLongitude(),
                            results);
                    distanceKm = results[0] / 1000.0;
                }

                // 消費カロリー推定（MET値 = 5, 体重 = 60kg, 時速15kmと仮定）
                double calories = 60 * 5 * (distanceKm / 15.0) * 1.05;
                String record = String.format("移動距離: %.2f km\n消費カロリー: %.1f kcal", distanceKm, calories);

                // 緯度経度情報も記録に追加
                String locationRecord = String.format("\n位置: (%.4f, %.4f)", location.getLatitude(), location.getLongitude());
                record += locationRecord;

                // SharedPreferencesに保存
                SharedPreferences prefs = getSharedPreferences("MemoPrefs", MODE_PRIVATE);
                String currentMemo = prefs.getString(selectedDateKey, "");
                SharedPreferences.Editor editor = prefs.edit();
                String newMemo = currentMemo.isEmpty() ? record : currentMemo + "\n\n" + record;
                editor.putString(selectedDateKey, newMemo);
                editor.apply();

                savedMemo.setText(newMemo);

                // マップにマーカーを追加
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(currentLatLng).title("記録地点: " + selectedDateKey));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));

                // 次回比較用に位置を更新
                lastLocation = location;

                Toast.makeText(this, "自転車記録を追加しました", Toast.LENGTH_SHORT).show();

            } else if (selectedDateKey.isEmpty()) {
                Toast.makeText(this, "先に日付を選択してください", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "位置情報が取得できませんでした", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // パーミッションリクエストの結果を受け取る
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可された場合、再度パーミッションチェックとマップ設定を行う
                checkLocationPermission();
            } else {
                Toast.makeText(this, "位置情報の許可が必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }
}