#include <jni.h>
#include <cstdio>
#include <android/log.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <linux/i2c-dev.h>
#include <unistd.h>
#include <string>
#include <sys/ioctl.h>
#include <termios.h>
#include <time.h>


#define  LOG_TAG    "JNI Code"
//#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG ,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG ,__VA_ARGS__)
//#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,LOG_TAG ,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG ,__VA_ARGS__)
//#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,LOG_TAG ,__VA_ARGS__)


// Do not forget to dynamically load the C++ library into your application.
// For instance,
// In MainActivity.java:
//    static {
//        System.loadLibrary("native-lib");
//    }

extern "C" JNIEXPORT jint JNICALL Java_com_randi_RandiTestSys_MainActivity_openPort(JNIEnv *env, jclass thiz, jstring path)
{
    const char *path_utf;

    path_utf = env->GetStringUTFChars(path, 0);
    if (path_utf == NULL)
        return -1;

    int fd = open(path_utf, O_RDWR);
    env->ReleaseStringUTFChars(path, path_utf);

    return fd;
}




extern "C" JNIEXPORT void JNICALL Java_com_randi_RandiTestSys_MainActivity_closePort(JNIEnv *env, jclass thiz, jint fd)
{
    close(fd);
}



#define LOOPBACK   0x8000
extern "C" JNIEXPORT jint JNICALL Java_com_randi_RandiTestSys_MainActivity_cfgUART(JNIEnv *env, jclass clazz, jint fd, jboolean Baud9600) {
    struct termios cfg;
    unsigned int line_val=0;

/*    struct termios {
        tcflag_t c_iflag;		// input mode flags
        tcflag_t c_oflag;		// output mode flags
        tcflag_t c_cflag;		// control mode flags
        tcflag_t c_lflag;		// local mode flags
        cc_t c_line;			// line discipline
        cc_t c_cc[NCCS];		// control characters
    };
    https://blog.mbedded.ninja/programming/operating-systems/linux/linux-serial-ports-using-c-cpp/
    tty.c_cflag &= ~PARENB; // Clear parity bit, disabling parity (most common)
    tty.c_cflag |= PARENB;  // Set parity bit, enabling parity

    tty.c_cflag &= ~CSTOPB; // Clear stop field, only one stop bit used in communication (most common)
    tty.c_cflag |= CSTOPB;  // Set stop field, two stop bits used in communication

    tty.c_cflag &= ~CSIZE; // Clear all the size bits, then use one of the statements below
    tty.c_cflag |= CS5; // 5 bits per byte
    tty.c_cflag |= CS6; // 6 bits per byte
    tty.c_cflag |= CS7; // 7 bits per byte
    tty.c_cflag |= CS8; // 8 bits per byte (most common)

    tty.c_cflag &= ~CRTSCTS; // Disable RTS/CTS hardware flow control (most common)
    tty.c_cflag |= CRTSCTS;  // Enable RTS/CTS hardware flow control

    tty.c_cflag |= CREAD | CLOCAL; // Turn on READ & ignore ctrl lines (CLOCAL = 1)

    c_cflag = BAUDRATE | CRTSCTS | CS8 | CLOCAL | CREAD;
*/

    if (tcgetattr(fd, &cfg)){
        close(fd);
        return -1;
    }

    //memset(&cfg, 0, sizeof(cfg));

    cfg.c_cflag &= ~PARENB; // Clear parity bit, disabling parity (most common)
    cfg.c_cflag &= ~CSTOPB; // Clear stop field, only one stop bit used in communication (most common)
    cfg.c_cflag &= ~CSIZE; // Clear all bits that set the data size
    cfg.c_cflag |= CS8; // 8 bits per byte (most common)
    cfg.c_cflag &= ~CRTSCTS; // Disable RTS/CTS hardware flow control (most common)
    cfg.c_cflag |= CREAD | CLOCAL; // Turn on READ & ignore ctrl lines (CLOCAL = 1)

    cfg.c_lflag &= ~ICANON;
    cfg.c_lflag &= ~ECHO; // Disable echo
    cfg.c_lflag &= ~ECHOE; // Disable erasure
    cfg.c_lflag &= ~ECHONL; // Disable new-line echo
    cfg.c_lflag &= ~ISIG; // Disable interpretation of INTR, QUIT and SUSP
    cfg.c_iflag &= ~(IXON | IXOFF | IXANY); // Turn off s/w flow ctrl
    cfg.c_iflag &= ~(IGNBRK|BRKINT|PARMRK|ISTRIP|INLCR|IGNCR|ICRNL); // Disable any special handling of received bytes

    cfg.c_oflag &= ~OPOST; // Prevent special interpretation of output bytes (e.g. newline chars)
    cfg.c_oflag &= ~ONLCR;


    cfg.c_cc[VTIME] = 10;    // Wait for up to 1s (10 deciseconds), returning as soon as any data is received.
    cfg.c_cc[VMIN] = 0;

    cfmakeraw(&cfg);
    if(Baud9600)
        cfsetspeed(&cfg, B9600);
    else
        cfsetspeed(&cfg, B115200);

    if (tcsetattr(fd, TCSANOW, &cfg)) {
        close(fd);
        return -1;
    }

/*    if(Baud9600)
        cfg.c_cflag =  B9600 | CS8 | CLOCAL | CREAD;
    else
        cfg.c_cflag =  B115200 | CS8 | CLOCAL | CREAD;

    // Disable output processing, including messing with end-of-line characters.
    cfg.c_oflag &= ~OPOST;
    cfg.c_iflag = IGNPAR;
    cfg.c_lflag = 0; // turn of CANON, ECHO*, etc

    // no timeout but request at least one character per read
    cfg.c_cc[VTIME] = 1;
    tcsetattr(fd, TCSANOW, &cfg);


//    line_val = LOOPBACK;
//    ioctl(fd, TIOCMSET, &line_val);
//    ioctl(fd, TIOCMGET, &line_val);*/

    tcflush(fd, TCIOFLUSH);

    return 0;
}




