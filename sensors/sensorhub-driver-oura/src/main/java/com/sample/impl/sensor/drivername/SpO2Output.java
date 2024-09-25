/***************************** BEGIN LICENSE BLOCK ***************************

 The contents of this file are subject to the Mozilla Public License, v. 2.0.
 If a copy of the MPL was not distributed with this file, You can obtain one
 at http://mozilla.org/MPL/2.0/.

 Software distributed under the License is distributed on an "AS IS" basis,
 WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 for the specific language governing rights and limitations under the License.

 Copyright (C) 2020-2021 Botts Innovative Research, Inc. All Rights Reserved.

 ******************************* END LICENSE BLOCK ***************************/
package com.sample.impl.sensor.drivername;

import net.opengis.swe.v20.DataBlock;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import net.opengis.swe.v20.DataRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.impl.sensor.AbstractSensorOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vast.swe.SWEHelper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;

/**
 * Output specification and provider for {@link Sensor}.
 *
 * @author your_name
 * @since date
 */
public class SpO2Output extends AbstractSensorOutput<Sensor> implements Runnable{

    private static final String SENSOR_OUTPUT_NAME = "SpO2";
    private static final String SENSOR_OUTPUT_LABEL = "SpO2";
    private static final String SENSOR_OUTPUT_DESCRIPTION = "Daily SpO2 Data";

    private static final Logger logger = LoggerFactory.getLogger(SpO2Output.class);

    private DataRecord dataStruct;
    private DataEncoding dataEncoding;

    private Boolean stopProcessing = false;
    private final Object processingLock = new Object();

    private static final int MAX_NUM_TIMING_SAMPLES = 10;
    private int setCount = 0;
    private final long[] timingHistogram = new long[MAX_NUM_TIMING_SAMPLES];
    private final Object histogramLock = new Object();

    private Thread worker;

    private String bearerToken;
    private Date startTime, endTime;

    /**
     * Constructor
     *
     * @param parentSensor Sensor driver providing this output
     */
    SpO2Output(Sensor parentSensor) {

        super(SENSOR_OUTPUT_NAME, parentSensor);

        logger.debug("Output created");
    }

