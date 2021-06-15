package com.example.fitapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fitapp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.data.Session
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.fitness.request.SessionReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    enum class Task{
        SUBSCRIBE,
        STEPS_COUNT_DAILY,
        STEPS_COUNT_WEEKLY,
        CALORIE_COUNT_DAILY,
        DISTANCE_COUNT_DAILY,
        WRITE_SLEEP_COUNT,
        SLEEP_COUNT_DAILY,
        STEPS_COUNT_TOTAL,
        CALORIE_COUNT_TOTAL,
        DISTANCE_COUNT_TOTAL,
        SLEEP_COUNT_TOTAL
    }
    private val SLEEP_SESSION = "sleep session"
    private val SLEEP_SESSION_DESC = "Sleep Session Description"
    private var startDate : Long? = null
    private var endDate : Long? = null
    private var MY_PERMISION_REQUEST_ACTIVITY = 1
    val TAG = "MainActivity"
    var permission:Boolean = false;

    lateinit var binding : ActivityMainBinding
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_CALORIES_EXPENDED,FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED,FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_DISTANCE_DELTA,FitnessOptions.ACCESS_READ)
        .addDataType(DataType.AGGREGATE_DISTANCE_DELTA,FitnessOptions.ACCESS_READ)
        .accessSleepSessions(FitnessOptions.ACCESS_WRITE)
        .accessSleepSessions(FitnessOptions.ACCESS_READ)
        .build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val startDatePicker = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.set(year,monthOfYear,dayOfMonth)
            val myFormat = "dd/MM/yy" //In which you need put here
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            val date = sdf.format(calendar.time)
            startDate = calendar.timeInMillis
            binding.etStartDate.setText(date.toString())
        }
        val endDatePicker = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.set(year,monthOfYear,dayOfMonth)
            val myFormat = "dd/MM/yy" //In which you need put here
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            val date = sdf.format(calendar.time)
            endDate = calendar.timeInMillis
            binding.etEndDate.setText(date.toString())
        }
        binding.etEndDate.setOnClickListener {
            val calendar = Calendar.getInstance(Locale.getDefault())
            DatePickerDialog(this,endDatePicker,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.etStartDate.setOnClickListener {
            val calendar = Calendar.getInstance(Locale.getDefault())
            DatePickerDialog(this,startDatePicker,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        binding.btnGet.setOnClickListener {
            if (startDate!=null && endDate!=null && startDate!! < endDate!!){
                //getDaily()
                getTotal()
            }
            else{
                Toast.makeText(this,"Enter Correct Date!!",Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getTotal() {
        checkPermissionAndRun(Task.SUBSCRIBE)
        checkPermissionAndRun(Task.WRITE_SLEEP_COUNT)
        checkPermissionAndRun(Task.STEPS_COUNT_TOTAL)
        checkPermissionAndRun(Task.CALORIE_COUNT_TOTAL)
        checkPermissionAndRun(Task.DISTANCE_COUNT_TOTAL)
        checkPermissionAndRun(Task.SLEEP_COUNT_TOTAL)
    }

    private fun getDaily() {
        checkPermissionAndRun(Task.SUBSCRIBE)
        checkPermissionAndRun(Task.STEPS_COUNT_DAILY)
        checkPermissionAndRun(Task.CALORIE_COUNT_DAILY)
        checkPermissionAndRun(Task.DISTANCE_COUNT_DAILY)
        checkPermissionAndRun(Task.WRITE_SLEEP_COUNT)
        checkPermissionAndRun(Task.SLEEP_COUNT_DAILY)
    }


    private fun checkPermissionAndRun(task: Task) {
        Log.i(TAG, "checkPermissionAndRun Started")
        if (permission){
            fitSignIn(task)
        }
        else{
            requestPermission(task)

        }
        Log.i(TAG, "checkPermissionAndRun: Ended")
    }


    private fun fitSignIn(task: Task) {
        Log.i(TAG, "fitSignIn: Started")
        if (oAuthSignIn()){
            performTask(task)
        }
        else{
            GoogleSignIn.requestPermissions(
                this,
                task.ordinal,
                getGoogleAccount(),
                fitnessOptions
            )
        }
        Log.i(TAG, "fitSignIn: Ended")
    }

    private fun oAuthSignIn() = GoogleSignIn.hasPermissions(getGoogleAccount(),fitnessOptions);

    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this,fitnessOptions)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode){
            RESULT_OK->{
                val postSignInAction = Task.values()[requestCode]
                checkPermissionAndRun(postSignInAction)
            }
            else->{
                Toast.makeText(this,"There was someError in the SignIn RequestCode: $requestCode and ResultCode:$resultCode",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performTask(task: Task) {

        when(task.ordinal){
            0->subscribe()
            1->readStepsDaily()
            2-> readStepsWeekly()
            3->readCalorieDaily()
            4->  readDistanceDaily()
            5-> writeSleep()
            6-> readSleepDaily()
            7 ->stepsCountTotal()
            8->calorieCountTotal()
            9->distanceCountTotal()
            10->sleepCountTotal()
        }
    }

    private fun sleepCountTotal() {
        val request = SessionReadRequest.Builder()
            .includeSleepSessions()
            .setTimeInterval(startDate!!,endDate!!,TimeUnit.MILLISECONDS)
            .build()
        var count = 0L
        Fitness.getSessionsClient(this,getGoogleAccount())
            .readSession(request)
            .addOnSuccessListener {response->
                for (session in response.sessions){
                    val sessionStart = session.getStartTime(TimeUnit.MILLISECONDS)
                    val sessionEnd = session.getEndTime(TimeUnit.MILLISECONDS)
                    count += (sessionEnd - sessionStart)
                    Log.i(TAG, "Sleep between $sessionStart and $sessionEnd")
                }
                count = TimeUnit.HOURS.convert(count,TimeUnit.MILLISECONDS)
                binding.tvTotalSleep.text = "Total Sleep : $count Hours"
            }
    }

    private fun distanceCountTotal() {
        val dateReadRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_DISTANCE_DELTA)
            .setTimeRange(startDate!!,endDate!!,TimeUnit.MILLISECONDS)
            .bucketByTime(1,TimeUnit.DAYS)
            .build()

        var count = 0f

        Fitness.getHistoryClient(this,getGoogleAccount())
            .readData(dateReadRequest)
            .addOnSuccessListener { response->
                response.buckets.forEach { bucket ->
                    bucket.dataSets.forEach { dataSet ->
                        Log.i(TAG, "stepsCountTotal Datatype: ${dataSet.dataPoints.size}")
                        if (dataSet.dataPoints.isNotEmpty())
                            count += dataSet.dataPoints.first().getValue(Field.FIELD_DISTANCE).asFloat()
                    }
                }
                binding.tvTotalDistance.text = "Total Distance: $count metres"
            }
    }

    private fun calorieCountTotal() {
        val dateReadRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
            .setTimeRange(startDate!!,endDate!!,TimeUnit.MILLISECONDS)
            .bucketByTime(1,TimeUnit.DAYS)
            .build()

        var count = 0f

        Fitness.getHistoryClient(this,getGoogleAccount())
            .readData(dateReadRequest)
            .addOnSuccessListener { response->
                response.buckets.forEach { bucket ->
                    bucket.dataSets.forEach { dataSet ->
                        Log.i(TAG, "stepsCountTotal Datatype: ${dataSet.dataPoints.size}")
                        if (dataSet.dataPoints.isNotEmpty())
                            count += dataSet.dataPoints.first().getValue(Field.FIELD_CALORIES).asFloat()
                    }
                }
                binding.tvTotalCalorie.text = "Total Calories: $count KCal"
            }
    }

    private fun stepsCountTotal() {
        val dateReadRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startDate!!,endDate!!,TimeUnit.MILLISECONDS)
            .bucketByTime(1,TimeUnit.DAYS)
            .build()

        var count = 0L

        Fitness.getHistoryClient(this,getGoogleAccount())
            .readData(dateReadRequest)
            .addOnSuccessListener { response->
                response.buckets.forEach { bucket ->
                    bucket.dataSets.forEach { dataSet ->
                        Log.i(TAG, "stepsCountTotal Datatype: ${dataSet.dataPoints.size}")
                        if (dataSet.dataPoints.isNotEmpty())
                            count += dataSet.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                    }
                }
                binding.tvTotalSteps.text = "Total Steps: $count"
            }
    }

    private fun writeSleep() {
        val calender = Calendar.getInstance(Locale.getDefault())
        calender.time = Date()
        val endTime = calender.timeInMillis
        calender.add(Calendar.HOUR_OF_DAY,-6)
        val startTime = calender.timeInMillis
        val sleepSession = Session.Builder()
            .setName(SLEEP_SESSION)
            .setIdentifier(endTime.toString())
            .setDescription(SLEEP_SESSION_DESC)
            .setStartTime(startTime,TimeUnit.MILLISECONDS)
            .setEndTime(endTime,TimeUnit.MILLISECONDS)
            .setActivity(FitnessActivities.SLEEP)
            .build()

        val request = SessionInsertRequest.Builder()
            .setSession(sleepSession)
            .build()
        Fitness.getSessionsClient(this,getGoogleAccount())
            .insertSession(request)
            .addOnSuccessListener {
                Log.i(TAG, "Sleep Insertion Success")

            }
            .addOnFailureListener {e->
                Log.i(TAG, "Sleep Insertion Failed",e)
            }
    }
    private fun readSleepDaily(){
        val calender = Calendar.getInstance(Locale.getDefault())
        calender.time = Date()
        val endTime = calender.timeInMillis
        calender.add(Calendar.DAY_OF_YEAR,-1)
        val startTime = calender.timeInMillis
        val request = SessionReadRequest.Builder()
            .includeSleepSessions()
            .setTimeInterval(startTime,endTime,TimeUnit.MILLISECONDS)
            .build()
        var count = 0L
        Fitness.getSessionsClient(this,getGoogleAccount())
            .readSession(request)
            .addOnSuccessListener {response->
                for (session in response.sessions){
                    val sessionStart = session.getStartTime(TimeUnit.MILLISECONDS)
                    val sessionEnd = session.getEndTime(TimeUnit.MILLISECONDS)
                    count += (sessionEnd - sessionStart)
                    Log.i(TAG, "Sleep between $sessionStart and $sessionEnd")
                }
                count = TimeUnit.HOURS.convert(count,TimeUnit.MILLISECONDS)
                binding.tvTotalSleep.text = "Total Sleep : $count Hours"
            }
    }

    private fun readStepsWeekly() {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.time = Date()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR,-1)
        val startTime = calendar.timeInMillis
        val calorieReadRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1,TimeUnit.DAYS)
            .setTimeRange(startTime,endTime,TimeUnit.MILLISECONDS)
            .build()
        Fitness.getHistoryClient(this,getGoogleAccount())
            .readData(calorieReadRequest)
            .addOnSuccessListener { response->
                printData(response)
            }
            .addOnFailureListener {e->
                Log.i(TAG, "There was a problem with steps count",e)
            }
    }

    private fun readStepsDaily() {
        Log.i(TAG, "readSteps: Started")
        Fitness.getHistoryClient(this,getGoogleAccount())
            .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener { dataset->
                val total = when{
                    dataset.isEmpty -> 0
                    else -> dataset.dataPoints.first().getValue(Field.FIELD_STEPS).asInt()
                }
                binding.tvTotalSteps.text ="Total Steps : $total"
                Log.i(TAG,"Total: $total")
            }
            .addOnFailureListener {e->
                Log.i(TAG, "There was a problem with the step counts",e)
            }

        Log.i(TAG, "readSteps: Ended")
    }

    private fun readCalorieDaily() {
        Log.i(TAG, "readSteps: Started")
        Fitness.getHistoryClient(this,getGoogleAccount())
            .readDailyTotal(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener { dataset->
                val total = when{
                    dataset.isEmpty -> 0
                    else -> dataset.dataPoints.first().getValue(Field.FIELD_CALORIES).asFloat()
                }
                binding.tvTotalCalorie.text ="Total Calorie : $total"
                Log.i(TAG,"Total: $total")
            }
            .addOnFailureListener {e->
                Log.i(TAG, "There was a problem with the step counts",e)
            }

        Log.i(TAG, "readSteps: Ended")
    }
    private fun readDistanceDaily() {
        Log.i(TAG, "readSteps: Started")
        Fitness.getHistoryClient(this,getGoogleAccount())
            .readDailyTotal(DataType.TYPE_DISTANCE_DELTA)
            .addOnSuccessListener { dataset->
                val total = when{
                    dataset.isEmpty -> 0
                    else -> dataset.dataPoints.first().getValue(Field.FIELD_DISTANCE).asFloat()
                }
                binding.tvTotalDistance.text ="Total Distance : $total m"
                Log.i(TAG,"Total: $total")
            }
            .addOnFailureListener {e->
                Log.i(TAG, "There was a problem with the step counts",e)
            }

        Log.i(TAG, "readSteps: Ended")
    }

    private fun subscribe() {
        Log.i(TAG, "subscribe: Started")
        Fitness.getRecordingClient(this, getGoogleAccount())
            .subscribe(DataType.TYPE_CALORIES_EXPENDED)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully Calorie subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing calorie.", e)
            }

        Fitness.getRecordingClient(this, getGoogleAccount())
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_STEP_COUNT_DELTA)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully Steps subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing steps.", e)
            }
        Fitness.getRecordingClient(this, getGoogleAccount())
            // This example shows subscribing to a DataType, across all possible data
            // sources. Alternatively, a specific DataSource can be used.
            .subscribe(DataType.TYPE_DISTANCE_DELTA)
            .addOnSuccessListener {
                Log.i(TAG, "Successfully Distance subscribed!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "There was a problem subscribing distance.", e)
            }
        Log.i(TAG, "subscribe: Ended")
        Fitness.getRecordingClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .listSubscriptions()
            .addOnSuccessListener { subscriptions ->
                for (sc in subscriptions) {
                    val dt = sc.dataType
                    Log.i(TAG, "Active subscription for data type: ${dt.name}")
                }
            }
    }
    private fun printData(dataReadResult: DataReadResponse) {
        Log.i(TAG, "printData: Started")
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.buckets.isNotEmpty()) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
            for (bucket in dataReadResult.buckets) {
                Log.i(TAG, "Bucket: ${bucket.dataSets.first().dataType.fields}")
                bucket.dataSets.forEach { dumpDataSet(it) }
            }
        } else if (dataReadResult.dataSets.isNotEmpty()) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.dataSets.size)
            dataReadResult.dataSets.forEach {
                Log.i(TAG, "printData: ${it.isEmpty}")
                dumpDataSet(it)
            }
        }
        Log.i(TAG, "printData: Ended")
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private fun dumpDataSet(dataSet: DataSet) {
        Log.i(TAG, "Data returned for Data type: ${dataSet.dataType.name}")

        Log.i(TAG, "dumpDataSet: ${dataSet.dataPoints.size}")
        for (dp in dataSet.dataPoints) {
            dp.dataType.fields.forEach {
                Log.i(TAG, "\tField: ${it.name} Value: ${dp.getValue(it)}")
            }
        }
        Log.i(TAG, "dumpDataSet: ENded")
    }

    private fun requestPermission(task: Task){
        /*MY_PERMISION_REQUEST_ACTIVITY = task.ordinal
        Log.i(TAG, "requestPermission: Started")
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED){
            permission = true
        }
        else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION,Manifest.permission.),MY_PERMISION_REQUEST_ACTIVITY)
        }*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Dexter.withContext(this).withPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                        permission = true
                        checkPermissionAndRun(Task.values()[MY_PERMISION_REQUEST_ACTIVITY])
                        Log.i(TAG, "onPermissionGranted")
                    }

                    override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                        Log.i(TAG, "onPermissionDenied")
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        request: PermissionRequest?,
                        token: PermissionToken?
                    ) {
                        Log.i(TAG, "onPermissionRationaleShouldBeShown")
                        token?.continuePermissionRequest()
                    }

                })
                .withErrorListener {
                    Toast.makeText(
                        this@MainActivity,
                        "Error in the permission!",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .onSameThread()
                .check()
        }
        else{
            permission = true;
            checkPermissionAndRun(task)
        }
        Log.i(TAG, "requestPermission: Ended")
    }
}

