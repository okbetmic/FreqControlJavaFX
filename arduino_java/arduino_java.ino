#include <AD9850.h>
#include <stdint.h>
union long_byte_union
{
   unsigned long value;
   byte bytes[4];
};
long_byte_union long_byte;

#define HANDSHAKE_b 012
#define PL_b 066
#define PF_b 033
#define SF_b 001
#define READY_b 002

#define W_CLK_PIN 8
#define FQ_UD_PIN 7
#define DATA_PIN 6
#define RESET_PIN 5

/*enum states {NC, SB, SF, SW};
states _state;

byte deviceState = 0;*/

char buff[15];

void setup() {
  Serial.begin(9600);

  Serial.write(HANDSHAKE_b); // после загрузки отправляем сигнал о готовности
  while (!Serial.available()) {} // ждем рукопожатия
  buff[0] = Serial.read();
  if (buff[0] == HANDSHAKE_b) // отвечаем, если это компьютер
    Serial.write(HANDSHAKE_b);
  delay(10);

  DDS.begin(W_CLK_PIN, FQ_UD_PIN, DATA_PIN, RESET_PIN);
}

void loop() {
  handle();
}

void handle()
{
  while (!Serial.available()) {}
  delay(30);
  int num = Serial.available();
  for (int i = 0; i < num; i++) {
    buff[i] = Serial.read();
  }
  if (buff[0] == PF_b && buff[1] == HANDSHAKE_b && buff[2] == PL_b) // this is definitely a ping
    Serial.write(HANDSHAKE_b);
  if (buff[0] == PF_b && buff[6] == PL_b && buff[1] == SF_b) // setting single freq
  {
    delay(15);
    Serial.write(READY_b);
    
    long_byte.bytes[0] = buff[2];
    long_byte.bytes[1] = buff[3];
    long_byte.bytes[2] = buff[4];
    long_byte.bytes[3] = buff[5];
    DDS.setfreq(long_byte.value, 0);
  }
}

bool ping() {
  Serial.write(HANDSHAKE_b);
  while (!Serial.available()) {} // ждем рукопожатия
  byte bts = Serial.read();
  if (bts == HANDSHAKE_b) // отвечаем, если это компьютер
    Serial.write(HANDSHAKE_b);
}
