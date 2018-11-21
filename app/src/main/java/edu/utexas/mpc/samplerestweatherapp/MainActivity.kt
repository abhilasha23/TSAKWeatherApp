package edu.utexas.mpc.samplerestweatherapp


import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.net.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {


    // I'm using lateinit for these widgets because I read that repeated calls to findViewById
    // are energy intensive
    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var textView2: TextView
    lateinit var retrieveButton: Button
    lateinit var retrieveSteps: Button

    lateinit var queue: RequestQueue
    lateinit var gson: Gson
    lateinit var mostRecentWeatherResult: WeatherResult
    lateinit var forecastWeatherResult : WeatherForecast
    lateinit var forecastWeatherResult2 : WeatherForecast


    // I'm doing a late init here because I need this to be an instance variable but I don't
    // have all the info I need to initialize it yet
    lateinit var mqttAndroidClient: MqttAndroidClient

    // you may need to change this depending on where your MQTT broker is running
    val serverUri = "tcp://192.168.4.1:1883"
    // you can use whatever name you want to here
    val clientId = "EmergingTechMQTTClient"

    //these should "match" the topics on the "other side" (i.e, on the Raspberry Pi)
    val subscribeTopic = "steps"
    val publishTopic = "weather"

    var msg = "Before"
    var send_msg_to_pi = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = this.findViewById(R.id.text)
        retrieveButton = this.findViewById(R.id.retrieveButton)
        retrieveSteps = this.findViewById(R.id.syncbutton)
        textView1 = this.findViewById(R.id.steps)
        textView2 = this.findViewById(R.id.forecast)


        // when the user presses the syncbutton, this method will get called
        retrieveButton.setOnClickListener({
            requestWeather()
            forecastWeather()
            forecastWeather2()

        })

        retrieveSteps.setOnClickListener({ retrieveSteps()})

        // initialize the paho mqtt client with the uri and client id
        mqttAndroidClient = MqttAndroidClient(getApplicationContext(), serverUri, clientId);

        // when things happen in the mqtt client, these callbacks will be called
        mqttAndroidClient.setCallback(object: MqttCallbackExtended {

            // when the client is successfully connected to the broker, this method gets called
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                println("Connection Complete!!")
                // this subscribes the client to the subscribe topic
                mqttAndroidClient.subscribe(subscribeTopic, 1)

                println("+++++++ msg..." +msg)

                val message = MqttMessage()
                message.payload = (msg).toByteArray()
                //message.payload = (send_msg_to_pi).toByteArray()


                // this publishes a message to the publish topic
                mqttAndroidClient.publish(publishTopic, message)
            }

            // this method is called when a message is received that fulfills a subscription
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                println(message)

                steps.setText("Steps = "+message)
                mqttAndroidClient.disconnect()
            }

            override fun connectionLost(cause: Throwable?) {
                println("Connection Lost")
            }

            // this method is called when the client succcessfully publishes to the broker
            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                println("Delivery Complete")
            }
        })

        queue = Volley.newRequestQueue(this)
        gson = Gson()

    }

    fun retrieveSteps(){
        println("+++++++ inside retrieveSteps() : Connecting...")
        println(mqttAndroidClient)
        mqttAndroidClient.connect()
    }
    fun requestWeather(){
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/weather?id=4671654&units=metric&appid=1bb4e787e2b57fbf8c71c6998f0920ff").toString()
        val imageIcon = findViewById<View>(R.id.icon) as ImageView
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    //textView.text = response
                    mostRecentWeatherResult = gson.fromJson(response, WeatherResult::class.java)
                    textView.text = "Current Weather : " + mostRecentWeatherResult.weather.get(0).main + " ; " + mostRecentWeatherResult.main.temp + " Celcius"
                    msg = mostRecentWeatherResult.weather.get(0).main+ " -- Temp : " + mostRecentWeatherResult.main.temp

                    send_msg_to_pi = mostRecentWeatherResult.main.temp_max.toString() + "," + mostRecentWeatherResult.main.temp_min.toString() + ","+mostRecentWeatherResult.main.humidity.toString()

                    println("******* - " +msg)

                    val image = mostRecentWeatherResult.weather.get(0).icon
                    val iconUrl = "http://openweathermap.org/img/w/"+image+".png"

                    Picasso.get()
                            .load(iconUrl)
                            .resize(50, 50)
                            .centerCrop()
                            .into(imageIcon)

                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun forecastWeather(){
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/forecast?id=4671654&units=metric&cnt=16&appid=1bb4e787e2b57fbf8c71c6998f0920ff").toString()
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    //textView.text = response
                    forecastWeatherResult = gson.fromJson(response, WeatherForecast::class.java)
                    textView2.text = "Tomorrow same time : " + forecastWeatherResult.list.get(15).weather.get(0). description + "; " + forecastWeatherResult.list.get(15).main.temp + " Celcius"

                    print(forecastWeatherResult)

                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun forecastWeather2(){
        val url = StringBuilder("https://api.openweathermap.org/data/2.5/forecast?id=4671654&cnt=8&appid=1bb4e787e2b57fbf8c71c6998f0920ff").toString()
        val stringRequest = object : StringRequest(com.android.volley.Request.Method.GET, url,
                com.android.volley.Response.Listener<String> { response ->
                    //textView.text = response
                    forecastWeatherResult2 = gson.fromJson(response, WeatherForecast::class.java)
                    var min_temp = 0.0
                    var max_temp = 0.0
                    var sum_humidity = 0.0

                    for (item in forecastWeatherResult2.list ) {

                        if (min_temp == 0.0){
                            min_temp = item.main.temp_min
                        }
                         if (item.main.temp_min < min_temp) {
                             min_temp = item.main.temp_min
                         }

                        if (max_temp == 0.0){
                            max_temp = item.main.temp_max
                        }
                        if(item.main.temp_max > max_temp)
                            max_temp = item.main.temp_max

                        sum_humidity+=item.main.humidity

                    }

                    var avg_humidity = sum_humidity/8
                    println("max temp is : "+ max_temp)
                    println("min temp is : "+ min_temp)
                    println("avg humidity : "+ avg_humidity)
                    send_msg_to_pi = ""+min_temp+","+max_temp+","+avg_humidity

                },
                com.android.volley.Response.ErrorListener { println("******That didn't work!") }) {}
        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }
}

class WeatherResult(val id: Int, val name: String, val cod: Int, val coord: Coordinates, val main: WeatherMain, val weather: Array<Weather>)
class Coordinates(val lon: Double, val lat: Double)
class Weather(val id: Int, val main: String, val description: String, val icon: String)
class WeatherMain(val temp: Double, val pressure: Int, val humidity: Int, val temp_min: Double, val temp_max: Double)

class WeatherForecast(val cod: Int,val message: Double,val cnt: Int,val list: Array<ForecastList>,val city : City)
class ForecastList(val dt: Int,val main: WeatherMain_forecast,val weather:Array<Weather>,val clouds: Clouds,val wind : Wind,val rain : Rain, val sys : Sys,val date_txt : String)
class WeatherMain_forecast(val temp: Double, val temp_min: Double, val temp_max: Double, val pressure: Double,val sea_level: Double,val grnd_level : Double, val humidity: Int, val temp_kf : Double)
class City (val id:Int,val name: String,val coord:Coordinates,val country: String )
class Clouds (val all : Int)
class Wind (val speed: Double, val deg : Double)
class Rain ()
class Sys (val pod: String)