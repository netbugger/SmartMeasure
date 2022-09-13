#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>
#include <SPI.h>
#include <Wire.h>
#include <Adafruit_GFX.h>
#include <Adafruit_SSD1306.h>
//#include <LIDARLite.h>
#include <LIDARLiteV2.h>
#include <EEPROM.h>

//LidarLitev2 myLidarLite;
LIDARLite myLidarLite;

//Button
#define BTN 13
#define BTN_RELEASED 1
#define BTN_HOLD 0
#define BTN_NONE 0
#define BTN_SHORT 1
#define BTN_LONG 2
#define DEBOUNCETIME 50
#define BTN_LONG_TIME 2000
typedef struct {
    int lastDebounceTime;
    int btnin;
    int btnstate;
    int btnnow;
    int btnlast;
}button_t;
button_t gBtn;

// Laser
#define LASER 17
#define PWM_CHANNEL 0
//
#define MODE_WAIT 0
#define MODE_HEIGHT 1
#define MODE_DISTANCE 2
#define MODE_CONFIG 3
#define DEFAULT_BIAS 200
typedef struct {
    uint16_t mode;
    uint16_t val;
    uint16_t lastMode;
    uint16_t bias;
} measure_t;
measure_t gMeasure;

#define WAIT_TIME 10000
void hTm_wait_mode(TimerHandle_t xTimer);
TimerHandle_t gWaitTimer;

#define CFG_SIZE 2
#define CFG_ADDR_VAL_HIGH 0
#define CFG_ADDR_VAL_LOW 1
uint16_t cfgVal;
//QueueHandle_t gWaitQueue;

// Images for display
//VSPI
#define OLED_D1 23  //MOSI
#define SPI_MISO 19
#define OLED_D0 18  //CLK
#define OLED_CS 5   //CS
#define OLED_DC 14
#define OLED_RES 26
#define OLED_GND 27
#define SCREEN_WIDTH 128  // OLED display width, in pixels
#define SCREEN_HEIGHT 64  // OLED display height, in pixels

#define OLED_RESET -1        // Reset pin # (or -1 if sharing Arduino reset pin)
#define SCREEN_ADDRESS 0x3C  ///< See datasheet for Address; 0x3D for 128x64, 0x3C for 128x32
//Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, OLED_RESET);
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, OLED_D1, OLED_D0, OLED_DC, OLED_RES, OLED_CS);


//블루투스 설정
#define BLE_NAME "SMART_MEASURE"
#define SERVICE_UUID "000000FF-0000-1000-8000-00805F9B34FB"
#define CHAR_RESULT_UUID "0000FF01-0000-1000-8000-00805F9B34FB"
#define CHAR_CMD_UUID "0000FF02-0000-1000-8000-00805F9B34FB"
#define BLE_CMD_MODE 'm'
#define BLE_CMD_MEAUSER 'r'

BLEServer* pServer = NULL;
BLECharacteristic* pCharResult = NULL;
BLECharacteristic* pCharCmd = NULL;
bool deviceConnected = false;
bool oldDeviceConnected = false;
int btReconnCnt = 0;

void send_bt_result(void);
void hTm_wait_mode(TimerHandle_t xTimer);
void display_on(void);
void handle_short_btn(void);
void handle_btn_input(void);
void handle_long_btn(void);

class MyCallbacks : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pCharacteristic) {
    //앞의 1바아트만 제어 문자로 인식
    char cmd;
    std::string rxValue = pCharacteristic->getValue();
    if (rxValue.length() > 0) {
      cmd = rxValue[0];
      Serial.print("[BLE]CMD = ");
      Serial.println(cmd);
    }

    switch (cmd) {
    case 'm':
        handle_long_btn();
        break;
    case 'r':
        handle_short_btn();
        break;
    case 'i' : //Init
        printf("Init Cmd received\n");
        send_bt_result();
        break;
    default:
        Serial.println("BLE Command Error");
        break;
    }
  }
};

// 콜백 클래스 : 블루투스 커넥션 관리
class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    printf("Bluetooth Connected\n");
  }
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    printf("Bluetooth Connected\n");
  }
};


