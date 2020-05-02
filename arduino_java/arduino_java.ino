#include <AD9850.h>

#define HANDSHAKE_b 012 //0x6E
#define PL_b 066
#define PF_b 033
#define SF_b 001
#define READY_b 002

#define WRITING_DELAY 30

#define W_CLK_PIN 8
#define FQ_UD_PIN 7
#define DATA_PIN 6
#define RESET_PIN 5

enum states {NC, SB, SF, SW};
states _state;

byte deviceState = 0;

byte buff[15];

void setup() {
  _state = NC;
  Serial.begin(9600);

  Serial.write(HANDSHAKE_b); // после загрузки отправляем сигнал о готовности
  while (!Serial.available()) {} // ждем рукопожатия
  buff[0] = Serial.read();
  if (buff[0] == HANDSHAKE_b) // отвечаем, если это компьютер
    Serial.write(HANDSHAKE_b);
  delay(10);
  _state = SB;

  DDS.begin(W_CLK_PIN, FQ_UD_PIN, DATA_PIN, RESET_PIN);
  DDS.calibrate(125000000);
  DDS.setfreq(1000, 0);
}

void loop() {
  rr();
}

void rr()
{
  while (!Serial.available()) {}
  delay(30);
  int num = Serial.available();
  for (int i = 0; i < num; i++) {
    buff[i] = Serial.read();
  }
  if (buff[0] == PF_b && buff[1] == HANDSHAKE_b && buff[2] == PL_b) // this is definitely a ping
    Serial.write(HANDSHAKE_b);
  if (buff[0] == PF_b && buff[6] == PL_b && buff[1] == SF_b) // single freq
  {
    long f = ((buff[5]) << 24 |
                (buff[4]) << 16 |
                (buff[3]) << 8 |
                (buff[2]));
    delay(15);
    Serial.write(READY_b);
    _state = SF;

    char return_buff[4];
    return_buff[0] = ((f) & 0xFF);
    return_buff[1] = ((f >> 8) & 0xFF);
    return_buff[2] = ((f >> 16) & 0xFF);
    return_buff[3] = ((f >> 24) & 0xFF);

    for (int i = 0; i < 4; i++) {
      Serial.write(return_buff[i]);
    }
    DDS.setfreq(f, 0);
  }
}

bool ping() {
  Serial.write(HANDSHAKE_b);
  while (!Serial.available()) {} // ждем рукопожатия
  byte bts = Serial.read();
  if (bts == HANDSHAKE_b) // отвечаем, если это компьютер
    Serial.write(HANDSHAKE_b);
}