extern "C" JNIEXPORT int JNICALL Java_com_randi_RandiTestSys_MainActivity_sendI2CPort(JNIEnv *env, jclass thiz, jint fd, jbyteArray sendData, jint numChars)
{
    if(numChars > 0) {
        jbyte *bufferPtr = env->GetByteArrayElements(sendData, 0);
        int numSent=0, res;


        res = ioctl(fd, I2C_SLAVE, (int)bufferPtr[0]);
        bufferPtr++;
        numSent = write(fd, bufferPtr, numChars - 1);

        //(*env).ReleaseByteArrayElements(sendData,bufferPtr,JNI_ABORT);  //crashes???
        return numSent;
    }
    return -1;
}



#define ArraySize 4
extern "C" JNIEXPORT jint JNICALL Java_com_randi_RandiTestSys_MainActivity_readI2CPort(JNIEnv *env, jclass clazz, jint fd, jbyte addr, jintArray rcvBuf, jint numChars) {
    char rcvData[ArraySize];
    int int_rcvData[ArraySize];

    env->GetIntArrayRegion(rcvBuf, 0, numChars, int_rcvData);


    ioctl(fd, I2C_SLAVE, (int)addr);
    if(read(fd, rcvData, numChars) == numChars) {
        for(int i=0; i<ArraySize; i++)
            int_rcvData[i] = rcvData[i];
        env->SetIntArrayRegion(rcvBuf, 0, numChars, int_rcvData);

        short retval = rcvData[1] * 256;
        retval +=  rcvData[0];
        return retval;  // rcvData[0] + rcvData[1] * 256;
    }
    return -1;
}





extern "C" JNIEXPORT jint JNICALL Java_com_randi_RandiTestSys_MainActivity_readUARTport(JNIEnv *env, jclass clazz, jint fd, jintArray rcvBuf, jint numChars)
{
    jint numRcvdChars=-1;
    int num=0;
    char char_rcvData[256];
    int bufInt[256];

/*    char *char_rcvData;
    char_rcvData = (char*) malloc(numChars+1);
    if (char_rcvData == 0) {
        return -1;
    }

    int *bufInt;
    bufInt = (int *) malloc(numChars+1 * sizeof(int));
    if (bufInt == 0) {
        free(char_rcvData);
        return -1;
    }*/

    env->GetIntArrayRegion(rcvBuf, 0, numChars, bufInt);

    //wait 10mS for data in the rcv buffer
    clock_t start = clock();
    do {
        ioctl(fd, TIOCINQ, &num);   //get numbytes in input queue
    } while (num < numChars && clock()  < start + 100000);  //delay 100mS

    if(num >= numChars) {
            memset(char_rcvData, '\0', numChars + 1);
            numRcvdChars = read(fd, char_rcvData, numChars);

            for (int i = 0; i < numChars; i++)
                bufInt[i] = char_rcvData[i];
            env->SetIntArrayRegion(rcvBuf, 0, numChars, bufInt);
    }
    //free(bufInt);
    //free(char_rcvData);

    return numRcvdChars;
}






extern "C" JNIEXPORT jint JNICALL Java_com_randi_RandiTestSys_MainActivity_sendUARTPort(JNIEnv *env, jclass clazz,jint fd, jbyteArray send_str, jint num_chars) {
    if(num_chars > 0) {
        jbyte *bufferPtr = env->GetByteArrayElements(send_str, 0);
        int numSent;

        numSent = write(fd, bufferPtr, num_chars);

        //(*env).ReleaseByteArrayElements(sendData,bufferPtr,JNI_ABORT);  //crashes???
        return numSent;
    }
    return -1;
}