void setup() {
    Serial.begin(115200);
    printf("Hello Smart Measure\n");

    // 타이머 생성
    gWaitTimer = xTimerCreate("wait", pdMS_TO_TICKS(WAIT_TIME), pdTRUE, NULL, hTm_wait_mode);  //50ms마다 측정하도록 타이머 설정
  

    //블루투스 설정
    BLEDevice::init(BLE_NAME);
    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new MyServerCallbacks());

    BLEService* pService = pServer->createService(SERVICE_UUID);
    pCharResult = pService->createCharacteristic(
        CHAR_RESULT_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_WRITE |
        BLECharacteristic::PROPERTY_NOTIFY |
        BLECharacteristic::PROPERTY_INDICATE);
    pCharResult->addDescriptor(new BLE2902());

    pCharCmd = pService->createCharacteristic(
        CHAR_CMD_UUID,
        BLECharacteristic::PROPERTY_READ | BLECharacteristic::PROPERTY_WRITE);
    pCharCmd->setCallbacks(new MyCallbacks());

    pService->start();

  
    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);  // functions that help with iPhone connections issue
    pAdvertising->setMinPreferred(0x12);
    BLEDevice::startAdvertising();

    Serial.println("BLE Started");

    //DISPLAY
    pinMode(OLED_GND, OUTPUT);
    digitalWrite(OLED_GND, LOW);

    if (!display.begin(SSD1306_SWITCHCAPVCC)) {
        Serial.println("SSD1306 allocation failed");
    }
    display.clearDisplay();
    display.display();

    //Lidar
    myLidarLite.begin();

    //Laser
    pinMode(LASER, OUTPUT);
    ledcAttachPin(LASER, PWM_CHANNEL);
    ledcSetup(PWM_CHANNEL, 5000, 8);
    ledcWrite(PWM_CHANNEL, 0);
    
    //버튼 설정
    pinMode(BTN, INPUT_PULLUP);
    gBtn.lastDebounceTime;
    gBtn.btnin = BTN_NONE;
    gBtn.btnstate = BTN_NONE;
    gBtn.btnnow = BTN_RELEASED;
    gBtn.btnlast = BTN_RELEASED;


    //Init Variables
    EEPROM.begin(CFG_SIZE);
    cfgVal = EEPROM.read(CFG_ADDR_VAL_HIGH) << 8;
    cfgVal |= EEPROM.read(CFG_ADDR_VAL_LOW);
    printf("CFG=%x\n",cfgVal);
    if (cfgVal == 0xffff) {
        cfgVal = DEFAULT_BIAS;
    }
    gMeasure.mode = MODE_WAIT;
    gMeasure.val = 0;
    gMeasure.bias = cfgVal;
    gMeasure.lastMode = MODE_HEIGHT;
}

void loop() {
    // BLE WORK
    // disconnecting
    if (!deviceConnected && oldDeviceConnected) {
        btReconnCnt++;
        if (btReconnCnt == 10) {
            btReconnCnt = 0;
            pServer->startAdvertising();  // restart advertising
            Serial.println("start advertising");
            oldDeviceConnected = deviceConnected;
        }
    } 
    // connecting
    if (deviceConnected && !oldDeviceConnected) {
        // do stuff here on connecting
        oldDeviceConnected = deviceConnected;
        //send_bt_result();
    }
    handle_btn_input();  //버튼 입력 처리

    delay(10);
}

void handle_btn_input(void) {
    //Read
    gBtn.btnnow = digitalRead(BTN);

    if (gBtn.btnstate == BTN_NONE) {
        if (gBtn.btnnow == BTN_HOLD) {
            if (gBtn.btnnow != gBtn.btnlast) {
                gBtn.lastDebounceTime = millis();
            } 
            else if ((millis() - gBtn.lastDebounceTime) > DEBOUNCETIME) {
                gBtn.btnstate = BTN_SHORT;
            }
            gBtn.btnlast = gBtn.btnnow;
        }
        else if (gBtn.btnnow == BTN_RELEASED) {
            gBtn.btnlast = gBtn.btnnow;
        }
    }
    else if (gBtn.btnstate == BTN_SHORT) {
        if (gBtn.btnnow == BTN_HOLD) {
            if ((millis() - gBtn.lastDebounceTime) > BTN_LONG_TIME) {
                gBtn.btnstate = BTN_LONG;
                gBtn.btnin = BTN_LONG;
            }
            gBtn.btnlast = gBtn.btnnow;
        } 
        else if (gBtn.btnnow == BTN_RELEASED) {
            gBtn.btnin = BTN_SHORT;
            gBtn.btnstate = BTN_NONE;
            gBtn.btnlast = gBtn.btnnow;
        }
    } 
    else if (gBtn.btnstate == BTN_LONG) {
        if (gBtn.btnnow == BTN_RELEASED) {
            gBtn.btnstate = BTN_NONE;
            gBtn.btnlast = gBtn.btnnow;
        }
    }

    if (gBtn.btnin != BTN_NONE) {
        if (gBtn.btnin == BTN_SHORT) {
            printf("[BTN][SHORT]\n");
            handle_short_btn();
        } else if (gBtn.btnin == BTN_LONG) {
            //mode change
            printf("[BTN][LONG]\n");
            handle_long_btn();
        }
    }
    gBtn.btnin = BTN_NONE;
}

