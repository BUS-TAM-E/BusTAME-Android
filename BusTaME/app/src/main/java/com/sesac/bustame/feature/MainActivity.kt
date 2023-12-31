package com.sesac.bustame.feature

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.JsonObject
import com.sesac.bustame.BusRideBell
import com.sesac.bustame.R
import com.sesac.bustame.data.model.ItemList
import com.sesac.bustame.data.model.LocationInfo
import com.sesac.bustame.data.network.RetrofitClient
import com.sesac.bustame.databinding.ActivityMainBinding
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import net.daum.mf.map.api.MapView.CurrentLocationEventListener
import net.daum.mf.map.api.MapView.POIItemEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity(), CurrentLocationEventListener, POIItemEventListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var bottomSheetDefaultDialog: BottomSheetDialog
    private lateinit var bottomSheetBusInfoDialog: BottomSheetDialog
    private lateinit var locationJson: JsonObject
    private var tmX: String? = null
    private var tmY: String? = null
    private var radius: String = "100"

    private lateinit var busStopNum: String
    private lateinit var busStopName: String
    private var responseData: ItemList? = null

    private lateinit var passengerTypeValue: String
    private lateinit var messageValue: String

    private var isDefalutBottomSheetOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        locationJson = JsonObject()

        // 이전 액티비티 값 받아오기
        passengerTypeValue =
            intent.getStringExtra(BusRideBell.BUS_PASSENGER_TYPE_VALUE_KEY).toString()
        messageValue = intent.getStringExtra(BusRideBell.BUS_MESSAGE_KEY).toString()
        Log.d("intentvalue", "$passengerTypeValue $messageValue")

        // 권한 ID 선언
        val internetPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
        val fineLocaPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocaPermission =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        // 권한이 열려있는지 확인
        if (internetPermission == PackageManager.PERMISSION_DENIED ||
            fineLocaPermission == PackageManager.PERMISSION_DENIED ||
            coarseLocaPermission == PackageManager.PERMISSION_DENIED
        ) {
            // 마쉬멜로우 이상 버전부터 권한을 물어봅니다.
            // 권한 체크(READ_PHONE_STATE의 requestCode를 1000으로 세팅
            requestPermissions(
                arrayOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
                1000,
            )
        }

        // 현재 위치로 지도 이동
        binding.mapView.currentLocationTrackingMode =
            MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
        binding.mapView.setShowCurrentLocationMarker(true)
        // 현재 위치 이벤트 리스너
        binding.mapView.setCurrentLocationEventListener(this)

        binding.mapView.setPOIItemEventListener(this)

        binding.myLocation.setOnClickListener {
            binding.mapView.currentLocationTrackingMode =
                MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading
            binding.mapView.setShowCurrentLocationMarker(true)
        }
        findBusstop()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1000) {
            var checkResult = true

            // 모든 퍼미션을 허용했는지 체크
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    checkResult = false
                    break
                }
            }

            // 권한 체크에 동의를 하지 않으면 안드로이드 종료
            if (!checkResult) {
                finish()
            }
        }
    }

    override fun onCurrentLocationUpdate(
        mapView: MapView,
        mapPoint: MapPoint,
        accuracyInMeters: Float,
    ) {
        Log.d("lati", "위치위치위치위치")
        tmX = mapPoint.mapPointGeoCoord.latitude.toString() // 위도
        tmY = mapPoint.mapPointGeoCoord.longitude.toString() // 경도
        Log.d("lati", "$tmY $tmX")
    }

    override fun onCurrentLocationDeviceHeadingUpdate(mapView: MapView, v: Float) {
        // 현재 위치 디바이스 방향 업데이트 이벤트 처리
    }

    override fun onCurrentLocationUpdateFailed(mapView: MapView) {
        // 현재 위치 업데이트 실패 처리
        Log.d("lati", "latiFail")
    }

    override fun onCurrentLocationUpdateCancelled(mapView: MapView) {
        // 현재 위치 업데이트 취소 처리
    }

    override fun onPOIItemSelected(mapView: MapView, marker: MapPOIItem) {
        // 선택한 버스 정류장의 정보를 마커에서 가져옵니다
        busStopNum = marker.tag.toString()
        busStopName = marker.itemName

        // 버스 정보 바텀시트를 생성하고 표시합니다
        createBusInfoBottomSheet(responseData!!, busStopNum, busStopName)
    }

    override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView, marker: MapPOIItem) {
    }

    override fun onCalloutBalloonOfPOIItemTouched(
        p0: MapView?,
        p1: MapPOIItem?,
        p2: MapPOIItem.CalloutBalloonButtonType?,
    ) {
        TODO("Not yet implemented")
    }

    override fun onDraggablePOIItemMoved(mapView: MapView, marker: MapPOIItem, mapPoint: MapPoint) {
        // 드래그 가능한 POI 아이템이 이동된 경우에 대한 처리 (선택된 버스 정류장 정보를 가져와서 사용할 수 있음)
        // 이 메서드가 필요하지 않다면 빈 구현으로 남겨둘 수 있습니다.
    }

    private fun findBusstop() {
        binding.findBusStop.setOnClickListener {
            Toast.makeText(this, "주변 버스정류장을 확인합니다", Toast.LENGTH_SHORT).show()
            // 클릭 이벤트 처리 로직 추가

            if (tmX != null && tmY != null) {
                // 현재 위치 좌표 서버로 보내기
                val call =
                    RetrofitClient.service.sendUserPosData(LocationInfo(tmY!!, tmX!!, radius))
                call.enqueue(object : Callback<ItemList> {
                    override fun onResponse(call: Call<ItemList>, response: Response<ItemList>) {
                        if (response.isSuccessful) {
                            responseData = response.body()
                            Log.d("responsedata", responseData.toString())
                            // itemList를 처리하고 지도 위에 마커를 표시합니다
                            for (item in responseData!!.itemList) {
                                val latitude = item.gpsY.toDouble()
                                val longitude = item.gpsX.toDouble()

                                // 지도 위에 마커 표시
                                val marker = MapPOIItem()
                                marker.tag = item.arsId.toInt()
                                marker.itemName = item.stationNm
                                marker.mapPoint = MapPoint.mapPointWithGeoCoord(latitude, longitude)

                                // 커스텀 마커
                                marker.markerType = MapPOIItem.MarkerType.CustomImage
                                marker.customImageResourceId = R.drawable.ic_busstop_marker
                                marker.customSelectedImageResourceId = R.drawable.ic_busstop_marker
                                marker.isCustomImageAutoscale = true // 커스텀 마커 이미지 크기 자동 조정
                                marker.setCustomImageAnchor(0.5f, 1.0f)

                                // 마커를 지도에 추가합니다
                                binding.mapView.addPOIItem(marker)
                            }
                        } else {
                            // 서버로부터 실패 응답을 받은 경우 처리
                            Log.d("serverresponse", "FailFailResponse")
                            Log.d("serverresponsecode", response.code().toString())
                        }
                    }

                    override fun onFailure(call: Call<ItemList>, t: Throwable) {
                        // 통신 실패 처리
                        Log.d("serverresponse", "fail $t")
                    }
                })
            } else {
                Toast.makeText(this, "위치를 찾을 수 없음", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // businfo 바텀시트 생성 및 표시
    private fun createBusInfoBottomSheet(
        responseData: ItemList,
        busStopNum: String,
        busStopName: String,
    ) {
        val bottomSheetView = layoutInflater.inflate(R.layout.layout_bottom_sheet_businfo, null)
        val busStopNumTextView = bottomSheetView.findViewById<TextView>(R.id.busStopNum)
        val busStopNameTextView = bottomSheetView.findViewById<TextView>(R.id.busStopName)
        val btnGoBusBell = bottomSheetView.findViewById<TextView>(R.id.btnGoBusBell)

        // 아래 코드 추가
        val itemList = responseData!!.itemList ?: emptyList()
        for (item in itemList) {
            if (item.stationNm == busStopName) {
                busStopNumTextView.text = item.arsId
                busStopNameTextView.text = item.stationNm
                break
            }
        }

        btnGoBusBell.visibility = View.VISIBLE

        btnGoBusBell.setOnClickListener {
            val intent = Intent(this, BellActivity::class.java)
            intent.putExtra("busStopNum", busStopNum)
            intent.putExtra("busStopName", busStopName)
            intent.putExtra(BusRideBell.BUS_PASSENGER_TYPE_VALUE_KEY, passengerTypeValue)
            intent.putExtra(BusRideBell.BUS_MESSAGE_KEY, messageValue)
            startActivity(intent)
        }

        bottomSheetBusInfoDialog = BottomSheetDialog(this, R.style.AppBottomSheetDialogTheme)
        bottomSheetBusInfoDialog.behavior.peekHeight = 1200 // 원하는 높이 값으로 설정
        bottomSheetBusInfoDialog.behavior.state =
            BottomSheetBehavior.STATE_COLLAPSED // 바텀 시트 완전히 펴져있는 상태
        bottomSheetBusInfoDialog.setContentView(bottomSheetView)
        bottomSheetBusInfoDialog.show()
    }
}