    /**
     * Initializes the data structure for the output, defining the fields, their ordering,
     * and data types.
     */
    void doInit(String token, Date start, Date end) {

        logger.debug("Initializing Output");

        // Get an instance of SWE Factory suitable to build components
        SWEHelper sweFactory = new SWEHelper();

        // TODO: Create data record description
        dataStruct = sweFactory.createRecord()
                .name(SENSOR_OUTPUT_NAME)
                .definition("urn:osh:data:oura:spo2")
                .label(SENSOR_OUTPUT_LABEL)
                .description(SENSOR_OUTPUT_DESCRIPTION)
                .addField("sampleTime", sweFactory.createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .addField("spo2", sweFactory.createQuantity()
                        .definition(SWEHelper.getCfUri("spo2"))
                        .label("SpO2")
                        .uomCode("%"))
                .build();

        dataEncoding = sweFactory.newTextEncoding(",", "\n");

        bearerToken = token;
        startTime = start;
        endTime = end;

        logger.debug("Initializing Output Complete");
    }

    /**
     * Begins processing data for output
     */
    public void doStart() {

        // Instantiate a new worker thread
        worker = new Thread(this, this.name);

        // TODO: Perform other startup

        logger.info("Starting worker thread: {}", worker.getName());

        // Start the worker thread
        worker.start();
    }

    /**
     * Terminates processing data for output
     */
    public void doStop() {

        synchronized (processingLock) {

            stopProcessing = true;
        }

        // TODO: Perform other shutdown procedures
    }

    /**
     * Check to validate data processing is still running
     *
     * @return true if worker thread is active, false otherwise
     */
    public boolean isAlive() {

        return worker.isAlive();
    }

    @Override
    public DataComponent getRecordDescription() {

        return dataStruct;
    }

    @Override
    public DataEncoding getRecommendedEncoding() {

        return dataEncoding;
    }

    @Override
    public double getAverageSamplingPeriod() {

        long accumulator = 0;

        synchronized (histogramLock) {

            for (int idx = 0; idx < MAX_NUM_TIMING_SAMPLES; ++idx) {

                accumulator += timingHistogram[idx];
            }
        }

        return accumulator / (double) MAX_NUM_TIMING_SAMPLES;
    }




    @Override
    public void run() {

        boolean processSets = true;

        long lastSetTimeMillis = System.currentTimeMillis();

        try {
            // TODO: Need to use getters & setters for timeFilter?
            Config config = new Config();

            LocalDateTime now = LocalDateTime.now();
            ZoneId zone = ZoneId.of("America/Chicago");
            ZoneOffset zoneOffset = zone.getRules().getOffset(now);

//            String startDate = config.timeFilter.startTime.toInstant().atZone(zoneOffset).toLocalDate().toString();
//            String endDate = config.timeFilter.endTime.toInstant().atZone(zoneOffset).toLocalDate().toString();
            String token = config.bearerToken;

            String requestString = "https://api.ouraring.com/v2/usercollection/daily_spo2?start_date=";
            requestString += startTime.toInstant().atZone(zoneOffset).toLocalDate().toString();
            requestString += "&end_date=";
            requestString += endTime.toInstant().atZone(zoneOffset).toLocalDate().toString();

            String spo2_response = makeRequest(requestString, bearerToken);
            JSONObject[] spo2_jsons = getDataRecord(spo2_response);

            int i =0;
            while (i < spo2_jsons.length) {

                DataBlock dataBlock;
                if (latestRecord == null) {

                    dataBlock = dataStruct.createDataBlock();

                } else {

                    dataBlock = latestRecord.renew();
                }

                if(!spo2_jsons[i].getJSONObject("spo2_percentage").isNull("average")) {
                    // TODO: Populate data block
                    LocalDateTime date = LocalDateTime.parse(spo2_jsons[i].getString("day") + "T07:00:00");
                    dataBlock.setDoubleValue(0, date.toEpochSecond(zoneOffset));
                    dataBlock.setDoubleValue(1, spo2_jsons[i].getJSONObject("spo2_percentage").getDouble("average"));
                    latestRecord = dataBlock;
                    latestRecordTime = date.toEpochSecond(zoneOffset);
                    eventHandler.publish(new DataEvent(latestRecordTime, SpO2Output.this, dataBlock));
                }
                else System.out.println("Record REJECTED due to NULL value");

                synchronized (histogramLock) {

                    int setIndex = setCount % MAX_NUM_TIMING_SAMPLES;

                    // Get a sampling time for latest set based on previous set sampling time
                    timingHistogram[setIndex] = System.currentTimeMillis() - lastSetTimeMillis;

                    // Set latest sampling time to now
                    lastSetTimeMillis = timingHistogram[setIndex];
                }

                ++setCount;

                i++;
            }

        } catch (Exception e) {

            logger.error("Error in worker thread: {}", Thread.currentThread().getName(), e);

        } finally {

            // Reset the flag so that when driver is restarted loop thread continues
            // until doStop called on the output again
            stopProcessing = false;

            logger.debug("Terminating worker thread: {}", this.name);
        }
    }

    public static String makeRequest(String requestString, String cloudToken) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestString))
                .header("Authorization", "Bearer " + cloudToken)
                .GET() // GET is default
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public static JSONObject[] getDataRecord(String response) {
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray temp_array = jsonResponse.getJSONArray("data");
        ArrayList<JSONObject> arrays = new ArrayList<>();
        for (int i = 0; i < temp_array.length(); i++) {
            JSONObject array = temp_array.getJSONObject(i);
            arrays.add(array);
        }
        JSONObject[] jsons = new JSONObject[arrays.size()];
        arrays.toArray(jsons);
        return jsons;
    }




}
