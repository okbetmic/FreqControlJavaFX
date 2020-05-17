#include <AD9850.h>
#include <stdint.h>
union long_byte_union
{
  unsigned long value;
  byte bytes[4];
};
long_byte_union long_byte;


#define PF_b 033
#define PL_b 066

#define SF_b 001
#define SW_b 002

#define HANDSHAKE_b 012
#define READY_b 002
#define SWEEP_LOOP_b 055
#define SWEEP_END_b 065


#define W_CLK_PIN 8
#define FQ_UD_PIN 7
#define DATA_PIN 6
#define RESET_PIN 5

/*enum states {NC, SB, SF, SW};
  states _state;

  byte deviceState = 0;*/

char buff[19];

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
  if (buff[0] == PF_b && buff[1] == HANDSHAKE_b && buff[2] == PL_b) // heartbeat
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
  if (buff[0] == PF_b && buff[18] == PL_b && buff[1] == SW_b) // setting sweep
  {
    long min_f, max_f, time_step, freq_step;

    long_byte.bytes[0] = buff[2];
    long_byte.bytes[1] = buff[3];
    long_byte.bytes[2] = buff[4];
    long_byte.bytes[3] = buff[5];
    min_f = long_byte.value;

    long_byte.bytes[0] = buff[6];
    long_byte.bytes[1] = buff[7];
    long_byte.bytes[2] = buff[8];
    long_byte.bytes[3] = buff[9];
    max_f = long_byte.value;

    long_byte.bytes[0] = buff[10];
    long_byte.bytes[1] = buff[11];
    long_byte.bytes[2] = buff[12];
    long_byte.bytes[3] = buff[13];
    time_step = long_byte.value; // MILLIS!!!

    long_byte.bytes[0] = buff[14];
    long_byte.bytes[1] = buff[15];
    long_byte.bytes[2] = buff[16];
    long_byte.bytes[3] = buff[17];
    freq_step = long_byte.value; // Hz!!
    
    
    //compensation
    unsigned long t1 = micros();
    Serial.write(READY_b);
    DDS.setfreq(min_f, 0);
    int delta = micros() - t1;
    
    delay(75);
    
    int step_count = ((max_f - min_f) / freq_step);
          
    for (int i = 1; i <= step_count; i++) {
      Serial.write(SWEEP_LOOP_b);
      DDS.setfreq(min_f + i * freq_step, 0);
      delay(time_step);
    }
    Serial.write(SWEEP_END_b);
    DDS.setfreq(0, 0);
  }
}

bool ping() {
  Serial.write(HANDSHAKE_b);
  while (!Serial.available()) {} // ждем рукопожатия
  byte bts = Serial.read();
  if (bts == HANDSHAKE_b) // отвечаем, если это компьютер
    Serial.write(HANDSHAKE_b);
}