void handle_short_btn(void) {
    int val;
    switch (gMeasure.mode) {
    case MODE_WAIT:
        //wakeup
        gMeasure.mode = gMeasure.lastMode;
        gMeasure.val = 0;
        break;
    case MODE_HEIGHT:
        val = myLidarLite.distance();
        if(gMeasure.bias > val) {
            gMeasure.val = gMeasure.bias - val;
        }
        else {
            gMeasure.val = 0;
        }
        break;
    case MODE_DISTANCE:
        val = myLidarLite.distance();
        gMeasure.val = val;
        break;
    case MODE_CONFIG:
        gMeasure.val = myLidarLite.distance();
        gMeasure.bias =  gMeasure.val;   
        EEPROM.write(CFG_ADDR_VAL_HIGH, (gMeasure.bias>>8 & 0xff));
        EEPROM.write(CFG_ADDR_VAL_LOW, (gMeasure.bias & 0xff));
        EEPROM.commit();
        break;
    default:
        break;
    }
    display_on();
}

void handle_long_btn(void) {
    switch (gMeasure.mode) {
    case MODE_WAIT:
        //wakeup
        gMeasure.mode = gMeasure.lastMode;
        gMeasure.val = 0;
        break;
    case MODE_HEIGHT:
        gMeasure.mode = MODE_DISTANCE;
        gMeasure.lastMode = gMeasure.mode;
        gMeasure.val = 0;
        break;
    case MODE_DISTANCE:
        gMeasure.mode = MODE_CONFIG;
        gMeasure.lastMode = gMeasure.mode;
        gMeasure.val = 0;
        break;
    case MODE_CONFIG:
        gMeasure.mode = MODE_HEIGHT;
        gMeasure.lastMode = gMeasure.mode;
        gMeasure.val = 0;
        break;
    default:
        break;
  }
  display_on();
}

void display_on(void) {
    char str[16];
    int val;
    switch (gMeasure.mode) {
    case MODE_HEIGHT:
        strcpy(str,"HEIGHT");
        val = gMeasure.val;
        break;
    case MODE_DISTANCE:
        strcpy(str,"DISTANCE");
        val = gMeasure.val;
        break;
    case MODE_CONFIG:
        strcpy(str,"CONFIG");
        val = gMeasure.bias;
        break;
    default:
        strcpy(str,"ERROR");
        val = 0;
        break;
    }

    display.clearDisplay();
    display.setCursor(0, 0);
    display.setTextSize(2);
    display.setTextColor(SSD1306_WHITE);

    display.print(str);
    display.setCursor(0, 32);
    display.print(val);
    display.print("cm");
    display.display();

    //Laser On;
    ledcWrite(PWM_CHANNEL, 150);
    //Bluetooth report
    send_bt_result();
    //Shutdown Timer
    xTimerStart(gWaitTimer, 0);  // 타이머 시작 10초동안 기다린 후 꺼짐
}

void hTm_wait_mode(TimerHandle_t xTimer) {
    //Turn Off Display
    display.clearDisplay();
    display.display();
    //Turn off Laser
    ledcWrite(PWM_CHANNEL, 0);
    //DigitalWrite(LASER, LOW);
    gMeasure.mode = MODE_WAIT;
    gMeasure.val = 0;
    //Bluetooth Report
    send_bt_result();
}

void send_bt_result(void) {
     if (deviceConnected) {
        pCharResult->setValue((uint8_t*)&gMeasure, sizeof(gMeasure));
        pCharResult->notify();
    }
}